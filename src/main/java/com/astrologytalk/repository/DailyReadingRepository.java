package com.astrologytalk.repository;

import com.astrologytalk.entity.DailyReading;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface DailyReadingRepository extends JpaRepository<DailyReading, Long> {

    Optional<DailyReading> findByUserIdAndReadingDate(Long userId, LocalDate readingDate);
}