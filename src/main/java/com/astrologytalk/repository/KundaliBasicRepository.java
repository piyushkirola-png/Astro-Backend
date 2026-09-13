package com.astrologytalk.repository;

import com.astrologytalk.entity.KundaliBasic;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KundaliBasicRepository extends JpaRepository<KundaliBasic, Long> {

  Optional<KundaliBasic> findByZodiac(String zodiac);
}
