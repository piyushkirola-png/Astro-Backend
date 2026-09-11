package com.astrologytalk.service;

import com.astrologytalk.dto.response.DashaPeriodResponse;
import com.astrologytalk.dto.response.KundaliBasicResponse;
import com.astrologytalk.dto.response.KundaliChartResponse;
import com.astrologytalk.dto.response.PlanetaryPositionResponse;

import java.util.List;

public interface KundaliService {
    KundaliBasicResponse getBasic(Long userId);
    List<KundaliChartResponse> getCharts(Long userId);
    List<PlanetaryPositionResponse> getPlanetaryPositions(Long userId);
    List<DashaPeriodResponse> getDashaPeriods(Long userId);
}