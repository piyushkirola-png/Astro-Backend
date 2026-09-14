package com.astrologytalk.dto.response;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatSessionResponse {
  private Long id;
  private String title;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private String lastMessagePreview;
  private Boolean isPinned;
}
