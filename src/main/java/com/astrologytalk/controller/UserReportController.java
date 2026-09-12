package com.astrologytalk.controller;

import com.astrologytalk.common.response.ApiResponse;
import com.astrologytalk.entity.ReportEntry;
import com.astrologytalk.entity.User;
import com.astrologytalk.repository.ReportEntryRepository;
import com.astrologytalk.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user/report")
@RequiredArgsConstructor
public class UserReportController {

    private final ReportEntryRepository reportRepository;
    private final PaymentService paymentService;

    @GetMapping("/general")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> general(
            @AuthenticationPrincipal User user) {
        return ok("GENERAL", user);
    }

    @GetMapping("/remedies")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> remedies(
            @AuthenticationPrincipal User user) {
        return ok("REMEDY", user);
    }

    @GetMapping("/doshas")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> doshas(
            @AuthenticationPrincipal User user) {
        return ok("DOSHA", user);
    }

    @GetMapping("/gemstones")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> gemstones(
            @AuthenticationPrincipal User user) {
        return ok("GEMSTONE", user);
    }

    private ResponseEntity<ApiResponse<List<Map<String, Object>>>> ok(
            String type, User user) {
        checkAccess(user.getId());
        List<Map<String, Object>> data = loadEntries(type, user);
        return ResponseEntity.ok(ApiResponse.success("OK", data));
    }

    private void checkAccess(Long userId) {
        if (!paymentService.hasPurchasedReport(userId)) {
            throw new RuntimeException("Report not purchased");
        }
    }

    private List<Map<String, Object>> loadEntries(String type, User user) {
        String zodiac = user.getZodiacSign();

        List<ReportEntry> entries;

        if (zodiac != null && !zodiac.isBlank()) {
            List<ReportEntry> filtered = reportRepository
                    .findByTypeAndZodiacAndIsActiveTrueOrderByIdAsc(type, zodiac.toUpperCase());

            if (!filtered.isEmpty()) {
                entries = filtered;
            } else {
                entries = reportRepository.findByTypeAndIsActiveTrueOrderByIdAsc(type);
            }
        } else {
            entries = reportRepository.findByTypeAndIsActiveTrueOrderByIdAsc(type);
        }

        return entries.stream().map(this::toMap).collect(Collectors.toList());
    }

    private Map<String, Object> toMap(ReportEntry e) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", e.getId());
        m.put("zodiac", e.getZodiac());
        m.put("category", e.getCategory());
        m.put("section", e.getCategory());
        m.put("title", e.getTitle());
        m.put("content", e.getContent());
        m.put("field1", e.getField1());
        m.put("field2", e.getField2());
        m.put("field3", e.getField3());
        m.put("field4", e.getField4());

        if ("DOSHA".equals(e.getType())) {
            m.put("doshaName", e.getTitle());
            m.put("description", e.getContent());
            m.put("severity", e.getField1());
            m.put("present", "true".equalsIgnoreCase(e.getField2()));
            m.put("remedy", e.getField3());
        } else if ("GEMSTONE".equals(e.getType())) {
            m.put("planet", e.getField1());
            m.put("gemstone", e.getTitle());
            m.put("metal", e.getField2());
            m.put("finger", e.getField3());
            m.put("day", e.getField4());
            m.put("benefit", e.getContent());
        } else if ("REMEDY".equals(e.getType())) {
            m.put("category", e.getCategory());
        } else if ("GENERAL".equals(e.getType())) {
            m.put("section", e.getCategory());
        }

        return m;
    }
}