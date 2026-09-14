package com.astrologytalk.controller;

import com.astrologytalk.common.response.ApiResponse;
import com.astrologytalk.dto.response.DashaPeriodResponse;
import com.astrologytalk.dto.response.KundaliBasicResponse;
import com.astrologytalk.dto.response.KundaliChartResponse;
import com.astrologytalk.dto.response.PlanetaryPositionResponse;
import com.astrologytalk.entity.User;
import com.astrologytalk.service.KundaliPdfService;
import com.astrologytalk.service.KundaliService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user/kundali")
@RequiredArgsConstructor
public class UserKundaliController {

  private final KundaliService kundaliService;
  private final KundaliPdfService kundaliPdfService;

  @GetMapping("/basic")
  public ResponseEntity<ApiResponse<KundaliBasicResponse>> getBasic(
      @AuthenticationPrincipal User user) {
    return ResponseEntity.ok(
        ApiResponse.success("Kundali basic fetched", kundaliService.getBasic(user.getId())));
  }

  @GetMapping("/charts")
  public ResponseEntity<ApiResponse<List<KundaliChartResponse>>> getCharts(
      @AuthenticationPrincipal User user) {
    return ResponseEntity.ok(
        ApiResponse.success("Kundali charts fetched", kundaliService.getCharts(user.getId())));
  }

  @GetMapping("/planetary")
  public ResponseEntity<ApiResponse<List<PlanetaryPositionResponse>>> getPlanetary(
      @AuthenticationPrincipal User user) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Planetary positions fetched", kundaliService.getPlanetaryPositions(user.getId())));
  }

  @GetMapping("/dasha")
  public ResponseEntity<ApiResponse<List<DashaPeriodResponse>>> getDasha(
      @AuthenticationPrincipal User user) {
    return ResponseEntity.ok(
        ApiResponse.success("Dasha periods fetched", kundaliService.getDashaPeriods(user.getId())));
  }

  @GetMapping("/pdf")
  public ResponseEntity<byte[]> downloadPdf(@AuthenticationPrincipal User user) {
    byte[] pdf = kundaliPdfService.generate(user);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_PDF);
    headers.setContentDispositionFormData("attachment", "kundali_" + user.getId() + ".pdf");

    return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
  }
}
