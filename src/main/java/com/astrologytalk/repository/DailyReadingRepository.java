package com.astrologytalk.repository;

import com.astrologytalk.entity.DailyReading;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DailyReadingRepository extends JpaRepository<DailyReading, Long> {

  Optional<DailyReading> findByUserIdAndReadingDate(Long userId, LocalDate readingDate);
}
