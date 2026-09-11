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
public class DashaPeriodResponse {

    private String planet;
    private int startOffsetYears;
    private int endOffsetYears;
    private Integer house;
    private String sign;

    private LocalDate startDate;
    private LocalDate endDate;
    private String startDateFormatted;
    private String endDateFormatted;

    private boolean isActive;

    private String paragraph1;
    private String paragraph2;
}