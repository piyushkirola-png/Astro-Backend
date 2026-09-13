package com.astrologytalk.controller;

import com.astrologytalk.common.response.ApiResponse;
import com.astrologytalk.dto.response.PaymentResponse;
import com.astrologytalk.dto.response.WalletPackageResponse;
import com.astrologytalk.service.AdminPaymentService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/payments")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminPaymentController {

  private final AdminPaymentService adminPaymentService;
  private final com.astrologytalk.repository.PaymentRepository paymentRepository;
  private final com.astrologytalk.service.InvoicePdfService invoicePdfService;

  @GetMapping("/wallet/packages")
  public ResponseEntity<ApiResponse<List<WalletPackageResponse>>> listPackages() {
    return ResponseEntity.ok(
        ApiResponse.success("Packages retrieved", adminPaymentService.listAllPackages()));
  }

  @PatchMapping("/wallet/packages/{id}/price")
  public ResponseEntity<ApiResponse<WalletPackageResponse>> updatePrice(
      @PathVariable Long id, @RequestBody Map<String, Object> body) {

    Object amountRaw = body.get("amount");
    if (amountRaw == null) {
      throw new RuntimeException("amount is required");
    }

    BigDecimal amount;
    try {
      amount = new BigDecimal(amountRaw.toString());
    } catch (NumberFormatException e) {
      throw new RuntimeException("Invalid amount");
    }

    WalletPackageResponse response = adminPaymentService.updatePackagePrice(id, amount);
    return ResponseEntity.ok(ApiResponse.success("Price updated", response));
  }

  @PatchMapping("/wallet/packages/{id}/toggle")
  public ResponseEntity<ApiResponse<WalletPackageResponse>> toggleActive(
      @PathVariable Long id, @RequestBody Map<String, Object> body) {

    Object activeRaw = body.get("active");
    if (activeRaw == null) {
      throw new RuntimeException("active is required");
    }
    boolean active = Boolean.parseBoolean(activeRaw.toString());

    WalletPackageResponse response = adminPaymentService.togglePackageActive(id, active);
    return ResponseEntity.ok(ApiResponse.success("Status updated", response));
  }

  @GetMapping("/{orderId}/invoice")
  public ResponseEntity<byte[]> downloadInvoice(@PathVariable String orderId) {
    com.astrologytalk.entity.Payment payment =
        paymentRepository
            .findByGatewayOrderId(orderId)
            .orElseThrow(() -> new RuntimeException("Payment not found"));

    byte[] pdf = invoicePdfService.generate(payment);

    org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
    headers.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
    headers.setContentDispositionFormData("attachment", orderId + ".pdf");

    return new ResponseEntity<>(pdf, headers, org.springframework.http.HttpStatus.OK);
  }

  @GetMapping("/all")
  public ResponseEntity<ApiResponse<List<PaymentResponse>>> listAll() {
    return ResponseEntity.ok(
        ApiResponse.success("Payments retrieved", adminPaymentService.listAllPayments()));
  }
}
