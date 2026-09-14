package com.astrologytalk.controller;

import com.astrologytalk.common.response.ApiResponse;
import com.astrologytalk.entity.AiUsageLog;
import com.astrologytalk.entity.User;
import com.astrologytalk.service.AiUsageLogService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user/ai-usage")
@RequiredArgsConstructor
public class UserAiUsageController {

  private final AiUsageLogService aiUsageLogService;

  @GetMapping
  public ResponseEntity<ApiResponse<List<AiUsageLog>>> getUsage(
      @AuthenticationPrincipal User user) {
    List<AiUsageLog> logs = aiUsageLogService.getUserUsage(user.getId());
    return ResponseEntity.ok(ApiResponse.success("OK", logs));
  }
}
