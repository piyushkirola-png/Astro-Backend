package com.astrologytalk.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatSessionDetailResponse {
  private Long id;
  private String title;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private List<ChatMessageResponse> messages;
  private Integer chatSecondsBalance;
}
