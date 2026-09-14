package com.astrologytalk.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ai_usage_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AiUsageLog {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "session_id", nullable = false)
  private Long sessionId;

  @Column(name = "usage_date", nullable = false)
  private LocalDate usageDate;

  @Column(name = "message_count", nullable = false)
  private Integer messageCount = 0;

  @Column(name = "seconds_used", nullable = false)
  private Integer secondsUsed = 0;

  @Column(name = "rupees_deducted", nullable = false, precision = 10, scale = 2)
  private BigDecimal rupeesDeducted = BigDecimal.ZERO;

  @Column(name = "created_at")
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
    if (messageCount == null) messageCount = 0;
    if (secondsUsed == null) secondsUsed = 0;
    if (rupeesDeducted == null) rupeesDeducted = BigDecimal.ZERO;
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
