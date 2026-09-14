package com.astrologytalk.service;

import com.astrologytalk.common.exception.ResourceNotFoundException;
import com.astrologytalk.dto.request.PaymentRequest;
import com.astrologytalk.dto.request.WalletRechargeRequest;
import com.astrologytalk.dto.response.*;
import com.astrologytalk.entity.Payment;
import com.astrologytalk.entity.PaymentCategory;
import com.astrologytalk.entity.User;
import com.astrologytalk.entity.WalletPackage;
import com.astrologytalk.paymentgateway.cashfree.CashfreeGatewayService;
import com.astrologytalk.paymentgateway.sabpaisa.SabPaisaGatewayService;
import com.astrologytalk.paymentgateway.payu.PayUGatewayService;
import com.astrologytalk.repository.PaymentCategoryRepository;
import com.astrologytalk.repository.PaymentRepository;
import com.astrologytalk.repository.UserRepository;
import com.astrologytalk.repository.WalletPackageRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

  private final PaymentRepository paymentRepository;
  private final PaymentCategoryRepository categoryRepository;
  private final WalletPackageRepository packageRepository;
  private final UserRepository userRepository;
  private final InvoicePdfService invoicePdfService;
  private final InvoiceNumberService invoiceNumberService;


  private final CashfreeGatewayService cashfreeGatewayService;
  private final SabPaisaGatewayService sabPaisaGatewayService;
  private final PayUGatewayService payUGatewayService;


  private static final String REPORT_CODE = "REPORT";
  private static final java.math.BigDecimal GST_RATE = new java.math.BigDecimal("18.00");
  private static final java.math.BigDecimal HUNDRED = new java.math.BigDecimal("100");

  @Transactional
  public PaymentInitiateResponse initiateReportPurchase(PaymentRequest request, Long userId) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

    PaymentCategory category =
        categoryRepository
            .findByCode(REPORT_CODE)
            .orElseThrow(() -> new ResourceNotFoundException("REPORT category not configured"));

    if (!Boolean.TRUE.equals(category.getIsActive())) {
      throw new RuntimeException("Report purchase is currently disabled");
    }

    // Already purchased?
    if (paymentRepository.existsByUserIdAndCategoryCodeAndStatus(userId, REPORT_CODE, "SUCCESS")) {
      throw new RuntimeException("You have already purchased the Kundali Report");
    }

    java.math.BigDecimal baseAmount = category.getAmount();

    Payment payment = new Payment();
    payment.setUser(user);
    payment.setCategoryCode(REPORT_CODE);
    payment.setPackageId(null);
    applyGst(payment, baseAmount);
    payment.setCurrency("INR");
    payment.setStatus("PENDING");
    payment.setGateway(
        request.getGateway() != null ? request.getGateway().toUpperCase() : "CASHFREE");
    payment.setGatewayOrderId(generateOrderId(userId));
    payment.setCustomerName(user.getName());
    payment.setCustomerEmail(user.getEmail());
    payment.setCustomerPhone(sanitizePhone(user.getPhone()));
    payment.setNotes(request.getNotes());
    payment.setSecondsCredited(0);

    paymentRepository.save(payment);

    return callGatewayAndBuildResponse(payment, user);
  }

  @Transactional
  public PaymentInitiateResponse initiateWalletRecharge(
      WalletRechargeRequest request, Long userId) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

    WalletPackage pkg =
        packageRepository
            .findById(request.getPackageId())
            .orElseThrow(() -> new ResourceNotFoundException("Wallet package not found"));

    if (!Boolean.TRUE.equals(pkg.getIsActive())) {
      throw new RuntimeException("This package is currently unavailable");
    }

    java.math.BigDecimal baseAmount = pkg.getAmount();

    Payment payment = new Payment();
    payment.setUser(user);
    payment.setCategoryCode("WALLET");
    payment.setPackageId(pkg.getId());
    applyGst(payment, baseAmount);
    payment.setCurrency("INR");
    payment.setStatus("PENDING");
    payment.setGateway(
        request.getGateway() != null ? request.getGateway().toUpperCase() : "CASHFREE");
    payment.setGatewayOrderId(generateOrderId(userId));
    payment.setCustomerName(user.getName());
    payment.setCustomerEmail(user.getEmail());
    payment.setCustomerPhone(sanitizePhone(user.getPhone()));
    payment.setNotes("Recharge: " + pkg.getLabel());
    payment.setSecondsCredited(pkg.getSecondsCredited());

    paymentRepository.save(payment);

    return callGatewayAndBuildResponse(payment, user);
  }

  public boolean hasPurchasedReport(Long userId) {
    return paymentRepository.existsByUserIdAndCategoryCodeAndStatus(userId, REPORT_CODE, "SUCCESS");
  }

  public List<WalletPackageResponse> getWalletPackages() {
    return packageRepository.findByIsActiveTrueOrderByDisplayOrderAsc().stream()
        .map(
            p ->
                WalletPackageResponse.builder()
                    .id(p.getId())
                    .amount(p.getAmount())
                    .secondsCredited(p.getSecondsCredited())
                    .label(p.getLabel())
                    .displayOrder(p.getDisplayOrder())
                    .isActive(p.getIsActive())
                    .build())
        .collect(Collectors.toList());
  }

  public List<PaymentResponse> getPaymentHistory(Long userId) {
    return paymentRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
        .map(this::toPaymentResponse)
        .collect(Collectors.toList());
  }

  public PaymentCategoryResponse getReportCategory() {
    PaymentCategory c =
        categoryRepository
            .findByCode(REPORT_CODE)
            .orElseThrow(() -> new ResourceNotFoundException("REPORT category not configured"));
    return PaymentCategoryResponse.builder()
        .id(c.getId())
        .code(c.getCode())
        .name(c.getName())
        .amount(c.getAmount())
        .description(c.getDescription())
        .isActive(c.getIsActive())
        .build();
  }

  public java.util.Map<String, Object> getWalletBalance(Long userId) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

    int seconds = user.getChatSecondsBalance() != null ? user.getChatSecondsBalance() : 0;
    int minutes = seconds / 60;
    int remainingSeconds = seconds % 60;

    java.math.BigDecimal totalBase = paymentRepository.sumWalletBaseAmountByUser(userId);
    Long totalSeconds = paymentRepository.sumWalletSecondsByUser(userId);

    java.math.BigDecimal valueRupees = java.math.BigDecimal.ZERO;

    if (totalBase != null && totalSeconds != null && totalSeconds > 0) {
      java.math.BigDecimal rate =
          totalBase.divide(
              java.math.BigDecimal.valueOf(totalSeconds), 6, java.math.RoundingMode.HALF_UP);

      valueRupees =
          rate.multiply(java.math.BigDecimal.valueOf(seconds))
              .setScale(2, java.math.RoundingMode.HALF_UP);
    }

    java.util.Map<String, Object> result = new java.util.HashMap<>();
    result.put("seconds", seconds);
    result.put("minutes", minutes);
    result.put("remainingSeconds", remainingSeconds);
    result.put("formatted", String.format("%d:%02d", minutes, remainingSeconds));
    result.put("valueRupees", valueRupees);
    return result;
  }

  public org.springframework.http.ResponseEntity<byte[]> generateInvoiceForUser(
      String orderId, Long userId) {
    Payment payment =
        paymentRepository
            .findByGatewayOrderId(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

    if (!payment.getUser().getId().equals(userId)) {
      throw new RuntimeException("Access denied");
    }

    byte[] pdf = invoicePdfService.generate(payment);

    org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
    headers.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
    headers.setContentDispositionFormData("attachment", orderId + ".pdf");

    return new org.springframework.http.ResponseEntity<>(
        pdf, headers, org.springframework.http.HttpStatus.OK);
  }

  private PaymentInitiateResponse callGatewayAndBuildResponse(Payment payment, User user) {
    String gateway = payment.getGateway();

    boolean ok =
        switch (gateway) {
          case "CASHFREE" -> cashfreeGatewayService.initiate(payment, user);
          case "SABPAISA" -> sabPaisaGatewayService.initiate(payment, user);
          case "PAYU" -> payUGatewayService.initiate(payment, user);
          default -> throw new RuntimeException("Unsupported gateway: " + gateway);
        };

    if (!ok || payment.getPaymentLink() == null) {
      payment.setStatus("FAILED");
      payment.setFailureReason("Gateway initiation failed");
      payment.setProcessedAt(LocalDateTime.now());
      paymentRepository.save(payment);
      throw new RuntimeException("Unable to create payment link. Please try again.");
    }

    paymentRepository.save(payment);

    return PaymentInitiateResponse.builder()
        .paymentId(payment.getId())
        .gatewayOrderId(payment.getGatewayOrderId())
        .paymentLink(payment.getPaymentLink())
        .gateway(payment.getGateway())
        .amount(payment.getAmount())
        .status(payment.getStatus())
        .build();
  }

  private PaymentResponse toPaymentResponse(Payment p) {
    return PaymentResponse.builder()
        .id(p.getId())
        .categoryCode(p.getCategoryCode())
        .packageId(p.getPackageId())
        .amount(p.getAmount())
        .currency(p.getCurrency())
        .status(p.getStatus())
        .gateway(p.getGateway())
        .gatewayOrderId(p.getGatewayOrderId())
        .gatewayPaymentId(p.getGatewayPaymentId())
        .paymentLink(p.getPaymentLink())
        .utr(p.getUtr())
        .secondsCredited(p.getSecondsCredited())
        .notes(p.getNotes())
        .completedAt(p.getCompletedAt())
        .createdAt(p.getCreatedAt())
        .build();
  }

  private void applyGst(Payment payment, java.math.BigDecimal baseAmount) {
    java.math.BigDecimal gstAmount =
        baseAmount.multiply(GST_RATE).divide(HUNDRED, 2, java.math.RoundingMode.HALF_UP);

    java.math.BigDecimal totalAmount = baseAmount.add(gstAmount);

    payment.setBaseAmount(baseAmount);
    payment.setGstAmount(gstAmount);
    payment.setGstRate(GST_RATE);
    payment.setAmount(totalAmount);
  }

  private String sanitizePhone(String phone) {
    if (phone == null || phone.isBlank()) return "9999999999";
    String digits = phone.replaceAll("\\D", "");
    if (digits.length() > 10) digits = digits.substring(digits.length() - 10);
    if (digits.length() < 10) return "9999999999";
    return digits;
  }

  private String generateOrderId(Long userId) {
    long ts = System.currentTimeMillis();
    String rand = String.format("%04X", ThreadLocalRandom.current().nextInt(0xFFFF));
    return String.format("ORD-%d-%d-%s", userId, ts, rand);
  }
}
