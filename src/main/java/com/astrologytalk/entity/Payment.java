package com.astrologytalk.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "category_code", length = 32)
  private String categoryCode;

  @Column(name = "package_id")
  private Long packageId;

  @Column(nullable = false, precision = 10, scale = 2)
  private BigDecimal amount;

  @Column(name = "base_amount", precision = 10, scale = 2)
  private BigDecimal baseAmount;

  @Column(name = "gst_amount", precision = 10, scale = 2)
  private BigDecimal gstAmount;

  @Column(name = "gst_rate", precision = 5, scale = 2)
  private BigDecimal gstRate;

  @Column(name = "invoice_number", unique = true, length = 32)
  private String invoiceNumber;

  @Column(nullable = false, length = 8)
  private String currency = "INR";

  @Column(nullable = false, length = 20)
  private String status = "PENDING";

  @Column(nullable = false, length = 32)
  private String gateway;

  @Column(name = "gateway_order_id", nullable = false, unique = true, length = 100)
  private String gatewayOrderId;

  @Column(name = "gateway_payment_id", length = 100)
  private String gatewayPaymentId;

  @Column(name = "payment_link", length = 500)
  private String paymentLink;

  @Column(length = 100)
  private String utr;

  @Column(name = "signature_verified")
  private Boolean signatureVerified = false;

  @Column(name = "customer_name", length = 100)
  private String customerName;

  @Column(name = "customer_email", length = 100)
  private String customerEmail;

  @Column(name = "customer_phone", length = 20)
  private String customerPhone;

  @Column(name = "payment_mode", length = 32)
  private String paymentMode;

  @Column(name = "seconds_credited")
  private Integer secondsCredited;

  @Column(name = "failure_reason", length = 500)
  private String failureReason;

  @Column(name = "failure_code", length = 64)
  private String failureCode;

  @Column(length = 500)
  private String notes;

  @Column(columnDefinition = "TEXT")
  private String metadata;

  @Column(name = "ip_address", length = 45)
  private String ipAddress;

  @Column(name = "user_agent", length = 255)
  private String userAgent;

  @Column(name = "processed_at")
  private LocalDateTime processedAt;

  @Column(name = "completed_at")
  private LocalDateTime completedAt;

  @Column(name = "created_at")
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
