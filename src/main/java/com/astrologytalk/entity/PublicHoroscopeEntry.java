package com.astrologytalk.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "public_horoscope_entries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PublicHoroscopeEntry {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 20)
  private String zodiac;

  @Column(name = "sanskrit_name", nullable = false, length = 50)
  private String sanskritName;

  @Column(nullable = false, length = 50)
  private String symbol;

  @Column(name = "symbol_emoji", nullable = false, length = 8)
  private String symbolEmoji;

  @Column(name = "ruling_planet", nullable = false, length = 50)
  private String rulingPlanet;

  @Column(name = "tarot_card", nullable = false, length = 50)
  private String tarotCard;

  @Column(name = "lucky_stone", nullable = false, length = 100)
  private String luckyStone;

  @Column(name = "date_range", nullable = false, length = 50)
  private String dateRange;

  @Column(nullable = false, length = 20)
  private String period;

  @Column(name = "main_text", columnDefinition = "TEXT", nullable = false)
  private String mainText;

  @Column(name = "lucky_number", nullable = false)
  private Integer luckyNumber;

  @Column(name = "lucky_color", nullable = false, length = 32)
  private String luckyColor;

  @Column(name = "auspicious_time", nullable = false, length = 50)
  private String auspiciousTime;

  @Column(nullable = false, length = 50)
  private String mood;

  @Column(name = "love_score", nullable = false)
  private Integer loveScore;

  @Column(name = "love_text", columnDefinition = "TEXT", nullable = false)
  private String loveText;

  @Column(name = "finance_score", nullable = false)
  private Integer financeScore;

  @Column(name = "finance_text", columnDefinition = "TEXT", nullable = false)
  private String financeText;

  @Column(name = "career_score", nullable = false)
  private Integer careerScore;

  @Column(name = "career_text", columnDefinition = "TEXT", nullable = false)
  private String careerText;

  @Column(name = "health_score", nullable = false)
  private Integer healthScore;

  @Column(name = "health_text", columnDefinition = "TEXT", nullable = false)
  private String healthText;

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