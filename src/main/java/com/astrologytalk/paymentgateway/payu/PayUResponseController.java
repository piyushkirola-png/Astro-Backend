package com.astrologytalk.paymentgateway.payu;

import com.astrologytalk.entity.Payment;
import com.astrologytalk.entity.User;
import com.astrologytalk.repository.PaymentRepository;
import com.astrologytalk.repository.UserRepository;
import com.astrologytalk.service.EmailService;
import com.astrologytalk.service.InvoiceNumberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/webhooks/payu")
@RequiredArgsConstructor
public class PayUResponseController {

    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final PayUGatewayService payUGatewayService;
    private final EmailService emailService;
    private final InvoiceNumberService invoiceNumberService;

    @PostMapping("/response")
    public ResponseEntity<Void> handleResponse(
            @RequestParam(required = false) java.util.Map<String, String> params) {

        log.info("[PayU Response] Received: {}", params);

        String status = params.get("status");
        String txnId = params.get("txnid");
        String amount = params.get("amount");
        String productInfo = params.get("productinfo");
        String firstname = params.get("firstname");
        String email = params.get("email");
        String udf1 = params.get("udf1");
        String udf2 = params.get("udf2");
        String udf3 = params.get("udf3");
        String udf4 = params.get("udf4");
        String udf5 = params.get("udf5");
        String receivedHash = params.get("hash");
        String mihpayId = params.get("mihpayid");
        String bankRef = params.get("bank_ref_num");

        if (txnId == null || txnId.isBlank()) {
            log.warn("[PayU Response] Missing txnid");
            return redirect(payUGatewayService.getFrontendReturnUrl("UNKNOWN"));
        }

        Optional<Payment> paymentOpt = paymentRepository.findByGatewayOrderId(txnId);
        if (paymentOpt.isEmpty()) {
            log.warn("[PayU Response] No payment for txnid={}", txnId);
            return redirect(payUGatewayService.getFrontendReturnUrl(txnId));
        }

        Payment payment = paymentOpt.get();

        if ("SUCCESS".equals(payment.getStatus())) {
            log.info("[PayU Response] Payment {} already SUCCESS — skipping", txnId);
            return redirect(payUGatewayService.getFrontendReturnUrl(txnId));
        }

        boolean hashOk = payUGatewayService.verifyReverseHash(
                receivedHash, status, txnId, amount, productInfo,
                firstname, email, udf1, udf2, udf3, udf4, udf5);

        if (!hashOk) {
            log.warn("[PayU Response] Hash verification FAILED for {}", txnId);
            payment.setStatus("FAILED");
            payment.setFailureReason("Hash verification failed");
            payment.setProcessedAt(LocalDateTime.now());
            paymentRepository.save(payment);
            return redirect(payUGatewayService.getFrontendReturnUrl(txnId));
        }

        if ("SUCCESS".equalsIgnoreCase(status)) {
            markSuccess(payment, mihpayId, bankRef);
        } else if ("FAILURE".equalsIgnoreCase(status)
                || "FAILED".equalsIgnoreCase(status)
                || "CANCELLED".equalsIgnoreCase(status)) {
            markFailed(payment, status);
        } else {
            log.info("[PayU Response] Payment {} pending — webhook will handle", txnId);
        }

        return redirect(payUGatewayService.getFrontendReturnUrl(txnId));
    }

    private void markSuccess(Payment payment, String mihpayId, String bankRef) {
        payment.setStatus("SUCCESS");
        payment.setGatewayPaymentId(mihpayId);
        payment.setUtr(bankRef);
        payment.setCompletedAt(LocalDateTime.now());
        payment.setProcessedAt(LocalDateTime.now());
        payment.setFailureReason(null);
        payment.setFailureCode(null);

        try {
            invoiceNumberService.assignIfMissing(payment);
        } catch (Exception e) {
            log.warn("[PayU Response] Invoice # assignment failed: {}", e.getMessage());
        }

        if ("WALLET".equals(payment.getCategoryCode()) || payment.getPackageId() != null) {
            creditWallet(payment);
        }

        paymentRepository.save(payment);
        log.info("[PayU Response] Payment {} marked SUCCESS", payment.getGatewayOrderId());

        try {
            User user = userRepository.findById(payment.getUser().getId()).orElse(null);
            if (user != null) {
                emailService.sendInvoiceEmail(user, payment);
            }
        } catch (Exception e) {
            log.warn("[PayU Response] Invoice email failed: {}", e.getMessage());
        }
    }

    private void markFailed(Payment payment, String status) {
        payment.setStatus("FAILED");
        payment.setFailureReason("Payment " + status);
        payment.setFailureCode(status);
        payment.setProcessedAt(LocalDateTime.now());
        paymentRepository.save(payment);
        log.info("[PayU Response] Payment {} marked FAILED", payment.getGatewayOrderId());
    }

    private void creditWallet(Payment payment) {
        try {
            User user = userRepository.findById(payment.getUser().getId()).orElse(null);
            if (user == null) return;

            int seconds = payment.getSecondsCredited() != null ? payment.getSecondsCredited() : 0;
            int current = user.getChatSecondsBalance() != null ? user.getChatSecondsBalance() : 0;
            user.setChatSecondsBalance(current + seconds);
            userRepository.save(user);

            log.info("[PayU Response] Credited {} seconds to user {}", seconds, user.getId());
        } catch (Exception e) {
            log.error("[PayU Response] Wallet credit failed: {}", e.getMessage(), e);
            throw e;
        }
    }

    private ResponseEntity<Void> redirect(String url) {
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(url))
                .build();
    }
}