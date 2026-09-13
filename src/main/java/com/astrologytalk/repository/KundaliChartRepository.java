package com.astrologytalk.repository;

import com.astrologytalk.entity.KundaliChart;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KundaliChartRepository extends JpaRepository<KundaliChart, Long> {

  Optional<KundaliChart> findByZodiacAndChartType(String zodiac, String chartType);
}
