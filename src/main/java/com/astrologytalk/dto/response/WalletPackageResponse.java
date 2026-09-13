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
public class WalletPackageResponse {

  private Long id;
  private BigDecimal amount;
  private Integer secondsCredited;
  private String label;
  private Integer displayOrder;
  private Boolean isActive;
}
