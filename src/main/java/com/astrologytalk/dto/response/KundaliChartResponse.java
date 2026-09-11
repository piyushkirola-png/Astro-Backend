package com.astrologytalk.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

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