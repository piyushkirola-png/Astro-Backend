package com.astrologytalk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class AstrologyApplication {
  public static void main(String[] args) {
    SpringApplication.run(AstrologyApplication.class, args);
  }
}
