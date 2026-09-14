package com.astrologytalk.service;

import com.astrologytalk.dto.response.AdminStatsResponse;

public interface AdminService {
  AdminStatsResponse getStats();

  void resetUserPassword(Long userId, String newPassword);
}
