package com.astrologytalk.service;

import com.astrologytalk.common.exception.ResourceNotFoundException;
import com.astrologytalk.entity.PublicHoroscopeEntry;
import com.astrologytalk.repository.PublicHoroscopeEntryRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PublicHoroscopeService {

  private final PublicHoroscopeEntryRepository repo;

  public List<Map<String, Object>> getHubList(String period) {
    String normalizedPeriod = normalizePeriod(period);

    List<PublicHoroscopeEntry> entries =
        repo.findByPeriodAndIsActiveTrueOrderByZodiacAsc(normalizedPeriod);

    if (entries.isEmpty()) {
      throw new ResourceNotFoundException(
          "No horoscope content available for period: " + normalizedPeriod);
    }

    List<Map<String, Object>> result = new ArrayList<>();
    for (PublicHoroscopeEntry e : entries) {
      result.add(toHubMap(e));
    }
    return result;
  }

  public Map<String, Object> getDetail(String period, String zodiac) {
    String normalizedPeriod = normalizePeriod(period);
    String normalizedZodiac = normalizeZodiac(zodiac);

    Optional<PublicHoroscopeEntry> entry =
        repo.findByZodiacAndPeriodAndIsActiveTrue(normalizedZodiac, normalizedPeriod);

    if (entry.isEmpty()) {
      throw new ResourceNotFoundException(
          "Horoscope not found for " + normalizedZodiac + " " + normalizedPeriod);
    }

    return toDetailMap(entry.get());
  }

  private Map<String, Object> toHubMap(PublicHoroscopeEntry e) {
    Map<String, Object> m = new HashMap<>();
    m.put("zodiac", e.getZodiac());
    m.put("sanskritName", e.getSanskritName());
    m.put("symbol", e.getSymbol());
    m.put("symbolEmoji", e.getSymbolEmoji());
    m.put("dateRange", e.getDateRange());
    return m;
  }

  private Map<String, Object> toDetailMap(PublicHoroscopeEntry e) {
    Map<String, Object> m = new HashMap<>();
    m.put("zodiac", e.getZodiac());
    m.put("sanskritName", e.getSanskritName());
    m.put("symbol", e.getSymbol());
    m.put("symbolEmoji", e.getSymbolEmoji());
    m.put("rulingPlanet", e.getRulingPlanet());
    m.put("tarotCard", e.getTarotCard());
    m.put("luckyStone", e.getLuckyStone());
    m.put("dateRange", e.getDateRange());
    m.put("period", e.getPeriod());
    m.put("mainText", e.getMainText());
    m.put("luckyNumber", e.getLuckyNumber());
    m.put("luckyColor", e.getLuckyColor());
    m.put("auspiciousTime", e.getAuspiciousTime());
    m.put("mood", e.getMood());
    m.put("loveScore", e.getLoveScore());
    m.put("loveText", e.getLoveText());
    m.put("financeScore", e.getFinanceScore());
    m.put("financeText", e.getFinanceText());
    m.put("careerScore", e.getCareerScore());
    m.put("careerText", e.getCareerText());
    m.put("healthScore", e.getHealthScore());
    m.put("healthText", e.getHealthText());
    return m;
  }

  private String normalizePeriod(String period) {
    if (period == null || period.isBlank()) return "TODAY";
    return period.trim().toUpperCase();
  }

  private String normalizeZodiac(String zodiac) {
    if (zodiac == null || zodiac.isBlank()) {
      throw new ResourceNotFoundException("Zodiac is required");
    }
    return zodiac.trim().toUpperCase();
  }
}