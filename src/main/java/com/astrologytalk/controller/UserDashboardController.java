package com.astrologytalk.controller;

import com.astrologytalk.common.response.ApiResponse;
import com.astrologytalk.dto.response.DailyReadingResponse;
import com.astrologytalk.entity.User;
import com.astrologytalk.service.DailyReadingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user/dashboard")
@RequiredArgsConstructor
public class UserDashboardController {

    private final DailyReadingService dailyReadingService;

    @GetMapping("/daily")
    public ResponseEntity<ApiResponse<DailyReadingResponse>> getDaily(
            @AuthenticationPrincipal User user) {

        DailyReadingResponse reading =
                dailyReadingService.getOrGenerateForToday(user.getId());

        return ResponseEntity.ok(ApiResponse.success("Daily reading fetched", reading));
    }
}