package com.astrologytalk.repository;

import com.astrologytalk.entity.DashaPeriod;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DashaPeriodRepository extends JpaRepository<DashaPeriod, Long> {

  List<DashaPeriod> findByZodiacOrderByStartOffsetYearsAsc(String zodiac);
}
