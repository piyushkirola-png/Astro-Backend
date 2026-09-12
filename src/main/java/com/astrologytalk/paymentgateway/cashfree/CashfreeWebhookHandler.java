package com.astrologytalk.paymentgateway.cashfree;

import com.astrologytalk.paymentgateway.WebhookHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@Component
public class CashfreeWebhookHandler implements WebhookHandler {

    @Override
    public String getGatewayName() {
        return "CASHFREE";
    }

    @Override
    @SuppressWarnings("unchecked")
    public WebhookResult process(String rawBody, Map<String, Object> payload, Map<String, String> headers) {
        WebhookResult result = new WebhookResult();

        try {
            String eventType = firstNonNull(
                    payload.get("type"),
                    payload.get("event")
            );
            result.setEventType(eventType);
            log.info("[Cashfree Webhook] Event: {}", eventType);

            if (eventType != null && (
                    eventType.startsWith("CHARGEBACK") ||
                    eventType.startsWith("REFUND") ||
                    eventType.equals("PAYMENT_LINK_EVENT"))) {
                result.setStatus("IGNORED");
                return result;
            }

            // Extract data.order and data.payment
            Map<String, Object> data = (Map<String, Object>) payload.get("data");
            if (data == null) {
                data = payload; // some webhooks send flat
            }

            Map<String, Object> order = (Map<String, Object>) data.get("order");
            Map<String, Object> payment = (Map<String, Object>) data.get("payment");

            String paymentStatus = null;
            if (payment != null && payment.get("payment_status") != null) {
                paymentStatus = payment.get("payment_status").toString();
            }

            // Determine success / fail
            boolean isSuccess = "PAYMENT_SUCCESS_WEBHOOK".equals(eventType)
                    || "ORDER_PAID".equals(eventType)
                    || "SUCCESS".equalsIgnoreCase(paymentStatus);

            boolean isFailed = "PAYMENT_FAILED_WEBHOOK".equals(eventType)
                    || "FAILED".equalsIgnoreCase(paymentStatus);

            if (!isSuccess && !isFailed) {
                result.setStatus("IGNORED");
                return result;
            }

            // Order ID
            String orderId = null;
            if (order != null && order.get("order_id") != null) {
                orderId = order.get("order_id").toString();
            } else if (payment != null && payment.get("order_id") != null) {
                orderId = payment.get("order_id").toString();
            } else if (payload.get("order_id") != null) {
                orderId = payload.get("order_id").toString();
            }
            result.setOrderId(orderId);

            // Payment ID
            String paymentId = null;
            if (payment != null) {
                paymentId = firstNonNull(payment.get("cf_payment_id"), payment.get("payment_id"));
            }
            result.setPaymentId(paymentId);

            // Amount
            BigDecimal amount = null;
            if (payment != null && payment.get("payment_amount") != null) {
                amount = new BigDecimal(payment.get("payment_amount").toString());
            } else if (order != null && order.get("order_amount") != null) {
                amount = new BigDecimal(order.get("order_amount").toString());
            }
            result.setAmount(amount);

            // UTR
            String utr = null;
            if (payment != null) {
                utr = firstNonNull(payment.get("bank_reference"), payment.get("utr"));
            }
            result.setUtr(utr);

            result.setStatus(isSuccess ? "SUCCESS" : "FAILED");

            log.info("[Cashfree Webhook] Parsed: order={}, payment={}, amount={}, utr={}, status={}",
                    orderId, paymentId, amount, utr, result.getStatus());

            return result;

        } catch (Exception e) {
            log.error("[Cashfree Webhook] Parse error: {}", e.getMessage(), e);
            result.setStatus("FAILED");
            result.setFailureReason(e.getMessage());
            return result;
        }
    }

    private String firstNonNull(Object... vals) {
        for (Object v : vals) {
            if (v != null && !v.toString().isBlank()) return v.toString();
        }
        return null;
    }
}