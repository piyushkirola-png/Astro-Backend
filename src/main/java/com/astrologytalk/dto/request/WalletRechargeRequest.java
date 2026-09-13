package com.astrologytalk.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletRechargeRequest {

  @NotNull(message = "Package ID is required")
  private Long packageId;

  @NotNull(message = "Gateway is required")
  private String gateway;
}
