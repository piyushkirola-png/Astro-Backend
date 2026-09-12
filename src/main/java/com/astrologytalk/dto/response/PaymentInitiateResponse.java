package com.astrologytalk.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

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