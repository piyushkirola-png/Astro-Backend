package com.astrologytalk.controller;

import com.astrologytalk.common.response.ApiResponse;
import com.astrologytalk.service.PlacesService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/places")
@RequiredArgsConstructor
public class PlacesController {

  private final PlacesService placesService;

  // GET /api/places/autocomplete?q=delhi&limit=8
  @GetMapping("/autocomplete")
  public ResponseEntity<ApiResponse<List<Map<String, Object>>>> autocomplete(
      @RequestParam("q") String query,
      @RequestParam(value = "limit", defaultValue = "8") int limit) {
    List<Map<String, Object>> results = placesService.autocomplete(query, Math.min(limit, 15));
    return ResponseEntity.ok(ApiResponse.success("OK", results));
  }
}
