package com.astrologytalk.service;

import com.astrologytalk.entity.Payment;
import com.astrologytalk.repository.PaymentRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CsvExportService {

  private final PaymentRepository paymentRepository;

  private static final DateTimeFormatter DATE_FMT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private static final String[] HEADERS = {
    "Order ID", "Product", "Base Amount (INR)", "GST (INR)",
    "Total Paid (INR)", "Status", "Gateway", "Gateway Payment ID",
    "UTR", "Customer Name", "Customer Email", "Customer Phone",
    "Created At", "Completed At"
  };

  public byte[] exportUserPayments(Long userId) {
    List<Payment> payments = paymentRepository.findByUserIdOrderByCreatedAtDesc(userId);
    return buildCsv(payments);
  }

  public byte[] exportAllPayments() {
    List<Payment> payments = paymentRepository.findAll();
    payments.sort(
        (a, b) -> {
          if (a.getCreatedAt() == null) return 1;
          if (b.getCreatedAt() == null) return -1;
          return b.getCreatedAt().compareTo(a.getCreatedAt());
        });
    return buildCsv(payments);
  }

  private byte[] buildCsv(List<Payment> payments) {
    StringBuilder sb = new StringBuilder();

    sb.append('\uFEFF');

    sb.append(String.join(",", HEADERS)).append("\n");

    for (Payment p : payments) {
      sb.append(csv(orderId(p))).append(',');
      sb.append(csv(productLabel(p))).append(',');
      sb.append(csv(formatAmount(p.getBaseAmount()))).append(',');
      sb.append(csv(formatAmount(p.getGstAmount()))).append(',');
      sb.append(csv(formatAmount(p.getAmount()))).append(',');
      sb.append(csv(nvl(p.getStatus()))).append(',');
      sb.append(csv(nvl(p.getGateway()))).append(',');
      sb.append(csv(nvl(p.getGatewayPaymentId()))).append(',');
      sb.append(csv(nvl(p.getUtr()))).append(',');
      sb.append(csv(nvl(p.getCustomerName()))).append(',');
      sb.append(csv(nvl(p.getCustomerEmail()))).append(',');
      sb.append(csv(nvl(p.getCustomerPhone()))).append(',');
      sb.append(csv(p.getCreatedAt() != null ? p.getCreatedAt().format(DATE_FMT) : "")).append(',');
      sb.append(csv(p.getCompletedAt() != null ? p.getCompletedAt().format(DATE_FMT) : ""));
      sb.append('\n');
    }

    return sb.toString().getBytes(StandardCharsets.UTF_8);
  }

  private String csv(String value) {
    if (value == null) return "";
    String v = value.replace("\"", "\"\"");
    if (v.contains(",") || v.contains("\"") || v.contains("\n")) {
      return "\"" + v + "\"";
    }
    return v;
  }

  private String orderId(Payment p) {
    return p.getGatewayOrderId() != null ? p.getGatewayOrderId() : "";
  }

  private String productLabel(Payment p) {
    if ("REPORT".equalsIgnoreCase(p.getCategoryCode())) {
      return "Kundali Report";
    }
    if ("WALLET".equalsIgnoreCase(p.getCategoryCode())) {
      if (p.getSecondsCredited() != null && p.getSecondsCredited() > 0) {
        int mins = p.getSecondsCredited() / 60;
        return "Chat Recharge - " + mins + " min";
      }
      return "Chat Recharge";
    }
    return p.getNotes() != null ? p.getNotes() : "Payment";
  }

  private String formatAmount(BigDecimal amount) {
    if (amount == null) return "0.00";
    return amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
  }

  private String nvl(String s) {
    return s != null ? s : "";
  }
}
