package com.astrologytalk.service;

import com.astrologytalk.dto.response.AdminStatsResponse;
import com.astrologytalk.dto.response.AdminStatsResponse.DayPoint;
import com.astrologytalk.dto.response.AdminStatsResponse.DurationPoint;
import com.astrologytalk.dto.response.AdminStatsResponse.StatusPoint;
import com.astrologytalk.entity.Role;
import com.astrologytalk.repository.ChatMessageRepository;
import com.astrologytalk.repository.PaymentRepository;
import com.astrologytalk.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final PaymentRepository paymentRepository;

    private static final int DAYS = 7;
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_DATE;

    @Override
    public AdminStatsResponse getStats() {

        long totalUsers = userRepository.countByRole(Role.USER);

        long totalMessages = chatMessageRepository.count();

        long totalPayments = paymentRepository.count();

        BigDecimal totalRevenueBd = paymentRepository.sumAllSuccessful();
        long totalRevenue = totalRevenueBd != null ? totalRevenueBd.longValue() : 0L;

        List<String> last7 = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = DAYS - 1; i >= 0; i--) {
            last7.add(today.minusDays(i).format(ISO));
        }

        List<DayPoint> aiUsage = new ArrayList<>();
        for (String day : last7) {
            long count;
            try {
                count = chatMessageRepository.countUserMessagesByDate(LocalDate.parse(day));
            } catch (Exception e) {
                count = 0L;
            }
            aiUsage.add(DayPoint.builder().date(day).value(count).build());
        }

        List<DayPoint> revenue = new ArrayList<>();
        for (String day : last7) {
            long amt;
            try {
                BigDecimal bd = paymentRepository.sumSuccessfulByDate(LocalDate.parse(day));
                amt = bd != null ? bd.longValue() : 0L;
            } catch (Exception e) {
                amt = 0L;
            }
            revenue.add(DayPoint.builder().date(day).value(amt).build());
        }

        List<DurationPoint> revenueByDuration = buildRevenueByDuration();

        List<StatusPoint> statusDistribution = buildStatusDistribution();

        return AdminStatsResponse.builder()
                .totalUsers(totalUsers)
                .totalRevenue(totalRevenue)
                .totalMessages(totalMessages)
                .totalPayments(totalPayments)
                .revenueByDay(revenue)
                .aiUsageByDay(aiUsage)
                .revenueByDuration(revenueByDuration)
                .statusDistribution(statusDistribution)
                .build();
    }

    private List<DurationPoint> buildRevenueByDuration() {
        List<DurationPoint> result = new ArrayList<>();

        result.add(buildDurationPoint("2 min", 0, 180));
        result.add(buildDurationPoint("5 min", 181, 420));
        result.add(buildDurationPoint("10 min", 421, 900));
        result.add(buildDurationPoint("30 min", 901, 2700));
        result.add(buildDurationPoint("1 Hour", 2701, Integer.MAX_VALUE));

        return result;
    }

    private DurationPoint buildDurationPoint(String label, int minSec, int maxSec) {
        long value = 0L;
        try {
            BigDecimal bd = paymentRepository.sumRevenueBySecondsRange(minSec, maxSec);
            value = bd != null ? bd.longValue() : 0L;
        } catch (Exception e) {
            value = 0L;
        }
        return DurationPoint.builder().label(label).value(value).build();
    }

    private List<StatusPoint> buildStatusDistribution() {
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("SUCCESS", 0L);
        counts.put("PENDING", 0L);
        counts.put("FAILED", 0L);

        try {
            List<Object[]> rows = paymentRepository.countByStatusGrouped();
            for (Object[] row : rows) {
                String status = row[0] != null ? row[0].toString() : "UNKNOWN";
                long count = row[1] != null ? ((Number) row[1]).longValue() : 0L;

                if (counts.containsKey(status)) {
                    counts.put(status, count);
                } else {
                    counts.put(status, count);
                }
            }
        } catch (Exception e) {
            // leave defaults
        }

        List<StatusPoint> result = new ArrayList<>();
        for (Map.Entry<String, Long> e : counts.entrySet()) {
            result.add(StatusPoint.builder().status(e.getKey()).value(e.getValue()).build());
        }
        return result;
    }
}