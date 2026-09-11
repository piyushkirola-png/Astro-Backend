package com.astrologytalk.service;

import com.astrologytalk.dto.request.UpdateProfileRequest;
import com.astrologytalk.dto.response.UserResponse;
import com.astrologytalk.entity.Gender;
import com.astrologytalk.entity.User;
import com.astrologytalk.repository.UserRepository;
import com.astrologytalk.utils.ZodiacUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PlacesService placesService;

    private static final DateTimeFormatter DOB_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter TOB_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    public UserResponse getProfile(Long userId) {
        return mapToUserResponse(findUser(userId));
    }

    @Override
    public UserResponse getUserById(Long userId) {
        return mapToUserResponse(findUser(userId));
    }

    @Override
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest req) {
        User user = findUser(userId);

        if (req.getName() != null && !req.getName().isBlank()) {
            user.setName(req.getName().trim());
        }

        if (req.getEmail() != null && !req.getEmail().isBlank()
                && !req.getEmail().equalsIgnoreCase(user.getEmail())) {
            if (userRepository.existsByEmail(req.getEmail())) {
                throw new RuntimeException("Email already in use");
            }
            user.setEmail(req.getEmail().trim());
        }

        if (req.getGender() != null && !req.getGender().isBlank()) {
            user.setGender(Gender.valueOf(req.getGender()));
        }

        if (req.getDateOfBirth() != null && !req.getDateOfBirth().isBlank()) {
            try {
                LocalDate dob = LocalDate.parse(req.getDateOfBirth(), DOB_FORMAT);
                if (dob.isAfter(LocalDate.now())) {
                    throw new RuntimeException("Date of birth cannot be in the future");
                }
                user.setDateOfBirth(dob);
                user.setZodiacSign(ZodiacUtils.getZodiacSign(dob));
            } catch (DateTimeParseException e) {
                throw new RuntimeException("dateOfBirth must be dd-MM-yyyy");
            }
        }

        if (req.getTimeOfBirth() != null && !req.getTimeOfBirth().isBlank()) {
            try {
                user.setTimeOfBirth(LocalTime.parse(req.getTimeOfBirth(), TOB_FORMAT));
            } catch (DateTimeParseException e) {
                throw new RuntimeException("timeOfBirth must be HH:mm");
            }
        }

        if (req.getPlaceOfBirth() != null && !req.getPlaceOfBirth().isBlank()) {
            String newPlace = req.getPlaceOfBirth().trim();
            String oldPlace = user.getPlaceOfBirth();

            boolean placeChanged = oldPlace == null || !oldPlace.equalsIgnoreCase(newPlace);

            user.setPlaceOfBirth(newPlace);

            if (placeChanged) {
                log.info("Place changed from '{}' to '{}' — fetching coordinates",
                        oldPlace, newPlace);
                PlacesService.GeocodeResult coords = placesService.geocode(newPlace);

                if (coords != null) {
                    user.setBirthLat(coords.lat());
                    user.setBirthLng(coords.lng());
                    user.setBirthTimezone("GMT +05:30"); // default for India
                    log.info("Saved coordinates: lat={} lng={}", coords.lat(), coords.lng());
                } else {
                    log.warn("Could not resolve coordinates for '{}' — leaving old values", newPlace);
                }
            }
        }

        if (req.getCurrentAddress() != null) user.setCurrentAddress(blankToNull(req.getCurrentAddress()));
        if (req.getCity() != null)           user.setCity(blankToNull(req.getCity()));
        if (req.getState() != null)          user.setState(blankToNull(req.getState()));
        if (req.getCountry() != null)        user.setCountry(blankToNull(req.getCountry()));
        if (req.getPincode() != null)        user.setPincode(blankToNull(req.getPincode()));
        if (req.getPhone() != null)          user.setPhone(blankToNull(req.getPhone()));

        User updated = userRepository.save(user);
        return mapToUserResponse(updated);
    }

    @Override
    @Transactional
    public UserResponse updateAvatar(Long userId, String avatarUrl) {
        User user = findUser(userId);
        user.setAvatarUrl(avatarUrl);
        return mapToUserResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        User user = findUser(userId);
        if (Boolean.TRUE.equals(user.getIsActive())) {
            throw new RuntimeException("Cannot delete an active user. Deactivate first.");
        }
        userRepository.delete(user);
    }

    @Override
    @Transactional
    public UserResponse setActive(Long userId, boolean active) {
        User user = findUser(userId);
        user.setIsActive(active);
        return mapToUserResponse(userRepository.save(user));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private String blankToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .gender(user.getGender() != null ? user.getGender().name() : null)
                .phone(user.getPhone())
                .dateOfBirth(user.getDateOfBirth())
                .timeOfBirth(user.getTimeOfBirth())
                .placeOfBirth(user.getPlaceOfBirth())
                .birthLat(user.getBirthLat())
                .birthLng(user.getBirthLng())
                .birthTimezone(user.getBirthTimezone())
                .currentAddress(user.getCurrentAddress())
                .city(user.getCity())
                .state(user.getState())
                .country(user.getCountry())
                .pincode(user.getPincode())
                .avatarUrl(user.getAvatarUrl())
                .zodiacSign(user.getZodiacSign())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .build();
    }
}