package com.astrologytalk.service;

import com.astrologytalk.entity.Payment;
import com.astrologytalk.entity.User;
import com.astrologytalk.entity.WebhookInboundLog;
import com.astrologytalk.paymentgateway.WebhookHandler;
import com.astrologytalk.repository.PaymentRepository;
import com.astrologytalk.repository.UserRepository;
import com.astrologytalk.repository.WebhookInboundLogRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

  private final List<WebhookHandler> handlers;
  private final PaymentRepository paymentRepository;
  private final UserRepository userRepository;
  private final WebhookInboundLogRepository webhookLogRepository;
  private final EmailService emailService;
  private final InvoiceNumberService invoiceNumberService;

  /** Dispatcher — routes webhook to the right gateway handler. */
  public void processWebhook(
      String gatewayName,
      String rawBody,
      Map<String, Object> payload,
      Map<String, String> headers) {

    WebhookHandler handler =
        handlers.stream()
            .filter(h -> h.getGatewayName().equalsIgnoreCase(gatewayName))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Unknown gateway: " + gatewayName));

    WebhookHandler.WebhookResult result = handler.process(rawBody, payload, headers);

    // Always log inbound
    logInbound(gatewayName, result, rawBody, headers);

    if ("IGNORED".equals(result.getStatus())) {
      log.info("[Webhook] Ignored event type {} for {}", result.getEventType(), gatewayName);
      return;
    }

    if (result.getOrderId() == null) {
      log.warn("[Webhook] Missing order_id in payload for {}", gatewayName);
      return;
    }

    // Find our payment
    Optional<Payment> paymentOpt = paymentRepository.findByGatewayOrderId(result.getOrderId());
    if (paymentOpt.isEmpty()) {
      log.warn("[Webhook] No payment found for order {}", result.getOrderId());
      return;
    }

    handlePaymentResult(paymentOpt.get(), result);
  }

  // ---------- Core logic ----------

  @Transactional
  protected void handlePaymentResult(Payment payment, WebhookHandler.WebhookResult result) {

    // Idempotency: already SUCCESS → skip
    if ("SUCCESS".equals(payment.getStatus())) {
      log.info(
          "[Webhook] Payment {} already SUCCESS — skipping duplicate", payment.getGatewayOrderId());
      return;
    }

    if ("SUCCESS".equals(result.getStatus())) {
      payment.setStatus("SUCCESS");
      payment.setGatewayPaymentId(result.getPaymentId());
      payment.setUtr(result.getUtr());
      payment.setCompletedAt(LocalDateTime.now());
      payment.setProcessedAt(LocalDateTime.now());
      payment.setFailureReason(null);
      payment.setFailureCode(null);

      // Assign invoice number (once)
      try {
        invoiceNumberService.assignIfMissing(payment);
      } catch (Exception e) {
        log.warn(
            "[Webhook] Invoice # assignment failed for {}: {}",
            payment.getGatewayOrderId(),
            e.getMessage());
      }

      // Credit wallet if wallet recharge
      if ("WALLET".equals(payment.getCategoryCode()) || payment.getPackageId() != null) {
        creditWallet(payment);
      }

      paymentRepository.save(payment);
      log.info("[Webhook] Payment {} marked SUCCESS", payment.getGatewayOrderId());

      // Send invoice email async
      try {
        User user = userRepository.findById(payment.getUser().getId()).orElse(null);
        if (user != null) {
          emailService.sendInvoiceEmail(user, payment);
        }
      } catch (Exception e) {
        log.warn(
            "[Webhook] Invoice email failed for {}: {}",
            payment.getGatewayOrderId(),
            e.getMessage());
      }

    } else if ("FAILED".equals(result.getStatus())) {
      payment.setStatus("FAILED");
      payment.setFailureReason(result.getFailureReason());
      payment.setProcessedAt(LocalDateTime.now());
      paymentRepository.save(payment);
      log.info("[Webhook] Payment {} marked FAILED", payment.getGatewayOrderId());
    }
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
          "[Webhook] Credited {} seconds to user {}. New balance: {}",
          seconds,
          user.getId(),
          user.getChatSecondsBalance());

    } catch (Exception e) {
      log.error(
          "[Webhook] Wallet credit failed for payment {}: {}", payment.getId(), e.getMessage(), e);
      throw e; // rollback transaction
    }
  }

  private void logInbound(
      String gatewayName,
      WebhookHandler.WebhookResult result,
      String rawBody,
      Map<String, String> headers) {
    try {
      WebhookInboundLog log = new WebhookInboundLog();
      log.setGateway(gatewayName);
      log.setEventType(result.getEventType());
      log.setOrderId(result.getOrderId());
      log.setPaymentId(result.getPaymentId());
      log.setAmount(result.getAmount());
      log.setStatus(result.getStatus());
      log.setSignatureVerified(false); // skipped in sandbox
      log.setRequestBody(truncate(rawBody, 8000));
      log.setRequestHeaders(headers != null ? headers.toString() : null);
      webhookLogRepository.save(log);
    } catch (Exception e) {
      log.error("[Webhook] Failed to log inbound: {}", e.getMessage());
    }
  }

  private String truncate(String s, int max) {
    if (s == null) return null;
    return s.length() > max ? s.substring(0, max) : s;
  }
}
