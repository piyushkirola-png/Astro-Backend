package com.astrologytalk.repository;

import com.astrologytalk.entity.PaymentCategory;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentCategoryRepository extends JpaRepository<PaymentCategory, Long> {

  Optional<PaymentCategory> findByCode(String code);

  List<PaymentCategory> findByIsActiveTrueOrderByIdAsc();
}
