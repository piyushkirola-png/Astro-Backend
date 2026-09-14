package com.astrologytalk.repository;

import com.astrologytalk.entity.OtpCode;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OtpCodeRepository extends JpaRepository<OtpCode, Long> {

  Optional<OtpCode> findTopByEmailAndPurposeAndUsedFalseOrderByCreatedAtDesc(
      String email, String purpose);

  @Modifying
  @Query(
      "UPDATE OtpCode o SET o.used = true "
          + "WHERE o.email = :email AND o.purpose = :purpose AND o.used = false")
  void invalidateAllForEmailAndPurpose(
      @Param("email") String email, @Param("purpose") String purpose);
}
