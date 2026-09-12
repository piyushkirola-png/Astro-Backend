package com.astrologytalk.service;

import com.astrologytalk.dto.response.AdminStatsResponse;
import com.astrologytalk.dto.response.AdminStatsResponse.DayPoint;
import com.astrologytalk.repository.ChatMessageRepository;
import com.astrologytalk.repository.ChatSessionRepository;
import com.astrologytalk.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;

    private static final int DAYS = 7;
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_DATE;

    @Override
    public AdminStatsResponse getStats() {

        // ---- Total users ----
        long totalUsers = userRepository.count();

        // ---- Active sessions (open chat sessions) ----
        long activeSessions = chatSessionRepository.count();

        // ---- Total revenue (wired to PaymentRepository in Batch 3) ----
        long totalRevenue = 0L;

        // ---- Build last 7 days labels ----
        List<String> last7 = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = DAYS - 1; i >= 0; i--) {
            last7.add(today.minusDays(i).format(ISO));
        }

        // ---- AI usage by day (count of USER messages per day) ----
        List<DayPoint> aiUsage = new ArrayList<>();
        for (String day : last7) {
            long count = chatMessageRepository.countUserMessagesByDate(LocalDate.parse(day));
            aiUsage.add(DayPoint.builder().date(day).value(count).build());
        }

        // ---- Revenue by day ----
        List<DayPoint> revenue = new ArrayList<>();
        for (String day : last7) {
            revenue.add(DayPoint.builder().date(day).value(0L).build());
        }

        return AdminStatsResponse.builder()
                .totalUsers(totalUsers)
                .totalRevenue(totalRevenue)
                .activeSessions(activeSessions)
                .revenueByDay(revenue)
                .aiUsageByDay(aiUsage)
                .build();
    }
}