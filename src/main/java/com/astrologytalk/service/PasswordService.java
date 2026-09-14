package com.astrologytalk.service;

import com.astrologytalk.common.exception.ResourceNotFoundException;
import com.astrologytalk.entity.PasswordResetToken;
import com.astrologytalk.entity.User;
import com.astrologytalk.repository.PasswordResetTokenRepository;
import com.astrologytalk.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordService {

  private final UserRepository userRepo;
  private final PasswordResetTokenRepository resetTokenRepo;
  private final PasswordEncoder passwordEncoder;

  private static final int RESET_TOKEN_VALIDITY_MINUTES = 15;
  private static final int MIN_PASSWORD_LENGTH = 8;

  @Transactional
  public String createResetToken(Long userId) {
    resetTokenRepo.invalidateAllForUser(userId);

    String token = UUID.randomUUID().toString().replace("-", "");

    PasswordResetToken prt = new PasswordResetToken();
    prt.setUserId(userId);
    prt.setToken(token);
    prt.setExpiresAt(LocalDateTime.now().plusMinutes(RESET_TOKEN_VALIDITY_MINUTES));
    prt.setUsed(false);
    resetTokenRepo.save(prt);

    log.info(
        "[Password] Reset token created for user {} (valid {} min)",
        userId,
        RESET_TOKEN_VALIDITY_MINUTES);
    return token;
  }

  @Transactional
  public void resetPasswordWithToken(String token, String newPassword) {
    validatePasswordStrength(newPassword);

    PasswordResetToken prt =
        resetTokenRepo
            .findByTokenAndUsedFalse(token)
            .orElseThrow(() -> new RuntimeException("Invalid or used reset token"));

    if (prt.getExpiresAt().isBefore(LocalDateTime.now())) {
      prt.setUsed(true);
      resetTokenRepo.save(prt);
      throw new RuntimeException("Reset token has expired. Please request a new one.");
    }

    User user =
        userRepo
            .findById(prt.getUserId())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

    user.setPassword(passwordEncoder.encode(newPassword));
    userRepo.save(user);

    prt.setUsed(true);
    resetTokenRepo.save(prt);

    log.info("[Password] Password reset successful for user {}", user.getId());
  }

  @Transactional
  public void changePassword(Long userId, String currentPassword, String newPassword) {
    validatePasswordStrength(newPassword);

    User user =
        userRepo
            .findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

    if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
      throw new RuntimeException("Current password is incorrect");
    }

    if (passwordEncoder.matches(newPassword, user.getPassword())) {
      throw new RuntimeException("New password must be different from current password");
    }

    user.setPassword(passwordEncoder.encode(newPassword));
    userRepo.save(user);

    log.info("[Password] Password changed for user {}", userId);
  }

  private void validatePasswordStrength(String password) {
    if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
      throw new RuntimeException(
          "Password must be at least " + MIN_PASSWORD_LENGTH + " characters");
    }
    if (!password.matches(".*[A-Za-z].*")) {
      throw new RuntimeException("Password must contain at least one letter");
    }
    if (!password.matches(".*[0-9].*")) {
      throw new RuntimeException("Password must contain at least one number");
    }
  }
}
