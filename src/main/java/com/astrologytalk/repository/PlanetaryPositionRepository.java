package com.astrologytalk.repository;

import com.astrologytalk.entity.PlanetaryPosition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlanetaryPositionRepository extends JpaRepository<PlanetaryPosition, Long> {

    List<PlanetaryPosition> findByZodiacOrderByIdAsc(String zodiac);
}