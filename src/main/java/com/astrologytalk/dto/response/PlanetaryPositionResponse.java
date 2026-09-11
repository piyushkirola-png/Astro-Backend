package com.astrologytalk.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanetaryPositionResponse {
    private String planet;
    private String sign;
    private String signLord;
    private String nakshatra;
    private String nakshatraLord;
    private String degree;
    private String retro;
    private Integer house;
    private String state;
    private String status;
}