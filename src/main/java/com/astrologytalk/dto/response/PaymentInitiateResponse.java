package com.astrologytalk.dto.response;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentInitiateResponse {

  private Long paymentId;
  private String gatewayOrderId;
  private String paymentLink;
  private String gateway;
  private BigDecimal amount;
  private String status;
}
