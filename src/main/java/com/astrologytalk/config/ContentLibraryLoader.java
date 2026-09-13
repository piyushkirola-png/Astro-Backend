package com.astrologytalk.config;

import com.astrologytalk.entity.ContentEntry;
import com.astrologytalk.repository.ContentLibraryRepository;
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
public class ContentLibraryLoader implements CommandLineRunner {

  private final ContentLibraryRepository repository;

  @Override
  @Transactional
  public void run(String... args) throws Exception {
    long existing = repository.count();
    if (existing > 0) {
      log.info("📚 Content library already loaded ({} rows) — skipping CSV import", existing);
      return;
    }

    log.info("📚 Content library empty — importing from CSV...");

    ClassPathResource csv = new ClassPathResource("seed/content_library.csv");
    if (!csv.exists()) {
      log.warn("⚠️  seed/content_library.csv not found — skipping import");
      return;
    }

    List<ContentEntry> entries = new ArrayList<>();

    try (BufferedReader br =
        new BufferedReader(new InputStreamReader(csv.getInputStream(), StandardCharsets.UTF_8))) {

      String line = br.readLine(); // header
      int lineNum = 1;

      while ((line = br.readLine()) != null) {
        lineNum++;
        if (line.isBlank()) continue;

        // Simple CSV parser — handles quoted fields
        String[] parts = parseCsvLine(line);
        if (parts.length < 6) {
          log.warn("⚠️  Skipping malformed line {}: {}", lineNum, line);
          continue;
        }

        ContentEntry entry =
            ContentEntry.builder()
                .signature(parts[0].trim())
                .category(parts[1].trim())
                .variant(Integer.parseInt(parts[2].trim()))
                .language(parts[3].trim())
                .tone(parts[4].trim().isEmpty() ? "NEUTRAL" : parts[4].trim())
                .text(parts[5].trim())
                .build();

        entries.add(entry);
      }
    }

    repository.saveAll(entries);
    log.info("✅ Imported {} rows into content_library", entries.size());
  }

  // Minimal CSV parser that respects quoted fields with commas
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
