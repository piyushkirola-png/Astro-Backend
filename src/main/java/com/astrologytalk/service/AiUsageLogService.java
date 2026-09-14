package com.astrologytalk.service;

import com.astrologytalk.entity.AiUsageLog;
import com.astrologytalk.repository.AiUsageLogRepository;
import com.astrologytalk.repository.PaymentRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiUsageLogService {

  private final AiUsageLogRepository repo;
  private final PaymentRepository paymentRepository;

  /**
   * Increment today's usage for user + session. Computes ₹ equivalent from the user's overall rate.
   */
  @Transactional
  public void recordSeconds(Long userId, Long sessionId, int seconds) {
    if (seconds <= 0) return;

    LocalDate today = LocalDate.now();

    AiUsageLog log =
        repo.findByUserIdAndSessionIdAndUsageDate(userId, sessionId, today)
            .orElseGet(
                () -> {
                  AiUsageLog l = new AiUsageLog();
                  l.setUserId(userId);
                  l.setSessionId(sessionId);
                  l.setUsageDate(today);
                  return l;
                });

    BigDecimal rate = computeRate(userId);
    BigDecimal rupees =
        rate.multiply(BigDecimal.valueOf(seconds)).setScale(2, RoundingMode.HALF_UP);

    log.setSecondsUsed((log.getSecondsUsed() != null ? log.getSecondsUsed() : 0) + seconds);
    log.setRupeesDeducted(
        (log.getRupeesDeducted() != null ? log.getRupeesDeducted() : BigDecimal.ZERO).add(rupees));

    repo.save(log);
  }

  /** Increment today's message count for user + session. */
  @Transactional
  public void recordMessage(Long userId, Long sessionId) {
    LocalDate today = LocalDate.now();

    AiUsageLog log =
        repo.findByUserIdAndSessionIdAndUsageDate(userId, sessionId, today)
            .orElseGet(
                () -> {
                  AiUsageLog l = new AiUsageLog();
                  l.setUserId(userId);
                  l.setSessionId(sessionId);
                  l.setUsageDate(today);
                  return l;
                });

    log.setMessageCount((log.getMessageCount() != null ? log.getMessageCount() : 0) + 1);
    repo.save(log);
  }

  public List<AiUsageLog> getUserUsage(Long userId) {
    return repo.findByUserIdOrderByUsageDateDescIdDesc(userId);
  }

  /** Compute user's rate = total base_amount / total seconds bought (all-time). */
  private BigDecimal computeRate(Long userId) {
    BigDecimal base = paymentRepository.sumWalletBaseAmountByUser(userId);
    Long seconds = paymentRepository.sumWalletSecondsByUser(userId);

    if (base == null || seconds == null || seconds <= 0) {
      return BigDecimal.ZERO;
    }

    return base.divide(BigDecimal.valueOf(seconds), 6, RoundingMode.HALF_UP);
  }
}
