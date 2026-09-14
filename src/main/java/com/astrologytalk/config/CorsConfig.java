package com.astrologytalk.config;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfig {

  @Value("${frontend.url}")
  private String frontendUrl;

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

    // === Payment gateway callbacks — permissive (no auth, no sensitive data) ===
    CorsConfiguration gateway = new CorsConfiguration();
    gateway.setAllowedOriginPatterns(List.of("*"));
    gateway.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    gateway.setAllowedHeaders(List.of("*"));
    gateway.setAllowCredentials(false);
    gateway.setMaxAge(3600L);

    source.registerCorsConfiguration("/api/webhooks/**", gateway);
    source.registerCorsConfiguration("/api/payments/payu/**", gateway);

    // === Everything else — strict ===
    CorsConfiguration strict = new CorsConfiguration();
    strict.setAllowedOriginPatterns(
        List.of(frontendUrl, "http://localhost:*", "http://127.0.0.1:*"));
    strict.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    strict.setAllowedHeaders(List.of("*"));
    strict.setAllowCredentials(true);
    strict.setMaxAge(3600L);

    source.registerCorsConfiguration("/**", strict);

    return source;
  }
}