package com.astrologytalk.service;

import com.astrologytalk.entity.Payment;
import com.astrologytalk.entity.User;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

  private final JavaMailSender mailSender;
  private final InvoicePdfService invoicePdfService;

  @Value("${spring.mail.username:noreply@jyotishai.com}")
  private String fromEmail;

  @Value("${app.name:Jyotish AI}")
  private String appName;

  /** Send invoice email after successful payment. Runs async so webhook response is fast. */
  @Async
  public void sendInvoiceEmail(User user, Payment payment) {
    if (user == null || user.getEmail() == null) {
      log.warn("[Email] Skip invoice — user or email missing");
      return;
    }

    try {
      String subject = buildSubject(payment);
      String body = buildInvoiceHtml(user, payment);

      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
      helper.setFrom(fromEmail, appName);
      helper.setTo(user.getEmail());
      helper.setSubject(subject);
      helper.setText(body, true);

      try {
        byte[] pdfBytes = invoicePdfService.generate(payment);
        String filename = payment.getGatewayOrderId() + ".pdf";
        helper.addAttachment(filename, new ByteArrayResource(pdfBytes), "application/pdf");
        log.info(
            "[Email] Attached invoice PDF ({} bytes) for order {}",
            pdfBytes.length,
            payment.getGatewayOrderId());
      } catch (Exception e) {
        log.warn(
            "[Email] PDF attachment failed for {}: {}",
            payment.getGatewayOrderId(),
            e.getMessage());
      }

      mailSender.send(message);
      log.info(
          "[Email] Invoice sent to {} for order {}", user.getEmail(), payment.getGatewayOrderId());

    } catch (Exception e) {
      log.error(
          "[Email] Failed to send invoice for order {}: {}",
          payment.getGatewayOrderId(),
          e.getMessage(),
          e);
    }
  }

  /** Send welcome email on signup. */
  @Async
  public void sendWelcomeEmail(User user) {
    if (user == null || user.getEmail() == null) return;

    try {
      String subject = "Welcome to " + appName + " 🙏";
      String body = buildWelcomeHtml(user);

      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
      helper.setFrom(fromEmail, appName);
      helper.setTo(user.getEmail());
      helper.setSubject(subject);
      helper.setText(body, true);

      mailSender.send(message);
      log.info("[Email] Welcome sent to {}", user.getEmail());

    } catch (Exception e) {
      log.error("[Email] Failed to send welcome to {}: {}", user.getEmail(), e.getMessage());
    }
  }

  // ---------- HTML builders ----------

  private String buildSubject(Payment payment) {
    if ("REPORT".equalsIgnoreCase(payment.getCategoryCode())) {
      return "Kundali Report Purchased — Order " + payment.getGatewayOrderId();
    }
    return "Recharge Successful — ₹" + payment.getAmount() + " — " + appName;
  }

  private String buildInvoiceHtml(User user, Payment payment) {
    String firstName = firstName(user.getName());
    String productLine;
    String benefitLine = "";

    if ("REPORT".equalsIgnoreCase(payment.getCategoryCode())) {
      productLine = "Kundali Report (Full Vedic Analysis)";
      benefitLine =
          "<p style=\"margin:8px 0;color:#555;\">Your complete Kundali Report is now unlocked. View it anytime from your dashboard.</p>";
    } else {
      productLine = "AI Chat Wallet Recharge";
      int secs = payment.getSecondsCredited() != null ? payment.getSecondsCredited() : 0;
      int mins = secs / 60;
      benefitLine =
          "<p style=\"margin:8px 0;color:#555;\">You have added <strong>"
              + secs
              + " seconds ("
              + mins
              + " minutes)</strong> to your chat wallet.</p>";
    }

    return "<!DOCTYPE html>"
        + "<html><body style=\"font-family:Arial,sans-serif;background:#faf8f4;margin:0;padding:24px;\">"
        + "<div style=\"max-width:520px;margin:0 auto;background:#fff;border-radius:12px;padding:28px;border:1px solid #e7e3d8;\">"
        + "<h2 style=\"margin:0 0 6px;color:#17120c;\">Thank you, "
        + escape(firstName)
        + " 🙏</h2>"
        + "<p style=\"color:#585043;margin:0 0 20px;\">Your payment was successful.</p>"
        + "<table style=\"width:100%;border-collapse:collapse;margin:16px 0;\">"
        + "<tr><td style=\"padding:8px 0;color:#7a7260;\">Order ID</td><td style=\"padding:8px 0;text-align:right;font-weight:600;\">"
        + escape(payment.getGatewayOrderId())
        + "</td></tr>"
        + "<tr><td style=\"padding:8px 0;color:#7a7260;\">Product</td><td style=\"padding:8px 0;text-align:right;font-weight:600;\">"
        + escape(productLine)
        + "</td></tr>"
        + "<tr><td style=\"padding:8px 0;color:#7a7260;\">Amount Paid</td><td style=\"padding:8px 0;text-align:right;font-weight:600;\">₹"
        + payment.getAmount()
        + "</td></tr>"
        + "<tr><td style=\"padding:8px 0;color:#7a7260;\">Payment Method</td><td style=\"padding:8px 0;text-align:right;font-weight:600;\">"
        + escape(payment.getGateway())
        + "</td></tr>"
        + "<tr><td style=\"padding:8px 0;color:#7a7260;\">Date</td><td style=\"padding:8px 0;text-align:right;font-weight:600;\">"
        + java.time.LocalDateTime.now()
            .format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"))
        + "</td></tr>"
        + "</table>"
        + benefitLine
        + "<hr style=\"border:none;border-top:1px solid #e7e3d8;margin:20px 0;\">"
        + "<p style=\"color:#a29a86;font-size:12px;margin:0;\">Need help? Reply to this email. — Team "
        + appName
        + "</p>"
        + "</div></body></html>";
  }

  private String buildWelcomeHtml(User user) {
    String firstName = firstName(user.getName());
    return "<!DOCTYPE html>"
        + "<html><body style=\"font-family:Arial,sans-serif;background:#faf8f4;margin:0;padding:24px;\">"
        + "<div style=\"max-width:520px;margin:0 auto;background:#fff;border-radius:12px;padding:28px;border:1px solid #e7e3d8;\">"
        + "<h2 style=\"margin:0 0 6px;color:#17120c;\">Welcome, "
        + escape(firstName)
        + " 🙏</h2>"
        + "<p style=\"color:#585043;\">We're delighted to have you at <strong>"
        + appName
        + "</strong>.</p>"
        + "<p style=\"color:#585043;\">As a new user, you get <strong>120 seconds (2 minutes)</strong> of free chat with our Jyotish AI.</p>"
        + "<p style=\"color:#585043;\">Ask your first question now and receive guidance rooted in Vedic wisdom.</p>"
        + "<hr style=\"border:none;border-top:1px solid #e7e3d8;margin:20px 0;\">"
        + "<p style=\"color:#a29a86;font-size:12px;margin:0;\">— Team "
        + appName
        + "</p>"
        + "</div></body></html>";
  }

  private String firstName(String name) {
    if (name == null || name.isBlank()) return "Friend";
    String[] parts = name.trim().split("\\s+");
    return parts[0];
  }

  private String escape(String s) {
    if (s == null) return "";
    return s.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;");
  }
}
