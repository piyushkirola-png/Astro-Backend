package com.astrologytalk.config;

import com.astrologytalk.entity.HoroscopeEntry;
import com.astrologytalk.repository.HoroscopeEntryRepository;
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
public class HoroscopeCsvLoader implements CommandLineRunner {

  private final HoroscopeEntryRepository repository;

  private static final String CSV_PATH = "seed/horoscope_entries.csv";

  @Override
  @Transactional
  public void run(String... args) {
    long existing = repository.count();
    if (existing > 0) {
      log.info("📖 Horoscope entries already loaded ({} rows) — skipping CSV import", existing);
      return;
    }

    ClassPathResource csv = new ClassPathResource(CSV_PATH);
    if (!csv.exists()) {
      log.warn("⚠️  {} not found — skipping horoscope import", CSV_PATH);
      return;
    }

    log.info("📖 Horoscope entries empty — importing from CSV...");

    List<HoroscopeEntry> entries = new ArrayList<>();
    int lineNum = 1;

    try (BufferedReader br =
        new BufferedReader(new InputStreamReader(csv.getInputStream(), StandardCharsets.UTF_8))) {

      String line = br.readLine();

      while ((line = br.readLine()) != null) {
        lineNum++;
        if (line.isBlank()) continue;

        String[] parts = parseCsvLine(line);
        if (parts.length < 7) {
          log.warn("  ⚠️  Skipping malformed line {}: {}", lineNum, line);
          continue;
        }

        HoroscopeEntry e = new HoroscopeEntry();
        e.setZodiac(trim(parts[0]).toUpperCase());
        e.setPeriod(trim(parts[1]).toUpperCase());
        e.setVariant(Integer.parseInt(trim(parts[2])));
        e.setLoveText(trim(parts[3]));
        e.setCareerText(trim(parts[4]));
        e.setHealthText(trim(parts[5]));
        e.setMoneyText(trim(parts[6]));

        if (parts.length > 7 && !trim(parts[7]).isEmpty()) {
          try {
            e.setLuckyNumber(Integer.parseInt(trim(parts[7])));
          } catch (NumberFormatException ignore) {
          }
        }
        if (parts.length > 8 && !trim(parts[8]).isEmpty()) {
          e.setLuckyColor(trim(parts[8]));
        }

        e.setIsActive(true);
        entries.add(e);
      }

    } catch (Exception e) {
      log.error("❌ Failed to parse {}: {}", CSV_PATH, e.getMessage(), e);
      return;
    }

    if (entries.isEmpty()) {
      log.warn("⚠️  No horoscope entries parsed");
      return;
    }

    repository.saveAll(entries);
    log.info("✅ Imported {} horoscope entries", entries.size());
  }

  private String trim(String s) {
    return s == null ? "" : s.trim();
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
