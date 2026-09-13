package com.astrologytalk.repository;

import com.astrologytalk.entity.PlanetaryPosition;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlanetaryPositionRepository extends JpaRepository<PlanetaryPosition, Long> {

  List<PlanetaryPosition> findByZodiacOrderByIdAsc(String zodiac);
}
