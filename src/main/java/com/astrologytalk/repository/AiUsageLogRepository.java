package com.astrologytalk.repository;

import com.astrologytalk.entity.AiUsageLog;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AiUsageLogRepository extends JpaRepository<AiUsageLog, Long> {

  Optional<AiUsageLog> findByUserIdAndSessionIdAndUsageDate(
      Long userId, Long sessionId, LocalDate usageDate);

  List<AiUsageLog> findByUserIdOrderByUsageDateDescIdDesc(Long userId);

  @Query("SELECT l FROM AiUsageLog l " + "WHERE l.userId = :userId AND l.usageDate = :date")
  List<AiUsageLog> findByUserIdAndDate(@Param("userId") Long userId, @Param("date") LocalDate date);
}
