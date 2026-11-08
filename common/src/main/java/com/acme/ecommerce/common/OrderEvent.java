package com.acme.ecommerce.common;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderEvent(
        UUID eventId,
        UUID orderId,
        String customerId,
        String eventType,
        BigDecimal total,
        String status,
        Instant occurredAt) {
}
