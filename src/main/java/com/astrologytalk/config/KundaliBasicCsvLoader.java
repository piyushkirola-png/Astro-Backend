package com.astrologytalk.config;

import com.astrologytalk.entity.KundaliBasic;
import com.astrologytalk.repository.KundaliBasicRepository;
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
@Order(2)
@RequiredArgsConstructor
public class KundaliBasicCsvLoader implements CommandLineRunner {

    private final KundaliBasicRepository repository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        long existing = repository.count();
        if (existing > 0) {
            log.info("📚 Kundali basic already loaded ({} rows) — skipping CSV import", existing);
            return;
        }

        log.info("📚 Kundali basic empty — importing from CSV...");

        ClassPathResource csv = new ClassPathResource("seed/kundali_basic.csv");
        if (!csv.exists()) {
            log.warn("⚠️  seed/kundali_basic.csv not found — skipping import");
            return;
        }

        List<KundaliBasic> rows = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(csv.getInputStream(), StandardCharsets.UTF_8))) {

            String line = br.readLine(); // header
            int lineNum = 1;

            while ((line = br.readLine()) != null) {
                lineNum++;
                if (line.isBlank()) continue;

                String[] p = parseCsvLine(line);
                if (p.length < 22) {
                    log.warn("⚠️  Skipping malformed line {} ({} cols)", lineNum, p.length);
                    continue;
                }

                KundaliBasic k = KundaliBasic.builder()
                        .zodiac(p[0].trim())
                        .panchangTithi(p[1].trim())
                        .karana(p[2].trim())
                        .yoga(p[3].trim())
                        .nakshatra(p[4].trim())
                        .nakshatraLord(p[5].trim())
                        .ascendant(p[6].trim())
                        .ascendantLord(p[7].trim())
                        .sunrise(p[8].trim())
                        .sunset(p[9].trim())
                        .varna(p[10].trim())
                        .vashya(p[11].trim())
                        .yoni(p[12].trim())
                        .gan(p[13].trim())
                        .nadi(p[14].trim())
                        .sign(p[15].trim())
                        .signLord(p[16].trim())
                        .charan(p[17].trim())
                        .tatva(p[18].trim())
                        .nameAlphabet(p[19].trim())
                        .paya(p[20].trim())
                        .yunja(p[21].trim())
                        .build();

                rows.add(k);
            }
        }

        repository.saveAll(rows);
        log.info("✅ Imported {} rows into kundali_basic", rows.size());
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