package com.astrologytalk.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "planetary_positions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanetaryPosition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String zodiac;

    @Column(nullable = false, length = 32)
    private String planet;

    @Column(length = 32)
    private String sign;

    @Column(name = "sign_lord", length = 32)
    private String signLord;

    @Column(length = 64)
    private String nakshatra;

    @Column(name = "nakshatra_lord", length = 32)
    private String nakshatraLord;

    @Column(length = 32)
    private String degree;

    @Column(length = 8)
    private String retro;

    private Integer house;

    @Column(length = 16)
    private String state;

    @Column(length = 32)
    private String status;
}