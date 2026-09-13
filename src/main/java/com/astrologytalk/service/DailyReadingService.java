package com.astrologytalk.service;

import com.astrologytalk.dto.response.DailyReadingResponse;

public interface DailyReadingService {
  DailyReadingResponse getOrGenerateForToday(Long userId);
}
