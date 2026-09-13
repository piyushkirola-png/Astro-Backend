package com.astrologytalk.config;

import com.astrologytalk.entity.KundaliChart;
import com.astrologytalk.repository.KundaliChartRepository;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@Order(3)
@RequiredArgsConstructor
public class KundaliChartCsvLoader implements CommandLineRunner {

  private final KundaliChartRepository repository;

  @Override
  @Transactional
  public void run(String... args) throws Exception {
    long existing = repository.count();
    if (existing > 0) {
      log.info("📚 Kundali chart already loaded ({} rows) — skipping CSV import", existing);
      return;
    }

    log.info("📚 Kundali chart empty — importing from CSV...");

    ClassPathResource csv = new ClassPathResource("seed/kundali_chart.csv");
    if (!csv.exists()) {
      log.warn("⚠️  seed/kundali_chart.csv not found — skipping import");
      return;
    }

    List<KundaliChart> rows = new ArrayList<>();

    try (BufferedReader br =
        new BufferedReader(new InputStreamReader(csv.getInputStream(), StandardCharsets.UTF_8))) {

      String line = br.readLine(); // header
      int lineNum = 1;

      while ((line = br.readLine()) != null) {
        lineNum++;
        if (line.isBlank()) continue;

        String[] p = parseCsvLine(line);
        if (p.length < 14) {
          log.warn("⚠️  Skipping malformed line {} ({} cols)", lineNum, p.length);
          continue;
        }

        KundaliChart c =
            KundaliChart.builder()
                .zodiac(p[0].trim())
                .chartType(p[1].trim())
                .house1(blankToNull(p[2]))
                .house2(blankToNull(p[3]))
                .house3(blankToNull(p[4]))
                .house4(blankToNull(p[5]))
                .house5(blankToNull(p[6]))
                .house6(blankToNull(p[7]))
                .house7(blankToNull(p[8]))
                .house8(blankToNull(p[9]))
                .house9(blankToNull(p[10]))
                .house10(blankToNull(p[11]))
                .house11(blankToNull(p[12]))
                .house12(blankToNull(p[13]))
                .build();

        rows.add(c);
      }
    }

    repository.saveAll(rows);
    log.info("✅ Imported {} rows into kundali_chart", rows.size());
  }

  private String blankToNull(String s) {
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
