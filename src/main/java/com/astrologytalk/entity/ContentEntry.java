package com.astrologytalk.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "content_library",
    uniqueConstraints =
        @UniqueConstraint(columnNames = {"signature", "category", "variant", "language"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentEntry {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 64)
  private String signature;

  @Column(nullable = false, length = 32)
  private String category;

  @Column(nullable = false)
  private Integer variant;

  @Column(nullable = false, length = 16)
  private String language;

  @Column(nullable = false, length = 16)
  @Builder.Default
  private String tone = "NEUTRAL";

  @Column(nullable = false, columnDefinition = "TEXT")
  private String text;
}
