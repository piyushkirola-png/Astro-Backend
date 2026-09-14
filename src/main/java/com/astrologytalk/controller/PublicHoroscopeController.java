package com.astrologytalk.controller;

import com.astrologytalk.common.response.ApiResponse;
import com.astrologytalk.service.PublicHoroscopeService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/horoscope")
@RequiredArgsConstructor
public class PublicHoroscopeController {

  private final PublicHoroscopeService publicHoroscopeService;

  @GetMapping("/{period}")
  public ResponseEntity<ApiResponse<List<Map<String, Object>>>> hub(
      @PathVariable String period) {
    List<Map<String, Object>> result = publicHoroscopeService.getHubList(period);
    return ResponseEntity.ok(ApiResponse.success("OK", result));
  }

  @GetMapping("/{period}/{zodiac}")
  public ResponseEntity<ApiResponse<Map<String, Object>>> detail(
      @PathVariable String period, @PathVariable String zodiac) {
    Map<String, Object> result = publicHoroscopeService.getDetail(period, zodiac);
    return ResponseEntity.ok(ApiResponse.success("OK", result));
  }
}