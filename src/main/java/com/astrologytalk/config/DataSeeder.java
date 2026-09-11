package com.astrologytalk.config;

import com.astrologytalk.entity.Role;
import com.astrologytalk.entity.User;
import com.astrologytalk.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String ADMIN_NAME     = "Admin";
    private static final String ADMIN_EMAIL    = "admin@gmail.com";
    private static final String ADMIN_PASSWORD = "Admin@123";
    private static final String ADMIN_PHONE = "+919876543210";

    @Override
    public void run(String... args) {
        log.info("🔍 DataSeeder: checking for admin user '{}'", ADMIN_EMAIL);

        if (userRepository.existsByEmail(ADMIN_EMAIL)) {
            log.info("✅ DataSeeder: admin already exists — skipping (password NOT changed)");
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

        log.info("🌱 DataSeeder: admin CREATED with bcrypt-hashed password");
    }
}