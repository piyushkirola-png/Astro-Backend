package com.astrologytalk.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "daily_readings",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "reading_date"})
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyReading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "reading_date", nullable = false)
    private LocalDate readingDate;

    @Column(nullable = false, length = 64)
    private String signature;

    @Column(name = "lucky_number", nullable = false)
    private Integer luckyNumber;

    @Column(name = "lucky_color", nullable = false, length = 32)
    private String luckyColor;

    @Column(name = "energy_level", nullable = false)
    private Integer energyLevel;

    @Column(name = "mood_trend", nullable = false, length = 16)
    private String moodTrend;

    @Column(name = "forecast_text", nullable = false, columnDefinition = "TEXT")
    private String forecastText;

    @Column(name = "love_text", columnDefinition = "TEXT")
    private String loveText;

    @Column(name = "career_text", columnDefinition = "TEXT")
    private String careerText;

    @Column(name = "wellness_text", columnDefinition = "TEXT")
    private String wellnessText;

    @Column(name = "health_text", columnDefinition = "TEXT")
    private String healthText;

    @Column(name = "finance_text", columnDefinition = "TEXT")
    private String financeText;

    @Column(nullable = false, length = 16)
    private String language = "HINGLISH";

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (language == null) language = "HINGLISH";
    }
}