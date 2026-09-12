package com.astrologytalk.repository;

import com.astrologytalk.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByGatewayOrderId(String gatewayOrderId);

    List<Payment> findByUserIdOrderByCreatedAtDesc(Long userId);

    boolean existsByUserIdAndCategoryCodeAndStatus(
            Long userId, String categoryCode, String status);

    List<Payment> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, String status);
}