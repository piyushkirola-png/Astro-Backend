package com.astrologytalk.config;

import com.astrologytalk.entity.*;
import com.astrologytalk.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PaymentCategoryRepository categoryRepository;
    private final WalletPackageRepository packageRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String ADMIN_NAME     = "Admin";
    private static final String ADMIN_EMAIL    = "admin@gmail.com";
    private static final String ADMIN_PASSWORD = "Admin@123";
    private static final String ADMIN_PHONE    = "+919876543210";

    @Override
    public void run(String... args) {
        seedAdmin();
        seedPaymentCategories();
        seedWalletPackages();
    }

    private void seedAdmin() {
        log.info("🔍 DataSeeder: checking admin '{}'", ADMIN_EMAIL);

        if (userRepository.existsByEmail(ADMIN_EMAIL)) {
            log.info("✅ Admin already exists — skipping");
            return;
        }

        User admin = new User();
        admin.setName(ADMIN_NAME);
        admin.setEmail(ADMIN_EMAIL);
        admin.setPassword(passwordEncoder.encode(ADMIN_PASSWORD));
        admin.setPhone(ADMIN_PHONE);
        admin.setRole(Role.ADMIN);
        admin.setIsActive(true);
        userRepository.save(admin);

        log.info("🌱 Admin created");
    }

    private void seedPaymentCategories() {
        if (categoryRepository.count() > 0) {
            log.info("✅ Payment categories already seeded — skipping");
            return;
        }

        PaymentCategory report = new PaymentCategory();
        report.setCode("REPORT");
        report.setName("Kundali Report");
        report.setAmount(new BigDecimal("500.00"));
        report.setDescription("Full Vedic analysis of your birth chart");
        report.setIsActive(true);
        categoryRepository.save(report);

        log.info("🌱 Payment category REPORT seeded (₹500)");
    }

    private void seedWalletPackages() {
        if (packageRepository.count() > 0) {
            log.info("✅ Wallet packages already seeded — skipping");
            return;
        }

        createPackage(new BigDecimal("50.00"),  120,   "2 Minutes",  1);
        createPackage(new BigDecimal("100.00"), 300,   "5 Minutes",  2);
        createPackage(new BigDecimal("180.00"), 600,   "10 Minutes", 3);
        createPackage(new BigDecimal("450.00"), 1800,  "30 Minutes", 4);
        createPackage(new BigDecimal("800.00"), 3600,  "1 Hour",     5);

        log.info("🌱 Wallet packages seeded (5 slabs)");
    }

    private void createPackage(BigDecimal amount, int seconds, String label, int order) {
        WalletPackage p = new WalletPackage();
        p.setAmount(amount);
        p.setSecondsCredited(seconds);
        p.setLabel(label);
        p.setDisplayOrder(order);
        p.setIsActive(true);
        packageRepository.save(p);
    }
}