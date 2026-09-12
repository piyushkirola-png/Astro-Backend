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
public class PaymentCategoryResponse {

    private Long id;
    private String code;
    private String name;
    private BigDecimal amount;
    private String description;
    private Boolean isActive;
}