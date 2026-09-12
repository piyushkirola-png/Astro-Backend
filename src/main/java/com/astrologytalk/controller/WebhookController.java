package com.astrologytalk.controller;

import com.astrologytalk.service.WebhookService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookService webhookService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/{gatewayName}")
    public ResponseEntity<Map<String, Object>> handleWebhook(
            @PathVariable String gatewayName,
            @RequestBody String rawBody,
            @RequestHeader Map<String, String> headers) {

        log.info("[Webhook] Received from {}: {}", gatewayName, truncate(rawBody, 300));

        Map<String, Object> response = new HashMap<>();

        try {
            // Parse JSON
            Map<String, Object> payload;
            try {
                payload = objectMapper.readValue(rawBody, Map.class);
            } catch (Exception e) {
                log.warn("[Webhook] Non-JSON body from {}", gatewayName);
                payload = new HashMap<>();
            }

            webhookService.processWebhook(gatewayName, rawBody, payload, headers);

            response.put("success", true);
            response.put("message", "Webhook processed");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("[Webhook] Processing failed for {}: {}", gatewayName, e.getMessage(), e);
            response.put("success", false);
            response.put("message", e.getMessage());
            // Still return 200 so Cashfree doesn't retry on our parse errors
            return ResponseEntity.ok(response);
        }
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() > max ? s.substring(0, max) + "..." : s;
    }
}