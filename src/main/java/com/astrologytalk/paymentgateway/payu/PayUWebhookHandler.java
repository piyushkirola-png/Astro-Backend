package com.astrologytalk.paymentgateway.payu;

import com.astrologytalk.paymentgateway.WebhookHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PayUWebhookHandler implements WebhookHandler {

    private final PayUGatewayService payUGatewayService;

    @Override
    public String getGatewayName() {
        return "PAYU";
    }

    @Override
    public WebhookResult process(String rawBody, Map<String, Object> payload, Map<String, String> headers) {
        WebhookResult result = new WebhookResult();

        try {
            String status = str(payload.get("status"));
            String txnId = str(payload.get("txnid"));
            String amount = str(payload.get("amount"));
            String productInfo = str(payload.get("productinfo"));
            String firstname = str(payload.get("firstname"));
            String email = str(payload.get("email"));
            String udf1 = str(payload.get("udf1"));
            String udf2 = str(payload.get("udf2"));
            String udf3 = str(payload.get("udf3"));
            String udf4 = str(payload.get("udf4"));
            String udf5 = str(payload.get("udf5"));
            String receivedHash = str(payload.get("hash"));
            String mihpayId = str(payload.get("mihpayid"));
            String bankRef = str(payload.get("bank_ref_num"));
            String failureMsg = str(payload.get("field9"));
            if (failureMsg == null) failureMsg = str(payload.get("error_Message"));

            result.setEventType("PAYU_" + (status != null ? status.toUpperCase() : "UNKNOWN"));
            result.setOrderId(txnId);
            result.setPaymentId(mihpayId);
            result.setUtr(bankRef);

            log.info("[PayU Webhook] txnId={}, status={}, mihpayid={}, amount={}",
                    txnId, status, mihpayId, amount);

            if (amount != null && !amount.isBlank()) {
                try {
                    result.setAmount(new BigDecimal(amount));
                } catch (Exception e) {
                    log.warn("[PayU Webhook] Bad amount: {}", amount);
                }
            }

            boolean hashOk = payUGatewayService.verifyReverseHash(
                    receivedHash, status, txnId, amount, productInfo,
                    firstname, email, udf1, udf2, udf3, udf4, udf5);

            if (!hashOk) {
                log.warn("[PayU Webhook] Hash verification FAILED for txnId={}", txnId);
                result.setStatus("FAILED");
                result.setFailureReason("Hash verification failed");
                return result;
            }

            String upper = status != null ? status.toUpperCase() : "";

            if ("SUCCESS".equals(upper)) {
                result.setStatus("SUCCESS");
            } else if ("FAILURE".equals(upper) || "FAILED".equals(upper) || "CANCELLED".equals(upper)) {
                result.setStatus("FAILED");
                result.setFailureReason(failureMsg != null ? failureMsg : "Payment " + upper);
            } else if ("PENDING".equals(upper) || "INPROGRESS".equals(upper)) {
                result.setStatus("PENDING");
            } else {
                result.setStatus("IGNORED");
            }

            log.info("[PayU Webhook] Parsed: txnId={}, status={}, amount={}, utr={}",
                    txnId, result.getStatus(), result.getAmount(), bankRef);

            return result;

        } catch (Exception e) {
            log.error("[PayU Webhook] Parse error: {}", e.getMessage(), e);
            result.setStatus("FAILED");
            result.setFailureReason(e.getMessage());
            return result;
        }
    }

    private String str(Object o) {
        if (o == null) return null;
        String s = o.toString().trim();
        return s.isEmpty() ? null : s;
    }
}