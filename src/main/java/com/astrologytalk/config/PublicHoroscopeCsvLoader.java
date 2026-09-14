package com.astrologytalk.config;

import com.astrologytalk.entity.PublicHoroscopeEntry;
import com.astrologytalk.repository.PublicHoroscopeEntryRepository;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PublicHoroscopeCsvLoader implements CommandLineRunner {

  private final PublicHoroscopeEntryRepository repository;

  private static final String CSV_PATH = "seed/public_horoscope_entries.csv";

  @Override
  @Transactional
  public void run(String... args) {
    long existing = repository.count();
    if (existing > 0) {
      log.info("📖 Public horoscope entries already loaded ({} rows) — skipping CSV import", existing);
      return;
    }

    ClassPathResource csv = new ClassPathResource(CSV_PATH);
    if (!csv.exists()) {
      log.warn("⚠️  {} not found — skipping public horoscope import", CSV_PATH);
      return;
    }

    log.info("📖 Public horoscope entries empty — importing from CSV...");

    List<PublicHoroscopeEntry> entries = new ArrayList<>();
    int lineNum = 1;

    try (BufferedReader br =
        new BufferedReader(new InputStreamReader(csv.getInputStream(), StandardCharsets.UTF_8))) {

      String line = br.readLine(); // skip header

      while ((line = br.readLine()) != null) {
        lineNum++;
        if (line.isBlank()) continue;

        String[] parts = parseCsvLine(line);
        if (parts.length < 22) {
          log.warn("  ⚠️  Skipping malformed line {} ({} fields, need 22): {}",
              lineNum, parts.length, line);
          continue;
        }

        PublicHoroscopeEntry e = new PublicHoroscopeEntry();
        e.setZodiac(trim(parts[0]).toUpperCase());
        e.setSanskritName(trim(parts[1]));
        e.setSymbol(trim(parts[2]));
        e.setSymbolEmoji(trim(parts[3]));
        e.setRulingPlanet(trim(parts[4]));
        e.setTarotCard(trim(parts[5]));
        e.setLuckyStone(trim(parts[6]));
        e.setDateRange(trim(parts[7]));
        e.setPeriod(trim(parts[8]).toUpperCase());
        e.setMainText(trim(parts[9]));
        e.setLuckyNumber(parseIntSafe(parts[10]));
        e.setLuckyColor(trim(parts[11]));
        e.setAuspiciousTime(trim(parts[12]));
        e.setMood(trim(parts[13]));
        e.setLoveScore(parseIntSafe(parts[14]));
        e.setLoveText(trim(parts[15]));
        e.setFinanceScore(parseIntSafe(parts[16]));
        e.setFinanceText(trim(parts[17]));
        e.setCareerScore(parseIntSafe(parts[18]));
        e.setCareerText(trim(parts[19]));
        e.setHealthScore(parseIntSafe(parts[20]));
        e.setHealthText(trim(parts[21]));

        e.setIsActive(true);
        entries.add(e);
      }

    } catch (Exception e) {
      log.error("❌ Failed to parse {}: {}", CSV_PATH, e.getMessage(), e);
      return;
    }

    if (entries.isEmpty()) {
      log.warn("⚠️  No public horoscope entries parsed");
      return;
    }

    repository.saveAll(entries);
    log.info("✅ Imported {} public horoscope entries", entries.size());
  }

  private String trim(String s) {
    return s == null ? "" : s.trim();
  }

  private Integer parseIntSafe(String s) {
    try {
      return Integer.parseInt(trim(s));
    } catch (Exception e) {
      return 0;
    }
  }

  private String[] parseCsvLine(String line) {
    List<String> fields = new ArrayList<>();
    StringBuilder sb = new StringBuilder();
    boolean inQuotes = false;

    for (int i = 0; i < line.length(); i++) {
      char c = line.charAt(i);

      if (c == '"') {
        if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
          sb.append('"');
          i++;
        } else {
          inQuotes = !inQuotes;
        }
      } else if (c == ',' && !inQuotes) {
        fields.add(sb.toString());
        sb.setLength(0);
      } else {
        sb.append(c);
      }
    }
    fields.add(sb.toString());

    return fields.toArray(new String[0]);
  }
}