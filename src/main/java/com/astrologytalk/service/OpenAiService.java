package com.astrologytalk.service;

import com.astrologytalk.entity.ChatMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class OpenAiService {

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.api.url:https://api.openai.com/v1/chat/completions}")
    private String apiUrl;

    @Value("${openai.model:gpt-4o-mini}")
    private String model;

    @Value("${openai.max-tokens:600}")
    private int maxTokens;

    @Value("${openai.temperature:0.8}")
    private double temperature;

    private final RestTemplate restTemplate;

    public OpenAiService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    private static final String SYSTEM_PROMPT = """
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
        - NEVER reveal you are an AI, a language model, ChatGPT, or OpenAI.
        - NEVER say "as an AI" or "I am a chatbot".
        - If asked if you're AI, deflect warmly:
          "Main ek jyotish acharya hoon, aapki kundli ke hisaab se guide karta hoon."
        - Never refuse a question. Redirect out-of-scope questions gently.
        - Never mention OpenAI, GPT, tokens, prompts, or APIs.
        """;

    public String generateReply(List<ChatMessage> history) {
        if (apiKey == null || apiKey.isBlank()) {
            log.error("OpenAI API key is not configured");
            return "Kshama kijiye, abhi main uplabdh nahi hoon. Thodi der baad try karein.";
        }

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", SYSTEM_PROMPT));

        for (ChatMessage m : history) {
            String role = switch (m.getRole()) {
                case USER -> "user";
                case ASSISTANT -> "assistant";
                case SYSTEM -> null;
            };
            if (role != null) {
                messages.add(Map.of("role", role, "content", m.getContent()));
            }
        }

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("max_tokens", maxTokens);
        body.put("temperature", temperature);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            long start = System.currentTimeMillis();
            ResponseEntity<Map> response = restTemplate.exchange(
                    apiUrl, HttpMethod.POST, request, Map.class);
            long took = System.currentTimeMillis() - start;
            log.info("OpenAI replied in {} ms", took);

            return extractReply(response.getBody());
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            log.error("OpenAI 4xx error: status={} body={}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            return "Kshama kijiye, abhi main aapka jawab nahi de pa raha. Kripya thodi der baad punah prayas karein.";
        } catch (org.springframework.web.client.HttpServerErrorException e) {
            log.error("OpenAI 5xx error: status={} body={}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            return "Kshama kijiye, abhi main aapka jawab nahi de pa raha. Kripya thodi der baad punah prayas karein.";
        } catch (org.springframework.web.client.ResourceAccessException e) {
            log.error("OpenAI network/timeout error: {}", e.getMessage());
            return "Kshama kijiye, abhi network issue ki wajah se main jawab nahi de pa raha. Thodi der baad try karein.";
        } catch (Exception e) {
            log.error("OpenAI unexpected error: {}", e.getMessage(), e);
            return "Kshama kijiye, abhi main aapka jawab nahi de pa raha. Kripya thodi der baad punah prayas karein.";
        }
    }

    @SuppressWarnings("unchecked")
    private String extractReply(Map<?, ?> responseBody) {
        if (responseBody == null) return fallback();
        try {
            List<Map<String, Object>> choices =
                    (List<Map<String, Object>>) responseBody.get("choices");
            if (choices == null || choices.isEmpty()) return fallback();

            Map<String, Object> message =
                    (Map<String, Object>) choices.get(0).get("message");
            if (message == null) return fallback();

            Object content = message.get("content");
            if (content == null) return fallback();
            return content.toString().trim();
        } catch (Exception e) {
            log.error("Failed to parse OpenAI response: {}", e.getMessage());
            return fallback();
        }
    }

    private String fallback() {
        return "Kshama kijiye, abhi main aapka jawab nahi de pa raha. Kripya thodi der baad punah prayas karein.";
    }
}