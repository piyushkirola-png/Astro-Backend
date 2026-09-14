package com.astrologytalk.controller;

import com.astrologytalk.common.response.ApiResponse;
import com.astrologytalk.dto.request.LoginRequest;
import com.astrologytalk.dto.request.RegisterRequest;
import com.astrologytalk.dto.request.ResetPasswordRequest;
import com.astrologytalk.dto.request.SendOtpRequest;
import com.astrologytalk.dto.request.VerifyOtpRequest;
import com.astrologytalk.dto.response.AuthResponse;
import com.astrologytalk.entity.User;
import com.astrologytalk.security.JwtTokenProvider;
import com.astrologytalk.service.AuthService;
import com.astrologytalk.service.OtpService;
import com.astrologytalk.service.PasswordService;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;
  private final OtpService otpService;
  private final PasswordService passwordService;
  private final JwtTokenProvider tokenProvider;

  @PostMapping("/register")
  public ResponseEntity<ApiResponse<AuthResponse>> register(
      @Valid @RequestBody RegisterRequest request) {
    AuthResponse response = authService.register(request);
    return ResponseEntity.ok(ApiResponse.success("Registration successful", response));
  }

  @PostMapping("/login")
  public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
    AuthResponse response = authService.login(request);
    return ResponseEntity.ok(ApiResponse.success("Login successful", response));
  }

  @PostMapping("/logout")
  public ResponseEntity<ApiResponse<Void>> logout(
      @RequestHeader(value = "Authorization", required = false) String authHeader) {

    String token = null;
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      token = authHeader.substring(7);
    }

    authService.logout(token);
    return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null));
  }

  @PostMapping("/otp/send-login")
  public ResponseEntity<ApiResponse<Map<String, String>>> sendLoginOtp(
      @Valid @RequestBody SendOtpRequest request) {
    otpService.sendOtp(request.getEmail(), OtpService.PURPOSE_LOGIN);
    Map<String, String> data = new HashMap<>();
    data.put("message", "OTP sent to your email");
    return ResponseEntity.ok(ApiResponse.success("OTP sent", data));
  }

  @PostMapping("/otp/verify-login")
  public ResponseEntity<ApiResponse<AuthResponse>> verifyLoginOtp(
      @Valid @RequestBody VerifyOtpRequest request) {

    User user =
        otpService.verifyOtp(request.getEmail(), request.getCode(), OtpService.PURPOSE_LOGIN);

    String token = tokenProvider.createToken(user.getEmail(), user.getRole().name());

    AuthResponse response =
        AuthResponse.builder()
            .token(token)
            .userId(user.getId())
            .name(user.getName())
            .email(user.getEmail())
            .role(user.getRole().name())
            .message("Login successful")
            .build();

    return ResponseEntity.ok(ApiResponse.success("Login successful", response));
  }

  @PostMapping("/otp/send-reset")
  public ResponseEntity<ApiResponse<Map<String, String>>> sendResetOtp(
      @Valid @RequestBody SendOtpRequest request) {
    otpService.sendOtp(request.getEmail(), OtpService.PURPOSE_RESET);
    Map<String, String> data = new HashMap<>();
    data.put("message", "Reset code sent to your email");
    return ResponseEntity.ok(ApiResponse.success("Reset OTP sent", data));
  }

  @PostMapping("/otp/verify-reset")
  public ResponseEntity<ApiResponse<Map<String, String>>> verifyResetOtp(
      @Valid @RequestBody VerifyOtpRequest request) {

    User user =
        otpService.verifyOtp(request.getEmail(), request.getCode(), OtpService.PURPOSE_RESET);

    String resetToken = passwordService.createResetToken(user.getId());

    Map<String, String> data = new HashMap<>();
    data.put("resetToken", resetToken);
    data.put("email", user.getEmail());

    return ResponseEntity.ok(ApiResponse.success("OTP verified", data));
  }

  @PostMapping("/password/reset")
  public ResponseEntity<ApiResponse<Void>> resetPassword(
      @Valid @RequestBody ResetPasswordRequest request) {

    passwordService.resetPasswordWithToken(request.getResetToken(), request.getNewPassword());

    return ResponseEntity.ok(ApiResponse.success("Password reset successfully", null));
  }
}
