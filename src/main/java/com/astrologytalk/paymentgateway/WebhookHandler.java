package com.astrologytalk.paymentgateway;

import java.util.Map;

public interface WebhookHandler {

    /**
     * Gateway identifier
     */
    String getGatewayName();

    /**
     * Process raw webhook payload from the gateway.
     * Returns a standardized result the service layer can act on.
     *
     * @param rawBody  raw JSON body as string (needed for signature verification)
     * @param payload  parsed JSON as Map (convenience for handlers)
     * @param headers  HTTP headers from the webhook request
     * @return WebhookResult with status + identifiers
     */
    WebhookResult process(String rawBody, Map<String, Object> payload, Map<String, String> headers);

    /**
     * Standardized webhook result returned by all gateway handlers.
     */
    class WebhookResult {
        private String orderId;          // our gateway_order_id
        private String paymentId;        // gateway's payment id
        private String utr;              // bank reference / UTR
        private java.math.BigDecimal amount;
        private String status;           // "SUCCESS" | "FAILED" | "IGNORED"
        private String eventType;        // raw event type from gateway
        private String failureReason;    // if status=FAILED
        private boolean duplicate;       // true if already processed

        // --- constructors / getters / setters ---
        public WebhookResult() {}

        public String getOrderId() { return orderId; }
        public void setOrderId(String orderId) { this.orderId = orderId; }

        public String getPaymentId() { return paymentId; }
        public void setPaymentId(String paymentId) { this.paymentId = paymentId; }

        public String getUtr() { return utr; }
        public void setUtr(String utr) { this.utr = utr; }

        public java.math.BigDecimal getAmount() { return amount; }
        public void setAmount(java.math.BigDecimal amount) { this.amount = amount; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getEventType() { return eventType; }
        public void setEventType(String eventType) { this.eventType = eventType; }

        public String getFailureReason() { return failureReason; }
        public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

        public boolean isDuplicate() { return duplicate; }
        public void setDuplicate(boolean duplicate) { this.duplicate = duplicate; }
    }
}