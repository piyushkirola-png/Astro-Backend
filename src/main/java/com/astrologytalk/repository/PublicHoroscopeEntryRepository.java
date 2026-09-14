package com.astrologytalk.repository;

import com.astrologytalk.entity.PublicHoroscopeEntry;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PublicHoroscopeEntryRepository extends JpaRepository<PublicHoroscopeEntry, Long> {

  List<PublicHoroscopeEntry> findByPeriodAndIsActiveTrueOrderByZodiacAsc(String period);

  Optional<PublicHoroscopeEntry> findByZodiacAndPeriodAndIsActiveTrue(String zodiac, String period);
}