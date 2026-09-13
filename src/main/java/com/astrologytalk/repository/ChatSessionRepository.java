package com.astrologytalk.repository;

import com.astrologytalk.entity.ChatSession;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

  List<ChatSession> findByUserIdOrderByUpdatedAtDesc(Long userId);
}
