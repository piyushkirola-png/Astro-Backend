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
import com.astrologytalk.repository.PaymentCategoryRepository;
import com.astrologytalk.repository.PaymentRepository;
import com.astrologytalk.repository.UserRepository;
import com.astrologytalk.repository.WalletPackageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentCategoryRepository categoryRepository;
    private final WalletPackageRepository packageRepository;
    private final UserRepository userRepository;
    private final CashfreeGatewayService cashfreeGatewayService;

    private static final String REPORT_CODE = "REPORT";

    // ============================================================
    // 1. Initiate REPORT purchase (fixed ₹500 category)
    // ============================================================
    @Transactional
    public PaymentInitiateResponse initiateReportPurchase(PaymentRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        PaymentCategory category = categoryRepository.findByCode(REPORT_CODE)
                .orElseThrow(() -> new ResourceNotFoundException("REPORT category not configured"));

        if (!Boolean.TRUE.equals(category.getIsActive())) {
            throw new RuntimeException("Report purchase is currently disabled");
        }

        // Already purchased?
        if (paymentRepository.existsByUserIdAndCategoryCodeAndStatus(userId, REPORT_CODE, "SUCCESS")) {
            throw new RuntimeException("You have already purchased the Kundali Report");
        }

        Payment payment = new Payment();
        payment.setUser(user);
        payment.setCategoryCode(REPORT_CODE);
        payment.setPackageId(null);
        payment.setAmount(category.getAmount());
        payment.setCurrency("INR");
        payment.setStatus("PENDING");
        payment.setGateway(request.getGateway() != null ? request.getGateway().toUpperCase() : "CASHFREE");
        payment.setGatewayOrderId(generateOrderId(userId));
        payment.setCustomerName(user.getName());
        payment.setCustomerEmail(user.getEmail());
        payment.setCustomerPhone(user.getPhone());
        payment.setNotes(request.getNotes());
        payment.setSecondsCredited(0);

        paymentRepository.save(payment);

        return callGatewayAndBuildResponse(payment, user);
    }

    // ============================================================
    // 2. Initiate WALLET recharge (₹ amount from wallet_packages)
    // ============================================================
    @Transactional
    public PaymentInitiateResponse initiateWalletRecharge(WalletRechargeRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        WalletPackage pkg = packageRepository.findById(request.getPackageId())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet package not found"));

        if (!Boolean.TRUE.equals(pkg.getIsActive())) {
            throw new RuntimeException("This package is currently unavailable");
        }

        Payment payment = new Payment();
        payment.setUser(user);
        payment.setCategoryCode("WALLET");
        payment.setPackageId(pkg.getId());
        payment.setAmount(pkg.getAmount());
        payment.setCurrency("INR");
        payment.setStatus("PENDING");
        payment.setGateway(request.getGateway() != null ? request.getGateway().toUpperCase() : "CASHFREE");
        payment.setGatewayOrderId(generateOrderId(userId));
        payment.setCustomerName(user.getName());
        payment.setCustomerEmail(user.getEmail());
        payment.setCustomerPhone(user.getPhone());
        payment.setNotes("Recharge: " + pkg.getLabel());
        payment.setSecondsCredited(pkg.getSecondsCredited());

        paymentRepository.save(payment);

        return callGatewayAndBuildResponse(payment, user);
    }

    // ============================================================
    // 3. Has user purchased REPORT?
    // ============================================================
    public boolean hasPurchasedReport(Long userId) {
        return paymentRepository.existsByUserIdAndCategoryCodeAndStatus(userId, REPORT_CODE, "SUCCESS");
    }

    // ============================================================
    // 4. Get wallet packages (for frontend display)
    // ============================================================
    public List<WalletPackageResponse> getWalletPackages() {
        return packageRepository.findByIsActiveTrueOrderByDisplayOrderAsc()
                .stream()
                .map(p -> WalletPackageResponse.builder()
                        .id(p.getId())
                        .amount(p.getAmount())
                        .secondsCredited(p.getSecondsCredited())
                        .label(p.getLabel())
                        .displayOrder(p.getDisplayOrder())
                        .isActive(p.getIsActive())
                        .build())
                .collect(Collectors.toList());
    }

    // ============================================================
    // 5. Get payment history for a user
    // ============================================================
    public List<PaymentResponse> getPaymentHistory(Long userId) {
        return paymentRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toPaymentResponse)
                .collect(Collectors.toList());
    }

    // ============================================================
    // 6. Get REPORT category (for frontend display)
    // ============================================================
    public PaymentCategoryResponse getReportCategory() {
        PaymentCategory c = categoryRepository.findByCode(REPORT_CODE)
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

    // ============================================================
    // Helpers
    // ============================================================

    private PaymentInitiateResponse callGatewayAndBuildResponse(Payment payment, User user) {
        String gateway = payment.getGateway();

        boolean ok = switch (gateway) {
            case "CASHFREE" -> cashfreeGatewayService.initiate(payment, user);
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

    private String generateOrderId(Long userId) {
        long ts = System.currentTimeMillis();
        String rand = String.format("%04X", ThreadLocalRandom.current().nextInt(0xFFFF));
        return String.format("ORD-%d-%d-%s", userId, ts, rand);
    }
}