package com.astrologytalk.service;

import com.astrologytalk.common.exception.ResourceNotFoundException;
import com.astrologytalk.dto.response.PaymentResponse;
import com.astrologytalk.dto.response.WalletPackageResponse;
import com.astrologytalk.entity.WalletPackage;
import com.astrologytalk.repository.PaymentRepository;
import com.astrologytalk.repository.WalletPackageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminPaymentService {

    private final WalletPackageRepository packageRepository;
    private final PaymentRepository paymentRepository;

    public List<WalletPackageResponse> listAllPackages() {
        return packageRepository.findAll()
                .stream()
                .sorted((a, b) -> Integer.compare(
                        a.getDisplayOrder() == null ? 0 : a.getDisplayOrder(),
                        b.getDisplayOrder() == null ? 0 : b.getDisplayOrder()))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public WalletPackageResponse updatePackagePrice(Long id, BigDecimal newAmount) {
        if (newAmount == null || newAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Amount must be greater than 0");
        }

        WalletPackage pkg = packageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Package not found"));

        pkg.setAmount(newAmount);
        packageRepository.save(pkg);

        log.info("Admin updated package {} amount to ₹{}", id, newAmount);
        return toResponse(pkg);
    }

    @Transactional
    public WalletPackageResponse togglePackageActive(Long id, boolean active) {
        WalletPackage pkg = packageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Package not found"));
        pkg.setIsActive(active);
        packageRepository.save(pkg);
        log.info("Admin toggled package {} active = {}", id, active);
        return toResponse(pkg);
    }

    public List<PaymentResponse> listAllPayments() {
        return paymentRepository.findAll()
                .stream()
                .sorted((a, b) -> {
                    if (a.getCreatedAt() == null) return 1;
                    if (b.getCreatedAt() == null) return -1;
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                })
                .map(this::toPaymentResponse)
                .collect(Collectors.toList());
    }

    private WalletPackageResponse toResponse(WalletPackage p) {
        return WalletPackageResponse.builder()
                .id(p.getId())
                .amount(p.getAmount())
                .secondsCredited(p.getSecondsCredited())
                .label(p.getLabel())
                .displayOrder(p.getDisplayOrder())
                .isActive(p.getIsActive())
                .build();
    }

    private PaymentResponse toPaymentResponse(com.astrologytalk.entity.Payment p) {
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
}