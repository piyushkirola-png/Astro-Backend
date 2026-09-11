package com.astrologytalk.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KundaliBasicResponse {

    // ----- Birth Details (from user record) -----
    private String name;
    private String gender;
    private LocalDate dateOfBirth;
    private String timeOfBirth;
    private String placeOfBirth;
    private Double birthLat;
    private Double birthLng;
    private String birthTimezone;

    // ----- Panchang (from CSV) -----
    private String panchangTithi;
    private String karana;
    private String yoga;
    private String nakshatra;
    private String nakshatraLord;
    private String ascendant;
    private String ascendantLord;
    private String sunrise;
    private String sunset;

    // ----- Avakhada (from CSV) -----
    private String varna;
    private String vashya;
    private String yoni;
    private String gan;
    private String nadi;
    private String sign;
    private String signLord;
    private String charan;
    private String tatva;
    private String nameAlphabet;
    private String paya;
    private String yunja;
}