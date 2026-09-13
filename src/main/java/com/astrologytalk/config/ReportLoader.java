package com.astrologytalk.config;

import com.astrologytalk.entity.ReportEntry;
import com.astrologytalk.repository.ReportEntryRepository;
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
public class ReportLoader implements CommandLineRunner {

  private final ReportEntryRepository repository;

  private static final String[] FILES = {
    "seed/report_general.csv",
    "seed/report_remedies.csv",
    "seed/report_dosha.csv",
    "seed/report_gemstone.csv",
  };

  @Override
  @Transactional
  public void run(String... args) throws Exception {
    long existing = repository.count();
    if (existing > 0) {
      log.info("📖 Report entries already loaded ({} rows) — skipping CSV import", existing);
      return;
    }

    log.info("📖 Report entries empty — importing from CSVs...");

    List<ReportEntry> all = new ArrayList<>();

    for (String file : FILES) {
      int before = all.size();
      int imported = loadCsv(file, all);
      log.info("  → {} imported {} rows", file, imported - 0 == imported ? imported : 0);
      int added = all.size() - before;
      log.info("  ✓ {} → {} new rows ({} total)", file, added, all.size());
    }

    if (all.isEmpty()) {
      log.warn("⚠️  No report entries loaded — check CSV files exist in resources/seed/");
      return;
    }

    repository.saveAll(all);
    log.info("✅ Imported {} total report entries", all.size());
  }

  private int loadCsv(String path, List<ReportEntry> sink) {
    ClassPathResource csv = new ClassPathResource(path);
    if (!csv.exists()) {
      log.warn("  ⚠️  {} not found — skipping", path);
      return 0;
    }

    int lineNum = 0;

    try (BufferedReader br =
        new BufferedReader(new InputStreamReader(csv.getInputStream(), StandardCharsets.UTF_8))) {

      String line = br.readLine(); // header
      if (line == null) return 0;

      while ((line = br.readLine()) != null) {
        lineNum++;
        if (line.isBlank()) continue;

        String[] parts = parseCsvLine(line);
        if (parts.length < 9) {
          log.warn("  ⚠️  Skipping malformed line {} in {}: {}", lineNum, path, line);
          continue;
        }

        ReportEntry entry = new ReportEntry();
        entry.setType(trim(parts[0]));
        entry.setZodiac(trim(parts[1]));
        entry.setCategory(trim(parts[2]));
        entry.setTitle(trim(parts[3]));
        entry.setContent(trim(parts[4]));
        entry.setField1(trim(parts[5]));
        entry.setField2(trim(parts[6]));
        entry.setField3(trim(parts[7]));
        entry.setField4(trim(parts[8]));
        entry.setIsActive(true);

        if (entry.getType() == null || entry.getType().isBlank()) {
          continue;
        }

        sink.add(entry);
      }
    } catch (Exception e) {
      log.error("  ❌ Failed to parse {}: {}", path, e.getMessage(), e);
    }

    return lineNum;
  }

  private String trim(String s) {
    if (s == null) return null;
    String t = s.trim();
    return t.isEmpty() ? null : t;
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
