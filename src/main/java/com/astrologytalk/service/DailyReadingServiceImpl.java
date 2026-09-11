package com.astrologytalk.service;

import com.astrologytalk.dto.response.DailyReadingResponse;
import com.astrologytalk.entity.ContentEntry;
import com.astrologytalk.entity.DailyReading;
import com.astrologytalk.entity.User;
import com.astrologytalk.repository.ContentLibraryRepository;
import com.astrologytalk.repository.DailyReadingRepository;
import com.astrologytalk.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyReadingServiceImpl implements DailyReadingService {

    private final UserRepository userRepository;
    private final DailyReadingRepository dailyRepo;
    private final ContentLibraryRepository contentRepo;

    private static final String LANG = "HINGLISH";

    private static final Map<String, String> ZODIAC_SYMBOL = new HashMap<>();
    static {
        ZODIAC_SYMBOL.put("Aries", "♈");
        ZODIAC_SYMBOL.put("Taurus", "♉");
        ZODIAC_SYMBOL.put("Gemini", "♊");
        ZODIAC_SYMBOL.put("Cancer", "♋");
        ZODIAC_SYMBOL.put("Leo", "♌");
        ZODIAC_SYMBOL.put("Virgo", "♍");
        ZODIAC_SYMBOL.put("Libra", "♎");
        ZODIAC_SYMBOL.put("Scorpio", "♏");
        ZODIAC_SYMBOL.put("Sagittarius", "♐");
        ZODIAC_SYMBOL.put("Capricorn", "♑");
        ZODIAC_SYMBOL.put("Aquarius", "♒");
        ZODIAC_SYMBOL.put("Pisces", "♓");
    }

    private static final String[] ZODIAC_SIGNS = {
        "Aries", "Taurus", "Gemini", "Cancer", "Leo", "Virgo",
        "Libra", "Scorpio", "Sagittarius", "Capricorn", "Aquarius", "Pisces"
    };

    private static final String[] SIGNATURES = {
        "MARS_FAVORABLE",
        "MARS_CHALLENGING",
        "VENUS_FAVORABLE",
        "VENUS_CHALLENGING",
        "JUPITER_FAVORABLE",
        "JUPITER_CHALLENGING",
        "SATURN_FAVORABLE",
        "SATURN_CHALLENGING",
        "MOON_FAVORABLE",
        "MOON_CHALLENGING",
        "SUN_FAVORABLE",
        "SUN_CHALLENGING",
        "MERCURY_FAVORABLE",
        "MERCURY_CHALLENGING",
        "RAHU_CHALLENGING",
        "KETU_CHALLENGING",
    };

    private static final Map<String, Integer> PLANET_NUMBER = Map.of(
        "MARS", 9, "VENUS", 6, "JUPITER", 3, "SATURN", 8, "MOON", 2,
        "SUN", 1, "MERCURY", 5, "RAHU", 4, "KETU", 7
    );

    private static final Map<String, String> PLANET_COLOR = Map.of(
        "MARS", "Red", "VENUS", "Pink", "JUPITER", "Yellow", "SATURN", "Blue",
        "MOON", "White", "SUN", "Orange", "MERCURY", "Green", "RAHU", "Grey", "KETU", "Multicolor"
    );

    @Override
    @Transactional
    public DailyReadingResponse getOrGenerateForToday(Long userId) {
        LocalDate today = LocalDate.now();

        var existing = dailyRepo.findByUserIdAndReadingDate(userId, today);
        if (existing.isPresent()) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            return toResponse(existing.get(), user);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String zodiac = user.getZodiacSign();
        boolean hasZodiac = zodiac != null && !zodiac.isBlank();

        long seed = ((long) userId) * 1_000_003L + today.toEpochDay() * 31L;
        Random rnd = new Random(seed);

        if (!hasZodiac) {
            zodiac = ZODIAC_SIGNS[rnd.nextInt(ZODIAC_SIGNS.length)];
        }

        String signature = SIGNATURES[rnd.nextInt(SIGNATURES.length)];
        String dominantPlanet = signature.split("_")[0];

        int luckyNumber = PLANET_NUMBER.getOrDefault(dominantPlanet, 5);
        String luckyColor = PLANET_COLOR.getOrDefault(dominantPlanet, "White");
        int energyLevel = 30 + rnd.nextInt(71);
        String moodTrend = energyLevel > 66 ? "RISING" : energyLevel > 45 ? "STABLE" : "LOW";

        String forecastText = pickText(signature, "GENERAL", rnd);
        String loveText     = pickText(signature, "LOVE", rnd);
        String careerText   = pickText(signature, "CAREER", rnd);
        String wellnessText = pickText(signature, "WELLNESS", rnd);
        String healthText   = pickText(signature, "HEALTH", rnd);
        String financeText  = pickText(signature, "FINANCE", rnd);

        DailyReading reading = DailyReading.builder()
                .userId(userId)
                .readingDate(today)
                .signature(signature)
                .luckyNumber(luckyNumber)
                .luckyColor(luckyColor)
                .energyLevel(energyLevel)
                .moodTrend(moodTrend)
                .forecastText(forecastText)
                .loveText(loveText)
                .careerText(careerText)
                .wellnessText(wellnessText)
                .healthText(healthText)
                .financeText(financeText)
                .language(LANG)
                .build();

        try {
            reading = dailyRepo.save(reading);
        } catch (Exception e) {
            reading = dailyRepo.findByUserIdAndReadingDate(userId, today)
                    .orElseThrow(() -> new RuntimeException("Failed to save or fetch reading"));
        }

        return toResponseWithZodiac(reading, zodiac, hasZodiac);
    }

    private String pickText(String signature, String category, Random rnd) {
        List<ContentEntry> options = contentRepo
                .findBySignatureAndCategoryAndLanguage(signature, category, LANG);

        if (options.isEmpty()) {
            options = contentRepo.findBySignatureAndCategoryAndLanguage(signature, "GENERAL", LANG);
        }
        if (options.isEmpty()) {
            log.warn("No content found for signature={} category={}", signature, category);
            return "";
        }
        return options.get(rnd.nextInt(options.size())).getText();
    }

    private DailyReadingResponse toResponse(DailyReading r, User user) {
        String zodiac = user.getZodiacSign();
        boolean hasZodiac = zodiac != null && !zodiac.isBlank();
        return toResponseWithZodiac(r, hasZodiac ? zodiac : "Aries", hasZodiac);
    }

    private DailyReadingResponse toResponseWithZodiac(DailyReading r, String zodiac, boolean hasZodiac) {
        return DailyReadingResponse.builder()
                .readingDate(r.getReadingDate())
                .zodiacSign(zodiac)
                .zodiacSymbol(ZODIAC_SYMBOL.getOrDefault(zodiac, "✦"))
                .luckyNumber(r.getLuckyNumber())
                .luckyColor(r.getLuckyColor())
                .energyLevel(r.getEnergyLevel())
                .moodTrend(r.getMoodTrend())
                .forecastText(r.getForecastText())
                .loveText(r.getLoveText())
                .careerText(r.getCareerText())
                .wellnessText(r.getWellnessText())
                .healthText(r.getHealthText())
                .financeText(r.getFinanceText())
                .signature(r.getSignature())
                .language(r.getLanguage())
                .hasZodiac(hasZodiac)
                .build();
    }
}