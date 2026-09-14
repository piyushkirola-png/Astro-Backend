package com.astrologytalk.paymentgateway.payu;

import com.astrologytalk.entity.Payment;
import com.astrologytalk.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Slf4j
@Service
public class PayUGatewayService {

    @Value("${payu.merchant.key}")
    private String merchantKey;

    @Value("${payu.merchant.salt}")
    private String merchantSalt;

    @Value("${payu.base.url:https://test.payu.in}")
    private String baseUrl;

    @Value("${frontend.url:http://localhost:5170}")
    private String frontendUrl;

    @Value("${backend.url:http://localhost:8080}")
    private String backendUrl;

    public boolean initiate(Payment payment, User user) {
        try {
            String txnId = payment.getGatewayOrderId();
            String amount = payment.getAmount()
                    .setScale(2, RoundingMode.HALF_UP)
                    .toPlainString();

            String firstname = extractFirstName(user.getName());
            String email = user.getEmail() != null
                    ? user.getEmail()
                    : "customer_" + user.getId() + "@jyotishai.com";
            String phone = sanitizePhone(user.getPhone());

            String productInfo = buildProductInfo(payment);

            String udf1 = payment.getGatewayOrderId();
            String udf2 = String.valueOf(user.getId());
            String udf3 = payment.getCategoryCode() != null ? payment.getCategoryCode() : "";
            String udf4 = "";
            String udf5 = "";

            String hash = generateRequestHash(
                    txnId, amount, productInfo, firstname, email, udf1, udf2, udf3, udf4, udf5);

            if (hash == null) {
                log.error("[PayU] Hash generation failed for {}", txnId);
                return false;
            }

            String redirectUrl = backendUrl + "/api/payments/payu/redirect/" + txnId;
            payment.setPaymentLink(redirectUrl);

            log.info("[PayU] Initiated for txnId={}, amount=INR {}, redirect={}",
                    txnId, amount, redirectUrl);
            return true;

        } catch (Exception e) {
            log.error("[PayU] Unexpected error on initiate for {}: {}",
                    payment.getGatewayOrderId(), e.getMessage(), e);
            return false;
        }
    }

    public String getMerchantKey() {
        return merchantKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getSuccessUrl() {
        return backendUrl + "/api/webhooks/payu/response";
    }

    public String getFailureUrl() {
        return backendUrl + "/api/webhooks/payu/response";
    }

    public String getFrontendReturnUrl(String orderId) {
        return frontendUrl + "/user/payments?status=return&order_id=" + orderId;
    }

    public String generateRequestHash(
            String txnId, String amount, String productInfo,
            String firstname, String email,
            String udf1, String udf2, String udf3, String udf4, String udf5) {

        try {
            String hashString = merchantKey + "|" + txnId + "|" + amount + "|"
                    + productInfo + "|" + firstname + "|" + email + "|"
                    + nvl(udf1) + "|" + nvl(udf2) + "|" + nvl(udf3) + "|" + nvl(udf4) + "|" + nvl(udf5)
                    + "||||||" + merchantSalt;

            log.debug("[PayU] Request hash string: {}", hashString);
            return sha512(hashString);
        } catch (Exception e) {
            log.error("[PayU] Request hash error: {}", e.getMessage());
            return null;
        }
    }

    public String generateReverseHash(
            String status, String txnId, String amount, String productInfo,
            String firstname, String email,
            String udf1, String udf2, String udf3, String udf4, String udf5) {

        try {
            String hashString = merchantSalt + "|" + status + "||||||"
                    + nvl(udf5) + "|" + nvl(udf4) + "|" + nvl(udf3) + "|" + nvl(udf2) + "|" + nvl(udf1) + "|"
                    + email + "|" + firstname + "|" + productInfo + "|"
                    + amount + "|" + txnId + "|" + merchantKey;

            log.debug("[PayU] Reverse hash string: {}", hashString);
            return sha512(hashString);
        } catch (Exception e) {
            log.error("[PayU] Reverse hash error: {}", e.getMessage());
            return null;
        }
    }

    public boolean verifyReverseHash(
            String receivedHash, String status, String txnId, String amount,
            String productInfo, String firstname, String email,
            String udf1, String udf2, String udf3, String udf4, String udf5) {

        String computed = generateReverseHash(
                status, txnId, amount, productInfo, firstname, email,
                udf1, udf2, udf3, udf4, udf5);

        if (computed == null || receivedHash == null) return false;

        boolean matches = computed.equalsIgnoreCase(receivedHash);
        if (!matches) {
            log.warn("[PayU] Reverse hash mismatch — received={}, computed={}",
                    receivedHash, computed);
        }
        return matches;
    }

    private String sha512(String input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-512");
        byte[] bytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private String buildProductInfo(Payment payment) {
        if ("REPORT".equalsIgnoreCase(payment.getCategoryCode())) {
            return "Kundali Report";
        }
        if ("WALLET".equalsIgnoreCase(payment.getCategoryCode())) {
            Integer seconds = payment.getSecondsCredited();
            if (seconds != null && seconds > 0) {
                int mins = seconds / 60;
                return "AI Chat Wallet - " + mins + " min";
            }
            return "AI Chat Wallet Recharge";
        }
        return "Jyotish AI Payment";
    }

    private String extractFirstName(String name) {
        if (name == null || name.isBlank()) return "Customer";
        String[] parts = name.trim().split("\\s+");
        return parts[0];
    }

    private String sanitizePhone(String phone) {
        if (phone == null || phone.isBlank()) return "9876543210";
        String digits = phone.replaceAll("\\D", "");
        if (digits.length() > 10) digits = digits.substring(digits.length() - 10);
        if (digits.length() < 10) return "9876543210";
        return digits;
    }

    private String nvl(String s) {
        return s != null ? s : "";
    }
}