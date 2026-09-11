package com.astrologytalk.repository;

import com.astrologytalk.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findBySessionIdOrderByCreatedAtAsc(Long sessionId);

    // Count messages of a given role on a given date (for AI usage chart)
    @Query("""
        SELECT COUNT(m) FROM ChatMessage m
        WHERE m.role = com.astrologytalk.entity.MessageRole.USER
          AND DATE(m.createdAt) = :date
    """)
    long countUserMessagesByDate(@Param("date") LocalDate date);
}