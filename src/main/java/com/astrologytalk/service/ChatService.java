package com.astrologytalk.service;

import com.astrologytalk.common.exception.PaymentRequiredException;
import com.astrologytalk.dto.response.ChatMessageResponse;
import com.astrologytalk.dto.response.ChatSessionDetailResponse;
import com.astrologytalk.dto.response.ChatSessionResponse;
import com.astrologytalk.entity.ChatMessage;
import com.astrologytalk.entity.ChatSession;
import com.astrologytalk.entity.MessageRole;
import com.astrologytalk.entity.User;
import com.astrologytalk.repository.ChatMessageRepository;
import com.astrologytalk.repository.ChatSessionRepository;
import com.astrologytalk.repository.UserRepository;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

  private final ChatSessionRepository sessionRepo;
  private final ChatMessageRepository messageRepo;
  private final UserRepository userRepo;
  private final AiUsageLogService aiUsageLogService;
  private final GeminiService geminiService;

  // ---------- Create session + greeting ----------
  @Transactional
  public ChatSessionDetailResponse createSession(Long userId) {
    User user = userRepo.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

    ChatSession session = new ChatSession();
    session.setUser(user);
    session.setTitle("New Chat");
    ChatSession saved = sessionRepo.save(session);

    String firstName = user.getName().split(" ")[0];
    addMessage(saved, MessageRole.SYSTEM, "Welcome to Jyotish AI!");
    addMessage(saved, MessageRole.SYSTEM, "Jyotish AI will join within 10 seconds.");
    addMessage(saved, MessageRole.SYSTEM, "Please share your question in the meantime.");
    addMessage(saved, MessageRole.SYSTEM, "Jyotish AI has joined.");
    addMessage(
        saved,
        MessageRole.ASSISTANT,
        "Jai Shri Ram " + firstName + " ji! Bataiye, aap kya jaanna chahte hain?");

    return getSessionDetail(saved.getId(), userId);
  }

  // ---------- List sessions ----------
  public List<ChatSessionResponse> listSessions(Long userId) {
    return sessionRepo.findByUserIdOrderByIsPinnedDescPinnedAtDescUpdatedAtDesc(userId).stream()
        .map(this::toSessionSummary)
        .collect(Collectors.toList());
  }

  // ---------- Get session detail ----------
  public ChatSessionDetailResponse getSessionDetail(Long sessionId, Long userId) {
    ChatSession session =
        sessionRepo
            .findById(sessionId)
            .orElseThrow(() -> new RuntimeException("Session not found"));
    if (!session.getUser().getId().equals(userId)) throw new RuntimeException("Access denied");

    List<ChatMessageResponse> messages =
        messageRepo.findBySessionIdOrderByCreatedAtAsc(sessionId).stream()
            .map(this::toMessageResponse)
            .collect(Collectors.toList());

    User user = userRepo.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

    return ChatSessionDetailResponse.builder()
        .id(session.getId())
        .title(session.getTitle())
        .createdAt(session.getCreatedAt())
        .updatedAt(session.getUpdatedAt())
        .messages(messages)
        .chatSecondsBalance(user.getChatSecondsBalance())
        .build();
  }

  // ---------- Send message ----------
  @Transactional
  public ChatMessageResponse sendMessage(Long sessionId, Long userId, String content) {
    ChatSession session =
        sessionRepo
            .findById(sessionId)
            .orElseThrow(() -> new RuntimeException("Session not found"));
    if (!session.getUser().getId().equals(userId)) throw new RuntimeException("Access denied");

    User user = userRepo.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

    int balance = user.getChatSecondsBalance() == null ? 0 : user.getChatSecondsBalance();
    if (balance <= 0) {
      throw new PaymentRequiredException("Your chat time is over. Please recharge to continue.");
    }

    addMessage(session, MessageRole.USER, content);

    try {
      aiUsageLogService.recordMessage(userId, sessionId);
    } catch (Exception e) {
      log.warn("[Chat] Failed to log message usage: {}", e.getMessage());
    }

    if ("New Chat".equals(session.getTitle())) {
      session.setTitle(shorten(content, 40));
      sessionRepo.save(session);
    }

    List<ChatMessage> history = messageRepo.findBySessionIdOrderByCreatedAtAsc(sessionId);
    // String aiText = openAiService.generateReply(history);
    String aiText = geminiService.generateReply(history);

    ChatMessage aiMsg = addMessage(session, MessageRole.ASSISTANT, aiText);
    return toMessageResponse(aiMsg);
  }

  // ---------- Heartbeat — decrement balance ----------
  @Transactional
  public int heartbeat(Long userId, Long sessionId, int seconds) {
    User user = userRepo.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

    int current = user.getChatSecondsBalance() == null ? 0 : user.getChatSecondsBalance();
    int actuallyUsed = Math.min(current, seconds);
    int updated = Math.max(0, current - seconds);
    user.setChatSecondsBalance(updated);
    userRepo.save(user);

    if (sessionId != null && actuallyUsed > 0) {
      try {
        aiUsageLogService.recordSeconds(userId, sessionId, actuallyUsed);
      } catch (Exception e) {
        log.warn("[Chat] Failed to log seconds usage: {}", e.getMessage());
      }
    }

    return updated;
  }

  // ---------- Delete session ----------
  @Transactional
  public void deleteSession(Long sessionId, Long userId) {
    ChatSession session =
        sessionRepo
            .findById(sessionId)
            .orElseThrow(() -> new RuntimeException("Session not found"));
    if (!session.getUser().getId().equals(userId)) throw new RuntimeException("Access denied");
    sessionRepo.delete(session);
  }

  // ---------- Rename session ----------
  @Transactional
  public void renameSession(Long sessionId, Long userId, String title) {
    ChatSession session =
        sessionRepo
            .findById(sessionId)
            .orElseThrow(() -> new RuntimeException("Session not found"));
    if (!session.getUser().getId().equals(userId)) throw new RuntimeException("Access denied");
    session.setTitle(title);
    sessionRepo.save(session);
  }

  @Transactional
  public void togglePin(Long sessionId, Long userId) {
    ChatSession session =
        sessionRepo
            .findById(sessionId)
            .orElseThrow(() -> new RuntimeException("Session not found"));
    if (!session.getUser().getId().equals(userId)) throw new RuntimeException("Access denied");

    boolean newPinned = !Boolean.TRUE.equals(session.getIsPinned());
    session.setIsPinned(newPinned);
    session.setPinnedAt(newPinned ? java.time.LocalDateTime.now() : null);
    sessionRepo.save(session);
  }

  @Transactional
  public void bulkDelete(java.util.List<Long> ids, Long userId) {
    if (ids == null || ids.isEmpty()) return;

    java.util.List<ChatSession> sessions = sessionRepo.findAllById(ids);
    for (ChatSession s : sessions) {
      if (!s.getUser().getId().equals(userId)) {
        throw new RuntimeException("Access denied for session " + s.getId());
      }
    }
    sessionRepo.deleteAll(sessions);
  }

  @Transactional
  public void bulkPin(java.util.List<Long> ids, Long userId, boolean pinned) {
    if (ids == null || ids.isEmpty()) return;

    java.util.List<ChatSession> sessions = sessionRepo.findAllById(ids);
    for (ChatSession s : sessions) {
      if (!s.getUser().getId().equals(userId)) {
        throw new RuntimeException("Access denied for session " + s.getId());
      }
      s.setIsPinned(pinned);
      s.setPinnedAt(pinned ? java.time.LocalDateTime.now() : null);
    }
    sessionRepo.saveAll(sessions);
  }

  // ---------- Helpers ----------
  private ChatMessage addMessage(ChatSession session, MessageRole role, String content) {
    ChatMessage msg = new ChatMessage();
    msg.setSession(session);
    msg.setRole(role);
    msg.setContent(content);
    return messageRepo.save(msg);
  }

  private ChatMessageResponse toMessageResponse(ChatMessage m) {
    return ChatMessageResponse.builder()
        .id(m.getId())
        .role(m.getRole().name())
        .content(m.getContent())
        .createdAt(m.getCreatedAt())
        .build();
  }

  private ChatSessionResponse toSessionSummary(ChatSession s) {
    List<ChatMessage> msgs = messageRepo.findBySessionIdOrderByCreatedAtAsc(s.getId());
    String preview = msgs.isEmpty() ? "" : shorten(msgs.get(msgs.size() - 1).getContent(), 60);
    return ChatSessionResponse.builder()
        .id(s.getId())
        .title(s.getTitle())
        .createdAt(s.getCreatedAt())
        .updatedAt(s.getUpdatedAt())
        .lastMessagePreview(preview)
        .isPinned(Boolean.TRUE.equals(s.getIsPinned()))
        .build();
  }

  private String shorten(String text, int max) {
    if (text == null) return "";
    String t = text.trim().replaceAll("\\s+", " ");
    return t.length() > max ? t.substring(0, max) + "..." : t;
  }
}
