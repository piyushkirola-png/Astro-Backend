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
public class AdminStatsResponse {

  private long totalUsers;
  private long totalRevenue;
  private long totalMessages;
  private long totalPayments;

  private List<DayPoint> revenueByDay;
  private List<DayPoint> aiUsageByDay;
  private List<DurationPoint> revenueByDuration;
  private List<StatusPoint> statusDistribution;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class DayPoint {
    private String date;
    private long value;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class DurationPoint {
    private String label;
    private long value;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class StatusPoint {
    private String status;
    private long value;
  }
}
