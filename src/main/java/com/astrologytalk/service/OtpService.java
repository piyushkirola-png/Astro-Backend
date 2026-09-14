package com.astrologytalk.service;

import com.astrologytalk.common.exception.ResourceNotFoundException;
import com.astrologytalk.entity.OtpCode;
import com.astrologytalk.entity.User;
import com.astrologytalk.repository.OtpCodeRepository;
import com.astrologytalk.repository.UserRepository;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

  private final OtpCodeRepository otpRepo;
  private final UserRepository userRepo;
  private final EmailService emailService;

  private static final int OTP_VALIDITY_MINUTES = 5;
  private static final int MAX_ATTEMPTS = 5;

  private static final SecureRandom RANDOM = new SecureRandom();

  public static final String PURPOSE_LOGIN = "LOGIN";
  public static final String PURPOSE_RESET = "RESET";

  @Transactional
  public void sendOtp(String email, String purpose) {
    String normalizedEmail = email.trim().toLowerCase();

    Optional<User> userOpt = userRepo.findByEmail(normalizedEmail);

    if (userOpt.isEmpty()) {
      throw new ResourceNotFoundException("No account found with this email");
    }

    User user = userOpt.get();

    if (!Boolean.TRUE.equals(user.getIsActive())) {
      throw new RuntimeException("Your account is deactivated. Please contact support.");
    }

    otpRepo.invalidateAllForEmailAndPurpose(normalizedEmail, purpose);

    String code = String.format("%06d", RANDOM.nextInt(1_000_000));

    OtpCode otp = new OtpCode();
    otp.setEmail(normalizedEmail);
    otp.setCode(code);
    otp.setPurpose(purpose);
    otp.setExpiresAt(LocalDateTime.now().plusMinutes(OTP_VALIDITY_MINUTES));
    otp.setUsed(false);
    otpRepo.save(otp);

    String subject =
        PURPOSE_LOGIN.equals(purpose)
            ? "Your Login OTP — Jyotish AI"
            : "Your Password Reset OTP — Jyotish AI";

    String body = buildOtpEmailBody(user.getName(), code, purpose);
    emailService.sendOtpEmail(normalizedEmail, subject, body);

    log.info(
        "[OTP] Sent {} OTP to {} (valid {} min)", purpose, normalizedEmail, OTP_VALIDITY_MINUTES);
  }

  @Transactional
  public User verifyOtp(String email, String code, String purpose) {
    String normalizedEmail = email.trim().toLowerCase();

    OtpCode otp =
        otpRepo
            .findTopByEmailAndPurposeAndUsedFalseOrderByCreatedAtDesc(normalizedEmail, purpose)
            .orElseThrow(
                () -> new RuntimeException("No active OTP found. Please request a new one."));

    if (otp.getExpiresAt().isBefore(LocalDateTime.now())) {
      otp.setUsed(true);
      otpRepo.save(otp);
      throw new RuntimeException("OTP has expired. Please request a new one.");
    }

    if (!otp.getCode().equals(code.trim())) {
      throw new RuntimeException("Incorrect OTP. Please try again.");
    }

    otp.setUsed(true);
    otpRepo.save(otp);

    User user =
        userRepo
            .findByEmail(normalizedEmail)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

    return user;
  }

  private String buildOtpEmailBody(String name, String code, String purpose) {
    String firstName = name != null && !name.isBlank() ? name.split("\\s+")[0] : "Friend";

    String heading = PURPOSE_LOGIN.equals(purpose) ? "Your Login Code" : "Your Password Reset Code";

    String actionText =
        PURPOSE_LOGIN.equals(purpose)
            ? "Use this code to log in to your account."
            : "Use this code to reset your password.";

    return "<!DOCTYPE html>"
        + "<html><body style=\"font-family:Arial,sans-serif;background:#faf8f4;margin:0;padding:24px;\">"
        + "<div style=\"max-width:520px;margin:0 auto;background:#fff;border-radius:12px;padding:28px;border:1px solid #e7e3d8;\">"
        + "<h2 style=\"margin:0 0 8px;color:#17120c;\">"
        + heading
        + "</h2>"
        + "<p style=\"color:#585043;margin:0 0 20px;\">Namaste "
        + escape(firstName)
        + " ji,</p>"
        + "<p style=\"color:#585043;\">"
        + actionText
        + "</p>"
        + "<div style=\"background:#fdf8ed;border:2px dashed #b8862a;border-radius:12px;padding:24px;text-align:center;margin:20px 0;\">"
        + "<div style=\"font-size:36px;font-weight:bold;letter-spacing:8px;color:#b8862a;font-family:monospace;\">"
        + code
        + "</div>"
        + "<p style=\"color:#7a7260;font-size:12px;margin:8px 0 0;\">Valid for 5 minutes</p>"
        + "</div>"
        + "<p style=\"color:#7a7260;font-size:12px;\">If you didn't request this code, you can safely ignore this email. Your account is secure.</p>"
        + "<hr style=\"border:none;border-top:1px solid #e7e3d8;margin:20px 0;\">"
        + "<p style=\"color:#a29a86;font-size:12px;margin:0;\">— Team Jyotish AI</p>"
        + "</div></body></html>";
  }

  private String escape(String s) {
    if (s == null) return "";
    return s.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;");
  }
}
