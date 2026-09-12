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
public class WalletPackageResponse {

    private Long id;
    private BigDecimal amount;
    private Integer secondsCredited;
    private String label;
    private Integer displayOrder;
    private Boolean isActive;
}