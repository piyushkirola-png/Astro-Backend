package com.astrologytalk.paymentgateway.cashfree;

import com.astrologytalk.entity.Payment;
import com.astrologytalk.entity.User;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class CashfreeGatewayService {

  @Value("${cashfree.app.id}")
  private String appId;

  @Value("${cashfree.secret.key}")
  private String secretKey;

  @Value("${cashfree.mode:sandbox}")
  private String mode;

  @Value("${frontend.url:http://localhost:5170}")
  private String frontendUrl;

  @Value("${backend.url:http://localhost:8080}")
  private String backendUrl;

  private final RestTemplate restTemplate;

  public CashfreeGatewayService(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  private String baseUrl() {
    return "production".equalsIgnoreCase(mode)
        ? "https://api.cashfree.com/pg"
        : "https://sandbox.cashfree.com/pg";
  }

  private HttpHeaders buildHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("x-api-version", "2023-08-01");
    headers.set("x-client-id", appId);
    headers.set("x-client-secret", secretKey);
    return headers;
  }

  @SuppressWarnings("unchecked")
  public boolean initiate(Payment payment, User user) {
    try {
      String orderId = payment.getGatewayOrderId();
      BigDecimal amount = payment.getAmount();

      String customerName = user.getName() != null ? user.getName() : "Customer";
      String customerEmail =
          user.getEmail() != null ? user.getEmail() : "customer_" + user.getId() + "@jyotishai.com";
      String customerPhone = sanitizePhone(user.getPhone());

      Map<String, Object> orderBody = new HashMap<>();
      orderBody.put("order_id", orderId);
      orderBody.put("order_amount", amount);
      orderBody.put("order_currency", "INR");
      orderBody.put("order_note", "Payment for " + orderId);

      Map<String, Object> customerDetails = new HashMap<>();
      customerDetails.put("customer_id", "cust_" + user.getId());
      customerDetails.put("customer_name", customerName);
      customerDetails.put("customer_email", customerEmail);
      customerDetails.put("customer_phone", customerPhone);
      orderBody.put("customer_details", customerDetails);

      Map<String, Object> orderMeta = new HashMap<>();
      orderMeta.put("return_url", frontendUrl + "/user/payments?status=return&order_id=" + orderId);
      orderMeta.put("notify_url", backendUrl + "/api/webhooks/cashfree");
      orderMeta.put("payment_methods", "cc,dc,upi,nb,app,paylater");
      orderBody.put("order_meta", orderMeta);

      HttpEntity<Map<String, Object>> orderReq = new HttpEntity<>(orderBody, buildHeaders());
      ResponseEntity<Map> orderResp =
          restTemplate.exchange(baseUrl() + "/orders", HttpMethod.POST, orderReq, Map.class);

      log.info("[Cashfree] Order created: {}", orderId);

      Map<String, Object> linkBody = new HashMap<>();
      linkBody.put("link_id", orderId);
      linkBody.put("link_amount", amount);
      linkBody.put("link_currency", "INR");
      linkBody.put("link_name", "Payment for " + orderId);
      linkBody.put("link_purpose", "Recharge/Purchase " + orderId);

      Map<String, Object> linkCustomer = new HashMap<>();
      linkCustomer.put("customer_name", customerName);
      linkCustomer.put("customer_email", customerEmail);
      linkCustomer.put("customer_phone", customerPhone);
      linkBody.put("customer_details", linkCustomer);

      Map<String, Object> linkNotify = new HashMap<>();
      linkNotify.put("send_sms", false);
      linkNotify.put("send_email", false);
      linkBody.put("link_notify", linkNotify);

      linkBody.put("link_auto_reminders", false);

      String expiry =
          LocalDateTime.now().plusHours(24).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z";
      linkBody.put("link_expiry_time", expiry);

      Map<String, Object> linkMeta = new HashMap<>();
      linkMeta.put("return_url", frontendUrl + "/user/payments?status=return");
      linkMeta.put("notify_url", backendUrl + "/api/webhooks/cashfree");
      linkBody.put("link_meta", linkMeta);

      HttpEntity<Map<String, Object>> linkReq = new HttpEntity<>(linkBody, buildHeaders());
      ResponseEntity<Map> linkResp =
          restTemplate.exchange(baseUrl() + "/links", HttpMethod.POST, linkReq, Map.class);

      Map<String, Object> linkData = linkResp.getBody();
      if (linkData == null || linkData.get("link_url") == null) {
        log.error("[Cashfree] Payment link creation returned no URL for {}", orderId);
        return false;
      }

      String paymentLink = linkData.get("link_url").toString();
      payment.setPaymentLink(paymentLink);
      log.info("[Cashfree] Payment link for {}: {}", orderId, paymentLink);
      return true;

    } catch (org.springframework.web.client.HttpClientErrorException e) {
      log.error(
          "[Cashfree] 4xx on initiate for {}: {} — {}",
          payment.getGatewayOrderId(),
          e.getStatusCode(),
          e.getResponseBodyAsString());
      return false;
    } catch (org.springframework.web.client.HttpServerErrorException e) {
      log.error(
          "[Cashfree] 5xx on initiate for {}: {} — {}",
          payment.getGatewayOrderId(),
          e.getStatusCode(),
          e.getResponseBodyAsString());
      return false;
    } catch (Exception e) {
      log.error(
          "[Cashfree] Unexpected error on initiate for {}: {}",
          payment.getGatewayOrderId(),
          e.getMessage(),
          e);
      return false;
    }
  }

  private String sanitizePhone(String phone) {
    if (phone == null || phone.isBlank()) return "9999999999";
    String digits = phone.replaceAll("\\D", "");
    if (digits.length() > 10) digits = digits.substring(digits.length() - 10);
    if (digits.length() < 10) return "9999999999";
    return digits;
  }
}
