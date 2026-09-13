package com.astrologytalk.service;

import com.astrologytalk.dto.response.DashaPeriodResponse;
import com.astrologytalk.dto.response.KundaliBasicResponse;
import com.astrologytalk.dto.response.KundaliChartResponse;
import com.astrologytalk.dto.response.PlanetaryPositionResponse;
import com.astrologytalk.entity.KundaliBasic;
import com.astrologytalk.entity.KundaliChart;
import com.astrologytalk.entity.PlanetaryPosition;
import com.astrologytalk.entity.User;
import com.astrologytalk.repository.DashaPeriodRepository;
import com.astrologytalk.repository.KundaliBasicRepository;
import com.astrologytalk.repository.KundaliChartRepository;
import com.astrologytalk.repository.PlanetaryPositionRepository;
import com.astrologytalk.repository.UserRepository;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KundaliServiceImpl implements KundaliService {

  private final UserRepository userRepository;
  private final KundaliBasicRepository kundaliBasicRepository;
  private final KundaliChartRepository kundaliChartRepository;
  private final PlanetaryPositionRepository planetaryPositionRepository;
  private final DashaPeriodRepository dashaPeriodRepository;

  private static final DateTimeFormatter TOB_FMT = DateTimeFormatter.ofPattern("HH:mm");
  private static final DateTimeFormatter DISPLAY_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

  @Override
  public KundaliBasicResponse getBasic(Long userId) {
    User user =
        userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

    String zodiac = user.getZodiacSign();
    KundaliBasic k = null;
    if (zodiac != null && !zodiac.isBlank()) {
      k = kundaliBasicRepository.findByZodiac(zodiac).orElse(null);
    }
    if (k == null) {
      k =
          kundaliBasicRepository
              .findByZodiac("Aries")
              .orElseThrow(() -> new RuntimeException("Kundali basic data not loaded"));
    }

    String timeOfBirth = user.getTimeOfBirth() != null ? user.getTimeOfBirth().format(TOB_FMT) : "";

    return KundaliBasicResponse.builder()
        .name(user.getName())
        .gender(user.getGender() != null ? user.getGender().name() : null)
        .dateOfBirth(user.getDateOfBirth())
        .timeOfBirth(timeOfBirth)
        .placeOfBirth(user.getPlaceOfBirth())
        .birthLat(user.getBirthLat())
        .birthLng(user.getBirthLng())
        .birthTimezone(user.getBirthTimezone())
        .panchangTithi(k.getPanchangTithi())
        .karana(k.getKarana())
        .yoga(k.getYoga())
        .nakshatra(k.getNakshatra())
        .nakshatraLord(k.getNakshatraLord())
        .ascendant(k.getAscendant())
        .ascendantLord(k.getAscendantLord())
        .sunrise(k.getSunrise())
        .sunset(k.getSunset())
        .varna(k.getVarna())
        .vashya(k.getVashya())
        .yoni(k.getYoni())
        .gan(k.getGan())
        .nadi(k.getNadi())
        .sign(k.getSign())
        .signLord(k.getSignLord())
        .charan(k.getCharan())
        .tatva(k.getTatva())
        .nameAlphabet(k.getNameAlphabet())
        .paya(k.getPaya())
        .yunja(k.getYunja())
        .build();
  }

  @Override
  public List<KundaliChartResponse> getCharts(Long userId) {
    User user =
        userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
    String zodiac = user.getZodiacSign();
    if (zodiac == null || zodiac.isBlank()) zodiac = "Aries";

    List<KundaliChartResponse> out = new ArrayList<>();
    out.add(loadChart(zodiac, "D1", "Lagna Chart"));
    out.add(loadChart(zodiac, "D9", "Navamsa Chart"));
    return out;
  }

  @Override
  public List<PlanetaryPositionResponse> getPlanetaryPositions(Long userId) {
    User user =
        userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
    String zodiac = user.getZodiacSign();
    if (zodiac == null || zodiac.isBlank()) zodiac = "Aries";

    return planetaryPositionRepository.findByZodiacOrderByIdAsc(zodiac).stream()
        .map(this::toPlanetaryResponse)
        .collect(Collectors.toList());
  }

  @Override
  public List<DashaPeriodResponse> getDashaPeriods(Long userId) {
    User user =
        userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

    LocalDate dob = user.getDateOfBirth();
    if (dob == null) {
      log.warn("User {} has no dateOfBirth — cannot compute dasha dates", userId);
      return List.of();
    }

    String zodiac = user.getZodiacSign();
    if (zodiac == null || zodiac.isBlank()) zodiac = "Aries";

    LocalDate today = LocalDate.now();

    return dashaPeriodRepository.findByZodiacOrderByStartOffsetYearsAsc(zodiac).stream()
        .map(
            d -> {
              LocalDate start = dob.plusYears(d.getStartOffsetYears());
              LocalDate end = dob.plusYears(d.getEndOffsetYears());
              boolean active = !today.isBefore(start) && !today.isAfter(end);

              return DashaPeriodResponse.builder()
                  .planet(d.getPlanet())
                  .startOffsetYears(d.getStartOffsetYears())
                  .endOffsetYears(d.getEndOffsetYears())
                  .house(d.getHouse())
                  .sign(d.getSign())
                  .startDate(start)
                  .endDate(end)
                  .startDateFormatted(start.format(DISPLAY_FMT))
                  .endDateFormatted(end.format(DISPLAY_FMT))
                  .isActive(active)
                  .paragraph1(d.getParagraph1())
                  .paragraph2(d.getParagraph2())
                  .build();
            })
        .collect(Collectors.toList());
  }

  private PlanetaryPositionResponse toPlanetaryResponse(PlanetaryPosition p) {
    return PlanetaryPositionResponse.builder()
        .planet(p.getPlanet())
        .sign(p.getSign())
        .signLord(p.getSignLord())
        .nakshatra(p.getNakshatra())
        .nakshatraLord(p.getNakshatraLord())
        .degree(p.getDegree())
        .retro(p.getRetro())
        .house(p.getHouse())
        .state(p.getState())
        .status(p.getStatus())
        .build();
  }

  private KundaliChartResponse loadChart(String zodiac, String type, String label) {
    KundaliChart c = kundaliChartRepository.findByZodiacAndChartType(zodiac, type).orElse(null);
    if (c == null) {
      log.warn("Kundali chart not found for zodiac={} type={}", zodiac, type);
      return KundaliChartResponse.builder()
          .chartType(type)
          .chartLabel(label)
          .zodiac(zodiac)
          .houses(emptyHouses())
          .build();
    }
    List<String> houses = new ArrayList<>();
    houses.add(nz(c.getHouse1()));
    houses.add(nz(c.getHouse2()));
    houses.add(nz(c.getHouse3()));
    houses.add(nz(c.getHouse4()));
    houses.add(nz(c.getHouse5()));
    houses.add(nz(c.getHouse6()));
    houses.add(nz(c.getHouse7()));
    houses.add(nz(c.getHouse8()));
    houses.add(nz(c.getHouse9()));
    houses.add(nz(c.getHouse10()));
    houses.add(nz(c.getHouse11()));
    houses.add(nz(c.getHouse12()));

    return KundaliChartResponse.builder()
        .chartType(type)
        .chartLabel(label)
        .zodiac(zodiac)
        .houses(houses)
        .build();
  }

  private String nz(String s) {
    return s == null ? "" : s;
  }

  private List<String> emptyHouses() {
    List<String> e = new ArrayList<>();
    for (int i = 0; i < 12; i++) e.add("");
    return e;
  }
}
