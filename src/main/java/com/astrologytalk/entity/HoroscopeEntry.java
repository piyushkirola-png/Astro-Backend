package com.astrologytalk.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "horoscope_entries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HoroscopeEntry {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 20)
  private String zodiac;

  @Column(nullable = false, length = 20)
  private String period;

  @Column(nullable = false)
  private Integer variant;

  @Column(columnDefinition = "TEXT")
  private String content;

  @Column(name = "love_text", columnDefinition = "TEXT")
  private String loveText;

  @Column(name = "career_text", columnDefinition = "TEXT")
  private String careerText;

  @Column(name = "health_text", columnDefinition = "TEXT")
  private String healthText;

  @Column(name = "money_text", columnDefinition = "TEXT")
  private String moneyText;

  @Column(name = "lucky_number")
  private Integer luckyNumber;

  @Column(name = "lucky_color", length = 32)
  private String luckyColor;

  @Column(name = "is_active")
  private Boolean isActive = true;

  @Column(name = "created_at")
  private LocalDateTime createdAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    if (isActive == null) isActive = true;
  }
}
