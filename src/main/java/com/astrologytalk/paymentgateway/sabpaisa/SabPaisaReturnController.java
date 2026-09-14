package com.astrologytalk.paymentgateway.sabpaisa;

import com.astrologytalk.entity.Payment;
import com.astrologytalk.entity.User;
import com.astrologytalk.repository.PaymentRepository;
import com.astrologytalk.repository.UserRepository;
import com.astrologytalk.service.EmailService;
import com.astrologytalk.service.InvoiceNumberService;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/webhooks/sabpaisa")
@RequiredArgsConstructor
public class SabPaisaReturnController {

  private final PaymentRepository paymentRepository;
  private final UserRepository userRepository;
  private final EmailService emailService;
  private final InvoiceNumberService invoiceNumberService;

  @Value("${sabpaisa.secret.key}")
  private String secretKey;

  @Value("${frontend.url:http://localhost:5170}")
  private String frontendUrl;

  @Value("${sabpaisa.verify.webhook:true}")
  private boolean verifyWebhook;

  @GetMapping("/return")
  public ResponseEntity<Void> handleReturn(@RequestParam Map<String, String> query) {
    log.info("[SabPaisa Return] Received: {}", query);

    String merchantTxnId = query.get("merchant_txn_id");
    String status = query.get("status");
    String transactionId = query.get("transaction_id");
    String signature = query.get("signature");
    String paidAmountStr = query.get("paid_amount");

    // Signature verification (only if enabled)
    if (verifyWebhook && signature != null && !signature.isBlank()) {
      boolean valid = verifyReturnSignature(query, signature);
      if (!valid) {
        log.warn("[SabPaisa Return] Signature verification FAILED for {}", merchantTxnId);
        return redirect(frontendUrl + "/user/payments?status=failed");
      }
      log.info("[SabPaisa Return] Signature verified for {}", merchantTxnId);
    }

    if (merchantTxnId == null || merchantTxnId.isBlank()) {
      log.warn("[SabPaisa Return] Missing merchant_txn_id");
      return redirect(frontendUrl + "/user/payments?status=return");
    }

    Optional<Payment> paymentOpt = paymentRepository.findByGatewayOrderId(merchantTxnId);
    if (paymentOpt.isEmpty()) {
      log.warn("[SabPaisa Return] No payment for {}", merchantTxnId);
      return redirect(frontendUrl + "/user/payments?status=return");
    }

    Payment payment = paymentOpt.get();

    // Idempotency — webhook may have already marked it
    if ("SUCCESS".equals(payment.getStatus())) {
      log.info("[SabPaisa Return] Payment {} already SUCCESS — skipping", merchantTxnId);
      return redirect(frontendUrl + "/user/payments?status=return");
    }

    if ("SUCCESS".equalsIgnoreCase(status)) {
      markSuccess(payment, transactionId, paidAmountStr);
    } else if ("FAILED".equalsIgnoreCase(status) || "CANCELLED".equalsIgnoreCase(status)) {
      markFailed(payment, status);
    } else {
      log.info("[SabPaisa Return] Payment {} still pending — webhook will handle", merchantTxnId);
    }

    return redirect(frontendUrl + "/user/payments?status=return");
  }

  private void markSuccess(Payment payment, String transactionId, String paidAmountStr) {
    payment.setStatus("SUCCESS");
    payment.setGatewayPaymentId(transactionId);
    payment.setCompletedAt(LocalDateTime.now());
    payment.setProcessedAt(LocalDateTime.now());
    payment.setFailureReason(null);
    payment.setFailureCode(null);

    try {
      invoiceNumberService.assignIfMissing(payment);
    } catch (Exception e) {
      log.warn(
          "[SabPaisa Return] Invoice # assignment failed for {}: {}",
          payment.getGatewayOrderId(),
          e.getMessage());
    }

    // Credit wallet if wallet recharge
    if ("WALLET".equals(payment.getCategoryCode()) || payment.getPackageId() != null) {
      creditWallet(payment);
    }

    paymentRepository.save(payment);
    log.info("[SabPaisa Return] Payment {} marked SUCCESS", payment.getGatewayOrderId());

    try {
      User user = userRepository.findById(payment.getUser().getId()).orElse(null);
      if (user != null) {
        emailService.sendInvoiceEmail(user, payment);
      }
    } catch (Exception e) {
      log.warn(
          "[SabPaisa Return] Invoice email failed for {}: {}",
          payment.getGatewayOrderId(),
          e.getMessage());
    }
  }

  private void markFailed(Payment payment, String status) {
    payment.setStatus("FAILED");
    payment.setFailureReason("Payment " + status);
    payment.setFailureCode(status);
    payment.setProcessedAt(LocalDateTime.now());
    paymentRepository.save(payment);
    log.info("[SabPaisa Return] Payment {} marked FAILED", payment.getGatewayOrderId());
  }

  private void creditWallet(Payment payment) {
    try {
      User user = userRepository.findById(payment.getUser().getId()).orElse(null);
      if (user == null) return;

      int seconds = payment.getSecondsCredited() != null ? payment.getSecondsCredited() : 0;
      int current = user.getChatSecondsBalance() != null ? user.getChatSecondsBalance() : 0;
      user.setChatSecondsBalance(current + seconds);
      userRepository.save(user);

      log.info(
          "[SabPaisa Return] Credited {} seconds to user {}. New: {}",
          seconds,
          user.getId(),
          user.getChatSecondsBalance());
    } catch (Exception e) {
      log.error("[SabPaisa Return] Wallet credit failed: {}", e.getMessage(), e);
      throw e;
    }
  }

  private boolean verifyReturnSignature(Map<String, String> query, String receivedSignature) {
    try {
      // SabPaisa return signature: sorted key=value pairs joined by "|"
      TreeMap<String, String> sorted = new TreeMap<>();
      for (String key :
          new String[] {
            "amount",
            "merchant_txn_id",
            "paid_amount",
            "payment_mode",
            "status",
            "timestamp",
            "transaction_id"
          }) {
        if (query.containsKey(key) && query.get(key) != null) {
          sorted.put(key, query.get(key));
        }
      }

      StringBuilder sb = new StringBuilder();
      for (Map.Entry<String, String> e : sorted.entrySet()) {
        if (sb.length() > 0) sb.append("|");
        sb.append(e.getKey()).append("=").append(e.getValue());
      }
      String dataString = sb.toString();

      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      byte[] hash = mac.doFinal(dataString.getBytes(StandardCharsets.UTF_8));

      StringBuilder hex = new StringBuilder();
      for (byte b : hash) hex.append(String.format("%02x", b));

      return hex.toString().equals(receivedSignature);
    } catch (Exception e) {
      log.error("[SabPaisa Return] Signature verify error: {}", e.getMessage());
      return false;
    }
  }

  private ResponseEntity<Void> redirect(String url) {
    return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(url)).build();
  }
}
