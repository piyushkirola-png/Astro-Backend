package com.astrologytalk.controller;

import com.astrologytalk.common.response.ApiResponse;
import com.astrologytalk.dto.request.PaymentRequest;
import com.astrologytalk.dto.request.WalletRechargeRequest;
import com.astrologytalk.dto.response.*;
import com.astrologytalk.entity.User;
import com.astrologytalk.service.PaymentService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

  private final PaymentService paymentService;

  @PostMapping("/initiate/report")
  public ResponseEntity<ApiResponse<PaymentInitiateResponse>> initiateReport(
      @AuthenticationPrincipal User user, @Valid @RequestBody PaymentRequest request) {
    PaymentInitiateResponse response = paymentService.initiateReportPurchase(request, user.getId());
    return ResponseEntity.ok(ApiResponse.success("Report payment initiated", response));
  }

  @PostMapping("/initiate/wallet")
  public ResponseEntity<ApiResponse<PaymentInitiateResponse>> initiateWallet(
      @AuthenticationPrincipal User user, @Valid @RequestBody WalletRechargeRequest request) {
    PaymentInitiateResponse response = paymentService.initiateWalletRecharge(request, user.getId());
    return ResponseEntity.ok(ApiResponse.success("Wallet recharge initiated", response));
  }

  @GetMapping("/has-purchased/{categoryCode}")
  public ResponseEntity<ApiResponse<Map<String, Boolean>>> hasPurchased(
      @AuthenticationPrincipal User user, @PathVariable String categoryCode) {
    boolean purchased =
        "REPORT".equalsIgnoreCase(categoryCode) && paymentService.hasPurchasedReport(user.getId());
    return ResponseEntity.ok(ApiResponse.success("OK", Map.of("purchased", purchased)));
  }

  @GetMapping("/wallet/packages")
  public ResponseEntity<ApiResponse<List<WalletPackageResponse>>> getWalletPackages() {
    List<WalletPackageResponse> packages = paymentService.getWalletPackages();
    return ResponseEntity.ok(ApiResponse.success("Packages retrieved", packages));
  }

  @GetMapping("/report/category")
  public ResponseEntity<ApiResponse<PaymentCategoryResponse>> getReportCategory() {
    return ResponseEntity.ok(ApiResponse.success("OK", paymentService.getReportCategory()));
  }

  @GetMapping("/wallet/balance")
  public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> walletBalance(
      @AuthenticationPrincipal User user) {
    return ResponseEntity.ok(
        ApiResponse.success("OK", paymentService.getWalletBalance(user.getId())));
  }

  @GetMapping("/{orderId}/invoice")
  public ResponseEntity<byte[]> downloadInvoice(
      @PathVariable String orderId, @AuthenticationPrincipal User user) {
    return paymentService.generateInvoiceForUser(orderId, user.getId());
  }

  @GetMapping("/history")
  public ResponseEntity<ApiResponse<List<PaymentResponse>>> history(
      @AuthenticationPrincipal User user) {
    List<PaymentResponse> history = paymentService.getPaymentHistory(user.getId());
    return ResponseEntity.ok(ApiResponse.success("History retrieved", history));
  }
}
