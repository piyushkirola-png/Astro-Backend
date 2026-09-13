package com.astrologytalk.repository;

import com.astrologytalk.entity.WebhookInboundLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WebhookInboundLogRepository extends JpaRepository<WebhookInboundLog, Long> {

  List<WebhookInboundLog> findByOrderIdOrderByCreatedAtDesc(String orderId);

  List<WebhookInboundLog> findByGatewayOrderByCreatedAtDesc(String gateway);
}
