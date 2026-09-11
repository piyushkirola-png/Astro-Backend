package com.astrologytalk.repository;

import com.astrologytalk.entity.ContentEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContentLibraryRepository extends JpaRepository<ContentEntry, Long> {

    List<ContentEntry> findBySignatureAndCategoryAndLanguage(
            String signature, String category, String language);
}