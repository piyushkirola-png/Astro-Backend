package com.astrologytalk.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

  private Long id;
  private String categoryCode;
  private Long packageId;
  private BigDecimal amount;
  private String currency;
  private String status;
  private String gateway;
  private String gatewayOrderId;
  private String gatewayPaymentId;
  private String paymentLink;
  private String utr;
  private Integer secondsCredited;
  private String notes;
  private LocalDateTime completedAt;
  private LocalDateTime createdAt;
}
