package com.astrologytalk.repository;

import com.astrologytalk.entity.WalletPackage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WalletPackageRepository extends JpaRepository<WalletPackage, Long> {

  List<WalletPackage> findByIsActiveTrueOrderByDisplayOrderAsc();
}
