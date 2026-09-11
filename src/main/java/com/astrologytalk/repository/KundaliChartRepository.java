package com.astrologytalk.repository;

import com.astrologytalk.entity.KundaliChart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface KundaliChartRepository extends JpaRepository<KundaliChart, Long> {

    Optional<KundaliChart> findByZodiacAndChartType(String zodiac, String chartType);
}