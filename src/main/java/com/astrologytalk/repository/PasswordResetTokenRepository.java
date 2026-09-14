package com.astrologytalk.repository;

import com.astrologytalk.entity.PasswordResetToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

  Optional<PasswordResetToken> findByTokenAndUsedFalse(String token);

  @Modifying
  @Query(
      "UPDATE PasswordResetToken t SET t.used = true "
          + "WHERE t.userId = :userId AND t.used = false")
  void invalidateAllForUser(@Param("userId") Long userId);
}
