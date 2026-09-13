package com.astrologytalk.service;

import com.astrologytalk.common.exception.ResourceNotFoundException;
import com.astrologytalk.entity.HoroscopeEntry;
import com.astrologytalk.entity.User;
import com.astrologytalk.repository.HoroscopeEntryRepository;
import com.astrologytalk.repository.UserRepository;
import com.astrologytalk.utils.ZodiacUtils;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserHoroscopeService {

  private final HoroscopeEntryRepository entryRepo;
  private final UserRepository userRepo;

  private static final int VARIANTS = 30;

  public Map<String, Object> getForUser(Long userId, String period) {
    User user =
        userRepo
            .findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

    String zodiac = resolveZodiac(user);
    if (zodiac == null) {
      throw new RuntimeException(
          "Please complete your profile (date of birth) to see your horoscope");
    }

    String normalizedPeriod = period == null ? "TODAY" : period.toUpperCase();
    int variant = computeVariant(normalizedPeriod);

    Optional<HoroscopeEntry> entry =
        entryRepo.findByZodiacAndPeriodAndVariantAndIsActiveTrue(zodiac, normalizedPeriod, variant);

    if (entry.isEmpty()) {
      List<HoroscopeEntry> pool =
          entryRepo.findByZodiacAndPeriodAndIsActiveTrueOrderByVariantAsc(zodiac, normalizedPeriod);
      if (pool.isEmpty()) {
        throw new ResourceNotFoundException(
            "No horoscope content available for " + zodiac + " " + normalizedPeriod);
      }
      int fallbackIdx = Math.abs(variant) % pool.size();
      entry = Optional.of(pool.get(fallbackIdx));
    }

    HoroscopeEntry e = entry.get();

    Map<String, Object> result = new HashMap<>();
    result.put("zodiac", zodiac);
    result.put("period", normalizedPeriod);
    result.put("variant", e.getVariant());
    result.put("loveText", e.getLoveText());
    result.put("careerText", e.getCareerText());
    result.put("healthText", e.getHealthText());
    result.put("moneyText", e.getMoneyText());
    result.put("luckyNumber", e.getLuckyNumber());
    result.put("luckyColor", e.getLuckyColor());
    result.put("date", LocalDate.now().toString());

    return result;
  }

  private String resolveZodiac(User user) {
    if (user.getZodiacSign() != null && !user.getZodiacSign().isBlank()) {
      return user.getZodiacSign().toUpperCase();
    }
    if (user.getDateOfBirth() != null) {
      String derived = ZodiacUtils.getZodiacSign(user.getDateOfBirth());
      if (derived != null && !derived.isBlank()) {
        user.setZodiacSign(derived);
        userRepo.save(user);
        return derived.toUpperCase();
      }
    }
    return null;
  }

  private int computeVariant(String period) {
    LocalDate now = LocalDate.now();

    return switch (period) {
      case "TODAY" -> now.getDayOfYear() % VARIANTS;
      case "YESTERDAY" -> ((now.getDayOfYear() - 1) + 366) % VARIANTS;
      case "TOMORROW" -> (now.getDayOfYear() + 1) % VARIANTS;
      case "WEEKLY" -> {
        WeekFields wf = WeekFields.of(Locale.ENGLISH);
        int week = now.get(wf.weekOfWeekBasedYear());
        yield week % VARIANTS;
      }
      case "MONTHLY" -> now.getMonthValue() % VARIANTS;
      case "YEARLY" -> now.getYear() % VARIANTS;
      default -> now.getDayOfYear() % VARIANTS;
    };
  }
}
