package com.astrologytalk.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "dasha_periods")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashaPeriod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String zodiac;

    @Column(nullable = false, length = 32)
    private String planet;

    @Column(name = "start_offset_years", nullable = false)
    private Integer startOffsetYears;

    @Column(name = "end_offset_years", nullable = false)
    private Integer endOffsetYears;

    private Integer house;

    @Column(length = 32)
    private String sign;

    @Column(name = "paragraph1", columnDefinition = "TEXT")
    private String paragraph1;

    @Column(name = "paragraph2", columnDefinition = "TEXT")
    private String paragraph2;
}