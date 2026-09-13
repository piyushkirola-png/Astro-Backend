package com.astrologytalk.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlacesService {

  private final RestTemplate restTemplate;

  @Value("${app.nominatim.url:https://nominatim.openstreetmap.org}")
  private String nominatimUrl;

  @Value("${app.nominatim.user-agent:AstrologyApp/1.0}")
  private String userAgent;

  @SuppressWarnings("unchecked")
  public List<Map<String, Object>> autocomplete(String query, int limit) {
    if (query == null || query.trim().length() < 3) {
      return List.of();
    }

    String url =
        UriComponentsBuilder.fromHttpUrl(nominatimUrl + "/search")
            .queryParam("q", query.trim())
            .queryParam("format", "json")
            .queryParam("addressdetails", 1)
            .queryParam("limit", limit)
            .build()
            .toUriString();

    HttpHeaders headers = new HttpHeaders();
    headers.set("User-Agent", userAgent);
    headers.set("Accept", "application/json");

    try {
      ResponseEntity<List> response =
          restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), List.class);

      List<Map<String, Object>> raw = response.getBody();
      if (raw == null) return List.of();

      List<Map<String, Object>> out = new ArrayList<>();
      for (Map<String, Object> item : raw) {
        Map<String, Object> slim = new java.util.LinkedHashMap<>();
        slim.put("displayName", item.get("display_name"));
        slim.put("lat", item.get("lat"));
        slim.put("lon", item.get("lon"));

        Object addrObj = item.get("address");
        if (addrObj instanceof Map<?, ?> addrMap) {
          slim.put("city", firstNonNull(addrMap, "city", "town", "village", "suburb"));
          slim.put("state", addrMap.get("state"));
          slim.put("country", addrMap.get("country"));
          slim.put("postcode", addrMap.get("postcode"));
        }
        out.add(slim);
      }
      return out;
    } catch (Exception e) {
      log.warn("Nominatim autocomplete failed for '{}': {}", query, e.getMessage());
      return List.of();
    }
  }

  @SuppressWarnings("unchecked")
  public GeocodeResult geocode(String query) {
    if (query == null || query.isBlank()) return null;

    String url =
        UriComponentsBuilder.fromHttpUrl(nominatimUrl + "/search")
            .queryParam("q", query.trim())
            .queryParam("format", "json")
            .queryParam("limit", 1)
            .build()
            .toUriString();

    HttpHeaders headers = new HttpHeaders();
    headers.set("User-Agent", userAgent);
    headers.set("Accept", "application/json");

    try {
      ResponseEntity<List> response =
          restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), List.class);

      List<Map<String, Object>> raw = response.getBody();
      if (raw == null || raw.isEmpty()) {
        log.warn("Nominatim geocode returned no results for '{}'", query);
        return null;
      }

      Map<String, Object> first = raw.get(0);
      String lat = (String) first.get("lat");
      String lon = (String) first.get("lon");

      if (lat == null || lon == null) return null;

      return new GeocodeResult(Double.parseDouble(lat), Double.parseDouble(lon));
    } catch (Exception e) {
      log.warn("Nominatim geocode failed for '{}': {}", query, e.getMessage());
      return null;
    }
  }

  private Object firstNonNull(Map<?, ?> map, String... keys) {
    for (String k : keys) {
      Object v = map.get(k);
      if (v != null) return v;
    }
    return null;
  }

  // Simple DTO
  public record GeocodeResult(double lat, double lng) {}
}
