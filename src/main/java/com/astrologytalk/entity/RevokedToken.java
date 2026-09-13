package com.astrologytalk.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "revoked_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RevokedToken {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 512)
  private String token;

  @Column(name = "revoked_at")
  private LocalDateTime revokedAt;

  @Column(name = "expires_at", nullable = false)
  private LocalDateTime expiresAt;

  @PrePersist
  protected void onCreate() {
    revokedAt = LocalDateTime.now();
  }
}
