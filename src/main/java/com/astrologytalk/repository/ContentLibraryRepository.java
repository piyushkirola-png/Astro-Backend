package com.astrologytalk.repository;

import com.astrologytalk.entity.ContentEntry;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContentLibraryRepository extends JpaRepository<ContentEntry, Long> {

  List<ContentEntry> findBySignatureAndCategoryAndLanguage(
      String signature, String category, String language);
}
