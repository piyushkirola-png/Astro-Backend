package com.astrologytalk.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SendMessageRequest {

  @NotBlank(message = "Message content is required")
  @Size(max = 2000, message = "Message too long (max 2000 characters)")
  private String content;
}
