package com.astrologytalk.controller;

import com.astrologytalk.common.response.ApiResponse;
import com.astrologytalk.dto.request.SendMessageRequest;
import com.astrologytalk.dto.response.ChatMessageResponse;
import com.astrologytalk.dto.response.ChatSessionDetailResponse;
import com.astrologytalk.dto.response.ChatSessionResponse;
import com.astrologytalk.entity.User;
import com.astrologytalk.service.ChatService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

  private final ChatService chatService;

  @PostMapping("/sessions")
  public ResponseEntity<ApiResponse<ChatSessionDetailResponse>> createSession(
      @AuthenticationPrincipal User user) {
    ChatSessionDetailResponse session = chatService.createSession(user.getId());
    return ResponseEntity.ok(ApiResponse.success("Session created", session));
  }

  @GetMapping("/sessions")
  public ResponseEntity<ApiResponse<List<ChatSessionResponse>>> listSessions(
      @AuthenticationPrincipal User user) {
    List<ChatSessionResponse> sessions = chatService.listSessions(user.getId());
    return ResponseEntity.ok(ApiResponse.success("Sessions retrieved", sessions));
  }

  @GetMapping("/sessions/{id}")
  public ResponseEntity<ApiResponse<ChatSessionDetailResponse>> getSession(
      @PathVariable Long id, @AuthenticationPrincipal User user) {
    ChatSessionDetailResponse session = chatService.getSessionDetail(id, user.getId());
    return ResponseEntity.ok(ApiResponse.success("Session retrieved", session));
  }

  @PostMapping("/sessions/{id}/messages")
  public ResponseEntity<ApiResponse<ChatMessageResponse>> sendMessage(
      @PathVariable Long id,
      @AuthenticationPrincipal User user,
      @Valid @RequestBody SendMessageRequest request) {
    ChatMessageResponse aiReply = chatService.sendMessage(id, user.getId(), request.getContent());
    return ResponseEntity.ok(ApiResponse.success("Message sent", aiReply));
  }

  @DeleteMapping("/sessions/{id}")
  public ResponseEntity<ApiResponse<Void>> deleteSession(
      @PathVariable Long id, @AuthenticationPrincipal User user) {
    chatService.deleteSession(id, user.getId());
    return ResponseEntity.ok(ApiResponse.success("Session deleted", null));
  }

  @PatchMapping("/sessions/{id}")
  public ResponseEntity<ApiResponse<Void>> renameSession(
      @PathVariable Long id,
      @AuthenticationPrincipal User user,
      @RequestBody Map<String, String> body) {
    String title = body.getOrDefault("title", "").trim();
    if (title.isEmpty() || title.length() > 80) {
      return ResponseEntity.badRequest().body(ApiResponse.error("Title must be 1–80 characters"));
    }
    chatService.renameSession(id, user.getId(), title);
    return ResponseEntity.ok(ApiResponse.success("Session renamed", null));
  }

  @PatchMapping("/sessions/{id}/pin")
  public ResponseEntity<ApiResponse<Void>> togglePin(
      @PathVariable Long id, @AuthenticationPrincipal User user) {
    chatService.togglePin(id, user.getId());
    return ResponseEntity.ok(ApiResponse.success("Pin toggled", null));
  }

  @PostMapping("/sessions/bulk-delete")
  public ResponseEntity<ApiResponse<Void>> bulkDelete(
      @AuthenticationPrincipal User user, @RequestBody Map<String, List<Long>> body) {
    List<Long> ids = body.getOrDefault("ids", List.of());
    chatService.bulkDelete(ids, user.getId());
    return ResponseEntity.ok(ApiResponse.success("Sessions deleted", null));
  }

  @PostMapping("/sessions/bulk-pin")
  public ResponseEntity<ApiResponse<Void>> bulkPin(
      @AuthenticationPrincipal User user, @RequestBody Map<String, Object> body) {
    @SuppressWarnings("unchecked")
    List<Long> ids = (List<Long>) body.getOrDefault("ids", List.of());
    boolean pinned = Boolean.TRUE.equals(body.get("pinned"));
    chatService.bulkPin(ids, user.getId(), pinned);
    return ResponseEntity.ok(ApiResponse.success("Sessions updated", null));
  }

  @PostMapping("/heartbeat")
  public ResponseEntity<ApiResponse<Map<String, Integer>>> heartbeat(
      @AuthenticationPrincipal User user, @RequestBody(required = false) Map<String, Object> body) {

    int seconds = 10;
    Long sessionId = null;

    if (body != null) {
      Object secObj = body.get("seconds");
      if (secObj instanceof Number) {
        seconds = Math.max(1, Math.min(60, ((Number) secObj).intValue()));
      }
      Object sessObj = body.get("sessionId");
      if (sessObj instanceof Number) {
        sessionId = ((Number) sessObj).longValue();
      }
    }

    int newBalance = chatService.heartbeat(user.getId(), sessionId, seconds);

    return ResponseEntity.ok(
        ApiResponse.success("Heartbeat recorded", Map.of("chatSecondsBalance", newBalance)));
  }
}
