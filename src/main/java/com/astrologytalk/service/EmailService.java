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

  @Async
  public void sendOtpEmail(String toEmail, String subject, String htmlBody) {
    try {
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
      helper.setFrom(fromEmail, appName);
      helper.setTo(toEmail);
      helper.setSubject(subject);
      helper.setText(htmlBody, true);

      mailSender.send(message);
      log.info("[Email] OTP sent to {}", toEmail);
    } catch (Exception e) {
      log.error("[Email] Failed to send OTP to {}: {}", toEmail, e.getMessage());
      throw new RuntimeException("Failed to send OTP email. Please try again.");
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
            + "<html><body style=\"margin:0;padding:0;background:#faf8f4;font-family:Arial,'Helvetica Neue',sans-serif;color:#17120c;\">"
            + "<div style=\"max-width:560px;margin:0 auto;padding:32px 20px;\">"

            // Outer card
            + "<div style=\"background:#fff;border-radius:20px;overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,0.05);border:1px solid #f4f1ea;\">"

            // Gradient hero
            + "<div style=\"background:linear-gradient(135deg,#b8862a 0%,#f59e0b 100%);padding:36px 28px;text-align:center;\">"
            + "<div style=\"font-size:42px;line-height:1;margin-bottom:12px;\">🌟</div>"
            + "<h1 style=\"margin:0 0 6px;font-size:24px;font-weight:700;color:#ffffff;letter-spacing:-0.3px;\">Welcome to " + appName + "</h1>"
            + "<p style=\"margin:0;color:rgba(255,255,255,0.85);font-size:14px;\">Your journey to cosmic wisdom begins now</p>"
            + "</div>"

            // Body
            + "<div style=\"padding:32px 28px 24px;\">"

            + "<p style=\"margin:0 0 18px;font-size:16px;color:#3f382d;\">Namaste <strong style=\"color:#17120c;\">" + escape(firstName) + "</strong> ji 🙏</p>"

            + "<p style=\"margin:0 0 24px;font-size:14px;line-height:1.6;color:#585043;\">We're delighted to have you at <strong>" + appName + "</strong>. You now have access to AI-powered Vedic guidance for career, love, health, wealth, and spirituality — available 24/7.</p>"

            // Free gift box
            + "<div style=\"background:linear-gradient(135deg,#fffbeb 0%,#fef3c7 100%);border:1px solid #fde68a;border-radius:14px;padding:20px;margin-bottom:24px;text-align:center;\">"
            + "<div style=\"font-size:13px;text-transform:uppercase;letter-spacing:1px;color:#92400e;font-weight:700;margin-bottom:8px;\">🎁 Your Free Welcome Gift</div>"
            + "<div style=\"font-size:28px;font-weight:800;color:#b45309;margin-bottom:4px;\">120 seconds</div>"
            + "<div style=\"font-size:13px;color:#78350f;\">2 minutes of free chat with Jyotish AI</div>"
            + "</div>"

            + "<p style=\"margin:0 0 20px;font-size:14px;color:#585043;\">Just open the chat, ask your first question, and receive guidance rooted in ancient Vedic wisdom.</p>"

            // CTA Button
            + "<div style=\"text-align:center;margin-bottom:24px;\">"
            + "<a href=\"http://localhost:5170/user/chat\" style=\"display:inline-block;background:linear-gradient(135deg,#b8862a 0%,#f59e0b 100%);color:#ffffff;text-decoration:none;font-weight:600;font-size:14px;padding:14px 32px;border-radius:12px;\">Start Chatting Now →</a>"
            + "</div>"

            // Quick features list
            + "<div style=\"border-top:1px solid #f4f1ea;padding-top:20px;\">"
            + "<div style=\"font-size:13px;font-weight:700;color:#17120c;margin-bottom:12px;\">What you can explore:</div>"
            + "<table cellpadding=\"0\" cellspacing=\"0\" width=\"100%\">"

            + "<tr><td style=\"padding:6px 0;font-size:13px;color:#585043;\">🔮 <strong style=\"color:#17120c;\">Personalized Kundali</strong> — detailed birth chart analysis</td></tr>"
            + "<tr><td style=\"padding:6px 0;font-size:13px;color:#585043;\">📅 <strong style=\"color:#17120c;\">Daily Horoscope</strong> — forecasts for today, week, month, year</td></tr>"
            + "<tr><td style=\"padding:6px 0;font-size:13px;color:#585043;\">💬 <strong style=\"color:#17120c;\">AI Astrologer Chat</strong> — 24/7 guidance on any question</td></tr>"
            + "<tr><td style=\"padding:6px 0;font-size:13px;color:#585043;\">📜 <strong style=\"color:#17120c;\">Full Vedic Report</strong> — remedies, doshas, gemstones</td></tr>"

            + "</table>"
            + "</div>"

            + "</div>"

            // Footer
            + "<div style=\"background:#faf8f4;padding:20px 28px;text-align:center;border-top:1px solid #f4f1ea;\">"
            + "<p style=\"margin:0 0 6px;font-size:12px;color:#7a7260;\">Have questions? We're here to help.</p>"
            + "<p style=\"margin:0;font-size:12px;color:#a29a86;\">Reply to this email or contact <a href=\"mailto:support@jyotishai.com\" style=\"color:#b8862a;text-decoration:none;\">support@jyotishai.com</a></p>"
            + "</div>"

            + "</div>"

            // Bottom note
            + "<p style=\"text-align:center;font-size:11px;color:#a29a86;margin:20px 0 0;\">— Team " + appName + " · Vedic wisdom, modern guidance</p>"

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
