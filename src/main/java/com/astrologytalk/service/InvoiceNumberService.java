package com.astrologytalk.service;

import com.astrologytalk.entity.Payment;
import com.astrologytalk.repository.PaymentRepository;
import java.time.Year;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceNumberService {

  private final PaymentRepository paymentRepository;

  private static final String PREFIX = "INV";
  private static final int PAD = 4;

  /**
   * Generates next invoice number for the current year. Format: INV-2026-0001, INV-2026-0002, ...
   * Sequence resets every year.
   *
   * <p>Thread-safety note: not fully concurrent-safe. In practice, very low volume means collisions
   * are rare. If two payments happen in the same millisecond, one will fail on the UNIQUE
   * constraint — caller should retry.
   */
  public synchronized String generate() {
    int currentYear = Year.now().getValue();
    String yearPrefix = PREFIX + "-" + currentYear + "-";

    Optional<String> lastInvoice = paymentRepository.findMaxInvoiceNumberForYear(yearPrefix);

    int nextSeq = 1;

    if (lastInvoice.isPresent()) {
      String last = lastInvoice.get();
      try {
        String seqPart = last.substring(last.lastIndexOf('-') + 1);
        nextSeq = Integer.parseInt(seqPart) + 1;
      } catch (NumberFormatException e) {
        log.warn("[Invoice#] Could not parse last invoice number: {}", last);
      }
    }

    String formatted = yearPrefix + String.format("%0" + PAD + "d", nextSeq);
    log.info("[Invoice#] Generated: {}", formatted);
    return formatted;
  }

  /** Assigns invoice number to payment if not already assigned. Safe to call multiple times. */
  public void assignIfMissing(Payment payment) {
    if (payment.getInvoiceNumber() != null && !payment.getInvoiceNumber().isBlank()) {
      return;
    }
    String invoiceNumber = generate();
    payment.setInvoiceNumber(invoiceNumber);
    log.info("[Invoice#] Assigned {} to payment {}", invoiceNumber, payment.getGatewayOrderId());
  }
}
