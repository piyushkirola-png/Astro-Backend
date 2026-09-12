package com.astrologytalk.repository;

import com.astrologytalk.entity.WalletPackage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WalletPackageRepository extends JpaRepository<WalletPackage, Long> {

    List<WalletPackage> findByIsActiveTrueOrderByDisplayOrderAsc();
}