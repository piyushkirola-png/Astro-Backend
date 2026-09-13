package com.astrologytalk.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {

  @NotBlank(message = "Name is required")
  @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
  private String name;

  @Email(message = "Invalid email format")
  private String email;

  @Pattern(regexp = "^$|^[+]?[0-9]{10,15}$", message = "Phone must be 10–15 digits")
  private String phone;

  @Pattern(regexp = "^(MALE|FEMALE)?$", message = "Gender must be MALE or FEMALE")
  private String gender;

  @Pattern(
      regexp = "^$|^\\d{2}-\\d{2}-\\d{4}$",
      message = "dateOfBirth must be in dd-MM-yyyy format")
  private String dateOfBirth;

  @Pattern(
      regexp = "^$|^([01]\\d|2[0-3]):[0-5]\\d$",
      message = "timeOfBirth must be in HH:mm format")
  private String timeOfBirth;

  @Size(max = 255)
  private String placeOfBirth;

  private Double birthLat;
  private Double birthLng;

  @Size(max = 20)
  private String birthTimezone;

  @Size(max = 255)
  private String currentAddress;

  @Size(max = 100)
  private String city;

  @Size(max = 100)
  private String state;

  @Size(max = 100)
  private String country;

  @Pattern(regexp = "^$|^\\d{4,10}$", message = "Pincode must be 4-10 digits")
  private String pincode;
}
