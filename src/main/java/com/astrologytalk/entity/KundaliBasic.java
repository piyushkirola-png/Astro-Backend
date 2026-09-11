package com.astrologytalk.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "kundali_basic")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KundaliBasic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 20)
    private String zodiac;

    @Column(name = "panchang_tithi", length = 64)
    private String panchangTithi;

    @Column(length = 32)
    private String karana;

    @Column(length = 32)
    private String yoga;

    @Column(length = 64)
    private String nakshatra;

    @Column(name = "nakshatra_lord", length = 32)
    private String nakshatraLord;

    @Column(length = 32)
    private String ascendant;

    @Column(name = "ascendant_lord", length = 32)
    private String ascendantLord;

    @Column(length = 16)
    private String sunrise;

    @Column(length = 16)
    private String sunset;

    @Column(length = 32)
    private String varna;

    @Column(length = 32)
    private String vashya;

    @Column(length = 32)
    private String yoni;

    @Column(length = 32)
    private String gan;

    @Column(length = 32)
    private String nadi;

    @Column(length = 32)
    private String sign;

    @Column(name = "sign_lord", length = 32)
    private String signLord;

    @Column(length = 8)
    private String charan;

    @Column(length = 16)
    private String tatva;

    @Column(name = "name_alphabet", length = 32)
    private String nameAlphabet;

    @Column(length = 32)
    private String paya;

    @Column(length = 32)
    private String yunja;
}