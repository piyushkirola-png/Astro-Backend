package com.astrologytalk.controller;

import com.astrologytalk.common.response.ApiResponse;
import com.astrologytalk.entity.User;
import com.astrologytalk.service.UserHoroscopeService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user/horoscope")
@RequiredArgsConstructor
public class UserHoroscopeController {

  private final UserHoroscopeService horoscopeService;

  @GetMapping
  public ResponseEntity<ApiResponse<Map<String, Object>>> get(
      @AuthenticationPrincipal User user, @RequestParam(defaultValue = "today") String period) {

    Map<String, Object> result = horoscopeService.getForUser(user.getId(), period);
    return ResponseEntity.ok(ApiResponse.success("OK", result));
  }
}
