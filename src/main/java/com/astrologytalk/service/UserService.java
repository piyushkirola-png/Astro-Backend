package com.astrologytalk.service;

import com.astrologytalk.dto.request.UpdateProfileRequest;
import com.astrologytalk.dto.response.UserResponse;
import java.util.List;

public interface UserService {
  UserResponse getProfile(Long userId);

  UserResponse updateProfile(Long userId, UpdateProfileRequest request);

  List<UserResponse> getAllUsers();

  void deleteUser(Long userId);

  UserResponse getUserById(Long userId);

  UserResponse updateAvatar(Long userId, String avatarUrl);

  UserResponse setActive(Long userId, boolean active);
}
