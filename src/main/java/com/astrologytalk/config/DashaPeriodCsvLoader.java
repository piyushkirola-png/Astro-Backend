package com.astrologytalk.config;

import com.astrologytalk.entity.DashaPeriod;
import com.astrologytalk.repository.DashaPeriodRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@Order(5)
@RequiredArgsConstructor
public class DashaPeriodCsvLoader implements CommandLineRunner {

    private final DashaPeriodRepository repository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        long existing = repository.count();
        if (existing > 0) {
            log.info("📚 Dasha periods already loaded ({} rows) — skipping CSV import", existing);
            return;
        }

        log.info("📚 Dasha periods empty — importing from CSV...");

        ClassPathResource csv = new ClassPathResource("seed/dasha_periods.csv");
        if (!csv.exists()) {
            log.warn("⚠️  seed/dasha_periods.csv not found — skipping import");
            return;
        }

        List<DashaPeriod> rows = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(csv.getInputStream(), StandardCharsets.UTF_8))) {

            String line = br.readLine();
            int lineNum = 1;

            while ((line = br.readLine()) != null) {
                lineNum++;
                if (line.isBlank()) continue;

                String[] p = parseCsvLine(line);
                if (p.length < 8) {
                    log.warn("⚠️  Skipping malformed line {} ({} cols)", lineNum, p.length);
                    continue;
                }

                Integer house = null;
                try { house = Integer.parseInt(p[4].trim()); } catch (Exception ignored) {}

                DashaPeriod row = DashaPeriod.builder()
                        .zodiac(p[0].trim())
                        .planet(p[1].trim())
                        .startOffsetYears(Integer.parseInt(p[2].trim()))
                        .endOffsetYears(Integer.parseInt(p[3].trim()))
                        .house(house)
                        .sign(p[5].trim())
                        .paragraph1(p[6].trim())
                        .paragraph2(p[7].trim())
                        .build();

                rows.add(row);
            }
        }

        repository.saveAll(rows);
        log.info("✅ Imported {} rows into dasha_periods", rows.size());
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