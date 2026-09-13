package com.astrologytalk.paymentgateway.sabpaisa;

import com.astrologytalk.paymentgateway.WebhookHandler;
import java.math.BigDecimal;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SabPaisaWebhookHandler implements WebhookHandler {

  @Override
  public String getGatewayName() {
    return "SABPAISA";
  }

  @Override
  public WebhookResult process(
      String rawBody, Map<String, Object> payload, Map<String, String> headers) {
    WebhookResult result = new WebhookResult();

    try {
      String merchantTxnId = str(payload.get("merchantTxnId"));
      String paymentId = str(payload.get("paymentId"));
      String rawAmount = str(payload.get("amount"));
      String status = str(payload.get("status"));
      if (status == null) status = str(payload.get("paymentStatus"));

      String bankRef = str(payload.get("bankReference"));
      if (bankRef == null) bankRef = str(payload.get("utr"));

      String paymentMode = str(payload.get("paymentMode"));

      log.info(
          "[SabPaisa Webhook] merchantTxnId={}, paymentId={}, status={}, amountPaise={}",
          merchantTxnId,
          paymentId,
          status,
          rawAmount);

      result.setOrderId(merchantTxnId);
      result.setPaymentId(paymentId);
      result.setUtr(bankRef);
      result.setEventType("SABPAISA_" + (status != null ? status.toUpperCase() : "UNKNOWN"));

      // Amount in paise → convert to rupees
      if (rawAmount != null && !rawAmount.isBlank()) {
        try {
          long paise = Long.parseLong(rawAmount);
          result.setAmount(BigDecimal.valueOf(paise).divide(BigDecimal.valueOf(100)));
        } catch (NumberFormatException e) {
          log.warn("[SabPaisa Webhook] Bad amount: {}", rawAmount);
        }
      }

      String upper = status != null ? status.toUpperCase() : "";

      if ("SUCCESS".equals(upper) || "COMPLETED".equals(upper) || "CAPTURED".equals(upper)) {
        result.setStatus("SUCCESS");
      } else if ("FAILED".equals(upper)
          || "EXPIRED".equals(upper)
          || "CANCELLED".equals(upper)
          || "DECLINED".equals(upper)) {
        result.setStatus("FAILED");
        String reason = str(payload.get("failureReason"));
        if (reason == null) reason = str(payload.get("errorMessage"));
        if (reason == null) reason = "Payment " + upper;
        result.setFailureReason(reason);
      } else if ("PENDING".equals(upper)
          || "INITIATED".equals(upper)
          || "PROCESSING".equals(upper)) {
        result.setStatus("PENDING");
      } else {
        result.setStatus("IGNORED");
      }

      return result;

    } catch (Exception e) {
      log.error("[SabPaisa Webhook] Parse error: {}", e.getMessage(), e);
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
