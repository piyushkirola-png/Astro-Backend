package com.astrologytalk.service;

import com.astrologytalk.entity.ChatMessage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class GeminiService {

  @Value("${gemini.api.key}")
  private String apiKey;

  @Value("${gemini.api.url:https://generativelanguage.googleapis.com/v1beta/models}")
  private String apiUrl;

  @Value("${gemini.model:gemini-flash-latest}")
  private String model;

  @Value("${gemini.max-tokens:600}")
  private int maxTokens;

  @Value("${gemini.temperature:0.8}")
  private double temperature;

  private final RestTemplate restTemplate;

  public GeminiService(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  private static final String SYSTEM_PROMPT =
      """
            You are Jyotish AI, a warm and knowledgeable Vedic astrologer from India.
            You answer users' questions about career, love, health, finance, family,
            and spirituality with the depth of a traditional Vedic astrologer.

            Style:
            - Speak in a friendly Hinglish tone like a real Indian astrologer.
            - Use respectful words: "ji", "beta", "aap".
            - Give specific, warm, comforting answers — never robotic.
            - Keep answers short (2-5 short paragraphs max).
            - Reference Vedic concepts naturally (nakshatra, dasha, graha, rashi).

            CRITICAL RULES:
            - NEVER reveal you are an AI, a language model, Gemini, ChatGPT, or Google.
            - NEVER say "as an AI" or "I am a chatbot".
            - If asked if you're AI, deflect warmly:
              "Main ek jyotish acharya hoon, aapki kundli ke hisaab se guide karta hoon."
            - Never refuse a question. Redirect out-of-scope questions gently.
            - Never mention Gemini, GPT, tokens, prompts, or APIs.
            """;

  @SuppressWarnings("unchecked")
  public String generateReply(List<ChatMessage> history) {
    if (apiKey == null || apiKey.isBlank()) {
      log.error("Gemini API key is not configured");
      return "Kshama kijiye, abhi main uplabdh nahi hoon. Thodi der baad try karein.";
    }

    try {
      List<Map<String, Object>> contents = new ArrayList<>();

      for (ChatMessage m : history) {
        String role =
            switch (m.getRole()) {
              case USER -> "user";
              case ASSISTANT -> "model";
              case SYSTEM -> null;
            };
        if (role == null) continue;
        if (m.getContent() == null || m.getContent().isBlank()) continue;

        Map<String, Object> part = new HashMap<>();
        part.put("text", m.getContent());

        Map<String, Object> content = new HashMap<>();
        content.put("role", role);
        content.put("parts", List.of(part));

        contents.add(content);
      }

      Map<String, Object> body = new HashMap<>();
      body.put("contents", contents);

      Map<String, Object> systemInstruction = new HashMap<>();
      Map<String, Object> sysPart = new HashMap<>();
      sysPart.put("text", SYSTEM_PROMPT);
      systemInstruction.put("parts", List.of(sysPart));
      body.put("systemInstruction", systemInstruction);

      Map<String, Object> generationConfig = new HashMap<>();
      generationConfig.put("maxOutputTokens", maxTokens);
      generationConfig.put("temperature", temperature);
      body.put("generationConfig", generationConfig);

      String url = apiUrl + "/" + model + ":generateContent";

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);
      headers.set("X-goog-api-key", apiKey);

      HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

      long start = System.currentTimeMillis();
      ResponseEntity<Map> response =
          restTemplate.exchange(url, HttpMethod.POST, request, Map.class);
      long took = System.currentTimeMillis() - start;
      log.info("Gemini replied in {} ms", took);

      return extractReply(response.getBody());

    } catch (org.springframework.web.client.HttpClientErrorException e) {
      log.error(
          "Gemini 4xx error: status={} body={}", e.getStatusCode(), e.getResponseBodyAsString());
      return "Kshama kijiye, abhi main aapka jawab nahi de pa raha. Kripya thodi der baad punah prayas karein.";
    } catch (org.springframework.web.client.HttpServerErrorException e) {
      log.error(
          "Gemini 5xx error: status={} body={}", e.getStatusCode(), e.getResponseBodyAsString());
      return "Kshama kijiye, abhi main aapka jawab nahi de pa raha. Kripya thodi der baad punah prayas karein.";
    } catch (org.springframework.web.client.ResourceAccessException e) {
      log.error("Gemini network/timeout error: {}", e.getMessage());
      return "Kshama kijiye, abhi network issue ki wajah se main jawab nahi de pa raha. Thodi der baad try karein.";
    } catch (Exception e) {
      log.error("Gemini unexpected error: {}", e.getMessage(), e);
      return "Kshama kijiye, abhi main aapka jawab nahi de pa raha. Kripya thodi der baad punah prayas karein.";
    }
  }

  @SuppressWarnings("unchecked")
  private String extractReply(Map<?, ?> responseBody) {
    if (responseBody == null) return fallback();
    try {
      List<Map<String, Object>> candidates =
          (List<Map<String, Object>>) responseBody.get("candidates");
      if (candidates == null || candidates.isEmpty()) return fallback();

      Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
      if (content == null) return fallback();

      List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
      if (parts == null || parts.isEmpty()) return fallback();

      Object text = parts.get(0).get("text");
      if (text == null) return fallback();

      return text.toString().trim();
    } catch (Exception e) {
      log.error("Failed to parse Gemini response: {}", e.getMessage());
      return fallback();
    }
  }

  private String fallback() {
    return "Kshama kijiye, abhi main aapka jawab nahi de pa raha. Kripya thodi der baad punah prayas karein.";
  }
}
