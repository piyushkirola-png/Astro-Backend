package com.astrologytalk.paymentgateway.sabpaisa;

import com.astrologytalk.entity.Payment;
import com.astrologytalk.entity.User;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class SabPaisaGatewayService {

  @Value("${sabpaisa.merchant.id}")
  private String merchantId;

  @Value("${sabpaisa.api.key}")
  private String apiKey;

  @Value("${sabpaisa.secret.key}")
  private String secretKey;

  @Value("${sabpaisa.base.url:https://staging-sb-merchant-api.sabpaisa.in}")
  private String baseUrl;

  @Value("${frontend.url:http://localhost:5170}")
  private String frontendUrl;

  @Value("${backend.url:http://localhost:8080}")
  private String backendUrl;

  private final RestTemplate restTemplate;

  public SabPaisaGatewayService(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  private HttpHeaders buildHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("X-Api-Key", apiKey);
    return headers;
  }

  private String generateChecksum(
      String merchantTxnId, long amountPaise, String currency, long timestamp) {
    try {
      String message =
          merchantId + "|" + merchantTxnId + "|" + amountPaise + "|" + currency + "|" + timestamp;
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      byte[] hash = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
      StringBuilder hex = new StringBuilder();
      for (byte b : hash) {
        hex.append(String.format("%02x", b));
      }
      return hex.toString();
    } catch (Exception e) {
      throw new RuntimeException("Failed to generate checksum", e);
    }
  }

  @SuppressWarnings("unchecked")
  public boolean initiate(Payment payment, User user) {
    try {
      String merchantTxnId = payment.getGatewayOrderId();
      BigDecimal amount = payment.getAmount();
      long amountPaise = amount.multiply(BigDecimal.valueOf(100)).longValue();
      long timestamp = System.currentTimeMillis() / 1000;

      String checksum = generateChecksum(merchantTxnId, amountPaise, "INR", timestamp);

      String customerName = user.getName() != null ? user.getName() : "Customer";
      String customerEmail =
          user.getEmail() != null ? user.getEmail() : "customer_" + user.getId() + "@jyotishai.com";
      String customerPhone = sanitizePhone(user.getPhone());

      Map<String, Object> body = new HashMap<>();
      body.put("merchantId", merchantId);
      body.put("merchantTxnId", merchantTxnId);
      body.put("amount", amountPaise);
      body.put("currency", "INR");
      body.put("returnUrl", backendUrl + "/api/webhooks/sabpaisa/return");
      body.put("customerName", customerName);
      body.put("customerEmail", customerEmail);
      body.put("customerPhone", customerPhone);
      body.put("description", "Payment for " + merchantTxnId);
      body.put("timestamp", timestamp);
      body.put("checksum", checksum);

      HttpEntity<Map<String, Object>> req = new HttpEntity<>(body, buildHeaders());
      ResponseEntity<Map> resp =
          restTemplate.exchange(baseUrl + "/api/v2/payments", HttpMethod.POST, req, Map.class);

      Map<String, Object> data = resp.getBody();
      if (data == null) {
        log.error("[SabPaisa] Empty response for {}", merchantTxnId);
        return false;
      }

      Object checkoutUrlObj = data.get("checkoutUrl");
      if (checkoutUrlObj == null) {
        log.error("[SabPaisa] No checkoutUrl in response for {}: {}", merchantTxnId, data);
        return false;
      }

      String checkoutUrl = checkoutUrlObj.toString();
      Object clientSecret = data.get("clientSecret");
      if (clientSecret != null && !clientSecret.toString().isBlank()) {
        checkoutUrl = checkoutUrl + "?clientSecret=" + clientSecret;
      }

      Object paymentId = data.get("paymentId");
      if (paymentId != null) {
        payment.setGatewayPaymentId(paymentId.toString());
      }

      payment.setPaymentLink(checkoutUrl);
      log.info("[SabPaisa] Payment link for {}: {}", merchantTxnId, checkoutUrl);
      return true;

    } catch (HttpClientErrorException e) {
      log.error(
          "[SabPaisa] 4xx on initiate for {}: {} - {}",
          payment.getGatewayOrderId(),
          e.getStatusCode(),
          e.getResponseBodyAsString());
      return false;
    } catch (HttpServerErrorException e) {
      log.error(
          "[SabPaisa] 5xx on initiate for {}: {} - {}",
          payment.getGatewayOrderId(),
          e.getStatusCode(),
          e.getResponseBodyAsString());
      return false;
    } catch (Exception e) {
      log.error(
          "[SabPaisa] Unexpected error on initiate for {}: {}",
          payment.getGatewayOrderId(),
          e.getMessage(),
          e);
      return false;
    }
  }

  private String sanitizePhone(String phone) {
    if (phone == null || phone.isBlank()) return "9876543210";
    String digits = phone.replaceAll("\\D", "");
    if (digits.length() > 10) digits = digits.substring(digits.length() - 10);
    if (digits.length() < 10) return "9876543210";
    return digits;
  }
}
