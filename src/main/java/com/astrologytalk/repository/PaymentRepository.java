package com.astrologytalk.repository;

import com.astrologytalk.entity.Payment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

  Optional<Payment> findByGatewayOrderId(String gatewayOrderId);

  List<Payment> findByUserIdOrderByCreatedAtDesc(Long userId);

  boolean existsByUserIdAndCategoryCodeAndStatus(Long userId, String categoryCode, String status);

  List<Payment> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, String status);

  @Query(
      "SELECT COALESCE(SUM(p.amount), 0) FROM Payment p "
          + "WHERE p.status = 'SUCCESS' AND DATE(p.completedAt) = :date")
  java.math.BigDecimal sumSuccessfulByDate(@Param("date") java.time.LocalDate date);

  @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = 'SUCCESS'")
  java.math.BigDecimal sumAllSuccessful();

  @Query(
      "SELECT p.invoiceNumber FROM Payment p "
          + "WHERE p.invoiceNumber LIKE :yearPrefix% "
          + "ORDER BY p.invoiceNumber DESC")
  List<String> findTopInvoiceByYearPrefix(
      @Param("yearPrefix") String yearPrefix, org.springframework.data.domain.Pageable pageable);

  @Query(
      "SELECT COALESCE(SUM(p.amount), 0) FROM Payment p "
          + "WHERE p.status = 'SUCCESS' AND p.categoryCode = 'WALLET' "
          + "AND p.secondsCredited BETWEEN :minSec AND :maxSec")
  java.math.BigDecimal sumRevenueBySecondsRange(
      @Param("minSec") Integer minSec, @Param("maxSec") Integer maxSec);

  @Query("SELECT p.status, COUNT(p) FROM Payment p GROUP BY p.status")
  List<Object[]> countByStatusGrouped();

  @Query(
      "SELECT COALESCE(SUM(p.baseAmount), 0) FROM Payment p "
          + "WHERE p.user.id = :userId AND p.status = 'SUCCESS' "
          + "AND p.categoryCode = 'WALLET'")
  java.math.BigDecimal sumWalletBaseAmountByUser(@Param("userId") Long userId);

  @Query(
      "SELECT COALESCE(SUM(p.secondsCredited), 0) FROM Payment p "
          + "WHERE p.user.id = :userId AND p.status = 'SUCCESS' "
          + "AND p.categoryCode = 'WALLET'")
  Long sumWalletSecondsByUser(@Param("userId") Long userId);

  default Optional<String> findMaxInvoiceNumberForYear(String yearPrefix) {
    List<String> results =
        findTopInvoiceByYearPrefix(
            yearPrefix, org.springframework.data.domain.PageRequest.of(0, 1));
    return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
  }
}
