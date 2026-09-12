package com.astrologytalk.repository;

import com.astrologytalk.entity.ReportEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportEntryRepository extends JpaRepository<ReportEntry, Long> {

    List<ReportEntry> findByTypeAndIsActiveTrueOrderByIdAsc(String type);

    List<ReportEntry> findByTypeAndZodiacAndIsActiveTrueOrderByIdAsc(String type, String zodiac);
}