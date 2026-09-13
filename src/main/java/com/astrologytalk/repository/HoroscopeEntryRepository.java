package com.astrologytalk.repository;

import com.astrologytalk.entity.HoroscopeEntry;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HoroscopeEntryRepository extends JpaRepository<HoroscopeEntry, Long> {

  List<HoroscopeEntry> findByZodiacAndPeriodAndIsActiveTrueOrderByVariantAsc(
      String zodiac, String period);

  Optional<HoroscopeEntry> findByZodiacAndPeriodAndVariantAndIsActiveTrue(
      String zodiac, String period, Integer variant);
}
