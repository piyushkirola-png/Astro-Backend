package com.astrologytalk.repository;

import com.astrologytalk.entity.KundaliBasic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface KundaliBasicRepository extends JpaRepository<KundaliBasic, Long> {

    Optional<KundaliBasic> findByZodiac(String zodiac);
}