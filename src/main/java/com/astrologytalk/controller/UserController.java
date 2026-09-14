package com.astrologytalk.controller;

import com.astrologytalk.common.response.ApiResponse;
import com.astrologytalk.dto.request.ChangePasswordRequest;
import com.astrologytalk.dto.request.ResetUserPasswordRequest;
import com.astrologytalk.dto.request.UpdateProfileRequest;
import com.astrologytalk.dto.response.UserResponse;
import com.astrologytalk.entity.User;
import com.astrologytalk.service.AdminService;
import com.astrologytalk.service.AvatarStorageService;
import com.astrologytalk.service.PasswordService;
import com.astrologytalk.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;
  private final PasswordService passwordService;
  private final AvatarStorageService avatarStorageService;
  private final AdminService adminService;

  // ----- GET /api/users/me -----
  @GetMapping("/me")
  public ResponseEntity<ApiResponse<UserResponse>> getMe(@AuthenticationPrincipal User user) {
    return ResponseEntity.ok(
        ApiResponse.success("Profile fetched", userService.getUserById(user.getId())));
  }

  // ----- PUT /api/users/me -----
  @PutMapping("/me")
  public ResponseEntity<ApiResponse<UserResponse>> updateMe(
      @AuthenticationPrincipal User user, @Valid @RequestBody UpdateProfileRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success("Profile updated", userService.updateProfile(user.getId(), request)));
  }

  // ----- POST /api/users/me/avatar -----
  @PostMapping("/me/avatar")
  public ResponseEntity<ApiResponse<UserResponse>> uploadMyAvatar(
      @AuthenticationPrincipal User user, @RequestParam("file") MultipartFile file) {
    String url = avatarStorageService.store(file, user.getId());
    return ResponseEntity.ok(
        ApiResponse.success("Avatar updated", userService.updateAvatar(user.getId(), url)));
  }

  // ----- POST /api/users/me/password -----
  @PostMapping("/me/password")
  public ResponseEntity<ApiResponse<Void>> changePassword(
      @AuthenticationPrincipal User user, @Valid @RequestBody ChangePasswordRequest request) {

    passwordService.changePassword(
        user.getId(), request.getCurrentPassword(), request.getNewPassword());

    return ResponseEntity.ok(ApiResponse.success("Password changed successfully", null));
  }

  // ----- Admin: GET /api/users -----
  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
    return ResponseEntity.ok(ApiResponse.success("Users retrieved", userService.getAllUsers()));
  }

  // ----- Admin: GET /api/users/{id} -----
  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
    return ResponseEntity.ok(ApiResponse.success("User found", userService.getUserById(id)));
  }

  // ----- Admin: PUT /api/users/{id} -----
  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<UserResponse>> updateUser(
      @PathVariable Long id, @Valid @RequestBody UpdateProfileRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success("User updated", userService.updateProfile(id, request)));
  }

  // ----- Admin: PATCH /api/users/{id}/status -----
  @PatchMapping("/{id}/status")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<UserResponse>> setUserStatus(
      @PathVariable Long id, @RequestBody Map<String, Boolean> body) {
    boolean active = Boolean.TRUE.equals(body.get("active"));
    return ResponseEntity.ok(
        ApiResponse.success(
            active ? "User activated" : "User deactivated", userService.setActive(id, active)));
  }

  // ----- Admin: POST /api/users/{id}/reset-password -----
  @PostMapping("/{id}/reset-password")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<Void>> resetUserPassword(
      @PathVariable Long id, @Valid @RequestBody ResetUserPasswordRequest request) {
    adminService.resetUserPassword(id, request.getNewPassword());
    return ResponseEntity.ok(ApiResponse.success("Password reset successfully", null));
  }

  // ----- Admin: DELETE /api/users/{id} -----
  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
    userService.deleteUser(id);
    return ResponseEntity.ok(ApiResponse.success("User deleted", null));
  }
}
