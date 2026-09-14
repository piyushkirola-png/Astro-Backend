package com.astrologytalk.controller;

import com.astrologytalk.entity.Payment;
import com.astrologytalk.repository.PaymentRepository;
import com.astrologytalk.paymentgateway.payu.PayUGatewayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.RoundingMode;

@Slf4j
@RestController
@RequestMapping("/api/payments/payu")
@RequiredArgsConstructor
public class PayURedirectController {

    private final PaymentRepository paymentRepository;
    private final PayUGatewayService payUGatewayService;

    @GetMapping(value = "/redirect/{orderId}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> redirectToPayU(@PathVariable String orderId) {

        Payment payment = paymentRepository.findByGatewayOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        String amount = payment.getAmount().setScale(2, RoundingMode.HALF_UP).toPlainString();
        String txnId = payment.getGatewayOrderId();
        String productInfo = buildProductInfo(payment);
        String firstname = extractFirstName(payment.getCustomerName());
        String email = payment.getCustomerEmail() != null
                ? payment.getCustomerEmail()
                : "customer_" + payment.getUser().getId() + "@jyotishai.com";
        String phone = sanitizePhone(payment.getCustomerPhone());
        String udf1 = payment.getGatewayOrderId();
        String udf2 = String.valueOf(payment.getUser().getId());
        String udf3 = payment.getCategoryCode() != null ? payment.getCategoryCode() : "";
        String udf4 = "";
        String udf5 = "";

        String hash = payUGatewayService.generateRequestHash(
                txnId, amount, productInfo, firstname, email, udf1, udf2, udf3, udf4, udf5);

        String surl = payUGatewayService.getSuccessUrl();
        String furl = payUGatewayService.getFailureUrl();
        String payuUrl = payUGatewayService.getBaseUrl() + "/_payment";
        String merchantKey = payUGatewayService.getMerchantKey();

        String html = buildAutoSubmitHtml(
                payuUrl, merchantKey, txnId, amount, productInfo,
                firstname, email, phone, surl, furl, hash,
                udf1, udf2, udf3, udf4, udf5);

        log.info("[PayU] Serving redirect page for {}", orderId);

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(html);
    }

    private String buildProductInfo(Payment payment) {
        if ("REPORT".equalsIgnoreCase(payment.getCategoryCode())) {
            return "Kundali Report";
        }
        if ("WALLET".equalsIgnoreCase(payment.getCategoryCode())) {
            Integer seconds = payment.getSecondsCredited();
            if (seconds != null && seconds > 0) {
                return "AI Chat Wallet - " + (seconds / 60) + " min";
            }
            return "AI Chat Wallet Recharge";
        }
        return "Jyotish AI Payment";
    }

    private String extractFirstName(String name) {
        if (name == null || name.isBlank()) return "Customer";
        return name.trim().split("\\s+")[0];
    }

    private String sanitizePhone(String phone) {
        if (phone == null || phone.isBlank()) return "9876543210";
        String digits = phone.replaceAll("\\D", "");
        if (digits.length() > 10) digits = digits.substring(digits.length() - 10);
        if (digits.length() < 10) return "9876543210";
        return digits;
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private String buildAutoSubmitHtml(
            String payuUrl, String key, String txnId, String amount, String productInfo,
            String firstname, String email, String phone, String surl, String furl, String hash,
            String udf1, String udf2, String udf3, String udf4, String udf5) {

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\">");
        sb.append("<title>Redirecting to PayU...</title>");
        sb.append("<style>");
        sb.append("body{font-family:Arial,sans-serif;background:#faf8f4;display:flex;align-items:center;justify-content:center;height:100vh;margin:0;}");
        sb.append(".box{text-align:center;padding:40px;background:#fff;border-radius:16px;box-shadow:0 8px 32px rgba(0,0,0,0.08);max-width:400px;}");
        sb.append(".spinner{width:48px;height:48px;border:4px solid #f4f1ea;border-top-color:#b8862a;border-radius:50%;animation:spin 1s linear infinite;margin:0 auto 20px;}");
        sb.append("@keyframes spin{to{transform:rotate(360deg);}}");
        sb.append("h1{color:#17120c;font-size:18px;margin:0 0 8px;}");
        sb.append("p{color:#7a7260;font-size:13px;margin:0;}");
        sb.append("</style></head><body>");
        sb.append("<div class=\"box\">");
        sb.append("<div class=\"spinner\"></div>");
        sb.append("<h1>Redirecting to PayU</h1>");
        sb.append("<p>Please wait, do not close this window...</p>");
        sb.append("</div>");

        sb.append("<form id=\"payuForm\" action=\"").append(payuUrl).append("\" method=\"post\">");
        sb.append("<input type=\"hidden\" name=\"key\" value=\"").append(esc(key)).append("\" />");
        sb.append("<input type=\"hidden\" name=\"txnid\" value=\"").append(esc(txnId)).append("\" />");
        sb.append("<input type=\"hidden\" name=\"amount\" value=\"").append(esc(amount)).append("\" />");
        sb.append("<input type=\"hidden\" name=\"productinfo\" value=\"").append(esc(productInfo)).append("\" />");
        sb.append("<input type=\"hidden\" name=\"firstname\" value=\"").append(esc(firstname)).append("\" />");
        sb.append("<input type=\"hidden\" name=\"email\" value=\"").append(esc(email)).append("\" />");
        sb.append("<input type=\"hidden\" name=\"phone\" value=\"").append(esc(phone)).append("\" />");
        sb.append("<input type=\"hidden\" name=\"surl\" value=\"").append(esc(surl)).append("\" />");
        sb.append("<input type=\"hidden\" name=\"furl\" value=\"").append(esc(furl)).append("\" />");
        sb.append("<input type=\"hidden\" name=\"hash\" value=\"").append(esc(hash)).append("\" />");
        sb.append("<input type=\"hidden\" name=\"udf1\" value=\"").append(esc(udf1)).append("\" />");
        sb.append("<input type=\"hidden\" name=\"udf2\" value=\"").append(esc(udf2)).append("\" />");
        sb.append("<input type=\"hidden\" name=\"udf3\" value=\"").append(esc(udf3)).append("\" />");
        sb.append("<input type=\"hidden\" name=\"udf4\" value=\"").append(esc(udf4)).append("\" />");
        sb.append("<input type=\"hidden\" name=\"udf5\" value=\"").append(esc(udf5)).append("\" />");
        sb.append("<input type=\"hidden\" name=\"service_provider\" value=\"payu_paisa\" />");
        sb.append("</form>");

        sb.append("<script>setTimeout(function(){document.getElementById('payuForm').submit();},400);</script>");
        sb.append("</body></html>");

        return sb.toString();
    }
}