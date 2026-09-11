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
public class DailyReadingResponse {

    private LocalDate readingDate;

    private String zodiacSign;
    private String zodiacSymbol;

    private int luckyNumber;
    private String luckyColor;
    private int energyLevel;
    private String moodTrend;

    private String forecastText;
    private String loveText;
    private String careerText;
    private String wellnessText;
    private String healthText;
    private String financeText;

    private String signature;
    private String language;
    private boolean hasZodiac;
}