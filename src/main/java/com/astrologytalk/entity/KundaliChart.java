package com.astrologytalk.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "kundali_chart",
    uniqueConstraints = @UniqueConstraint(columnNames = {"zodiac", "chart_type"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KundaliChart {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 20)
  private String zodiac;

  @Column(name = "chart_type", nullable = false, length = 4)
  private String chartType; // "D1" or "D9"

  @Column(name = "house1", length = 64)
  private String house1;

  @Column(name = "house2", length = 64)
  private String house2;

  @Column(name = "house3", length = 64)
  private String house3;

  @Column(name = "house4", length = 64)
  private String house4;

  @Column(name = "house5", length = 64)
  private String house5;

  @Column(name = "house6", length = 64)
  private String house6;

  @Column(name = "house7", length = 64)
  private String house7;

  @Column(name = "house8", length = 64)
  private String house8;

  @Column(name = "house9", length = 64)
  private String house9;

  @Column(name = "house10", length = 64)
  private String house10;

  @Column(name = "house11", length = 64)
  private String house11;

  @Column(name = "house12", length = 64)
  private String house12;
}
