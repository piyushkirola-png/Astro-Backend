package com.astrologytalk.repository;

import com.astrologytalk.entity.DashaPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DashaPeriodRepository extends JpaRepository<DashaPeriod, Long> {

    List<DashaPeriod> findByZodiacOrderByStartOffsetYearsAsc(String zodiac);
}