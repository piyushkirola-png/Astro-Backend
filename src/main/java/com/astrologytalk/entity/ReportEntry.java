package com.astrologytalk.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "report_entries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReportEntry {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 20)
  private String type;

  @Column(length = 20)
  private String zodiac;

  @Column(length = 64)
  private String category;

  @Column(length = 255)
  private String title;

  @Column(columnDefinition = "TEXT")
  private String content;

  @Column(length = 255)
  private String field1;

  @Column(length = 255)
  private String field2;

  @Column(length = 255)
  private String field3;

  @Column(length = 255)
  private String field4;

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
