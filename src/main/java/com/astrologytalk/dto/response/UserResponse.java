package com.astrologytalk.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String name;
    private String email;
    private String role;

    private String gender;
    private String phone;

    private LocalDate dateOfBirth;
    private LocalTime timeOfBirth;

    private String placeOfBirth;
    private String currentAddress;
    private String city;
    private String state;
    private String country;
    private String pincode;

    private String avatarUrl;

    private String zodiacSign;
    private Boolean isActive;

    private LocalDateTime createdAt;
}