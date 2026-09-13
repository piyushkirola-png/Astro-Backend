package com.astrologytalk.service;

import com.astrologytalk.dto.request.LoginRequest;
import com.astrologytalk.dto.request.RegisterRequest;
import com.astrologytalk.dto.response.AuthResponse;

public interface AuthService {
  AuthResponse register(RegisterRequest request);

  AuthResponse login(LoginRequest request);

  void logout(String token);
}
