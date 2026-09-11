package com.astrologytalk.config;

import com.astrologytalk.entity.PlanetaryPosition;
import com.astrologytalk.repository.PlanetaryPositionRepository;
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
@Order(4)
@RequiredArgsConstructor
public class PlanetaryPositionCsvLoader implements CommandLineRunner {

    private final PlanetaryPositionRepository repository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        long existing = repository.count();
        if (existing > 0) {
            log.info("📚 Planetary positions already loaded ({} rows) — skipping CSV import", existing);
            return;
        }

        log.info("📚 Planetary positions empty — importing from CSV...");

        ClassPathResource csv = new ClassPathResource("seed/planetary_positions.csv");
        if (!csv.exists()) {
            log.warn("⚠️  seed/planetary_positions.csv not found — skipping import");
            return;
        }

        List<PlanetaryPosition> rows = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(csv.getInputStream(), StandardCharsets.UTF_8))) {

            String line = br.readLine();
            int lineNum = 1;

            while ((line = br.readLine()) != null) {
                lineNum++;
                if (line.isBlank()) continue;

                String[] p = parseCsvLine(line);
                if (p.length < 11) {
                    log.warn("⚠️  Skipping malformed line {} ({} cols)", lineNum, p.length);
                    continue;
                }

                Integer houseVal = null;
                try {
                    houseVal = Integer.parseInt(p[8].trim());
                } catch (Exception e) {
                    houseVal = null;
                }

                PlanetaryPosition row = PlanetaryPosition.builder()
                        .zodiac(p[0].trim())
                        .planet(p[1].trim())
                        .sign(p[2].trim())
                        .signLord(p[3].trim())
                        .nakshatra(p[4].trim())
                        .nakshatraLord(p[5].trim())
                        .degree(p[6].trim())
                        .retro(p[7].trim())
                        .house(houseVal)
                        .state(p[9].trim())
                        .status(p[10].trim())
                        .build();

                rows.add(row);
            }
        }

        repository.saveAll(rows);
        log.info("✅ Imported {} rows into planetary_positions", rows.size());
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