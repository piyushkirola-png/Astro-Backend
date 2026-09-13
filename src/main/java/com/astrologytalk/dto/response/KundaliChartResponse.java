package com.astrologytalk.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KundaliChartResponse {

  private String chartType;
  private String chartLabel;
  private String zodiac;
  private List<String> houses;
}
