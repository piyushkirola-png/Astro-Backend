package com.astrologytalk.dto.response;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
