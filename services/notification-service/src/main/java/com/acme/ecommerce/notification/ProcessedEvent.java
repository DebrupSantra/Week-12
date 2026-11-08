package com.acme.ecommerce.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "processed_notification_events")
public class ProcessedEvent {
    @Id
    private UUID eventId;
    @Column(nullable = false)
    private UUID orderId;
    @Column(nullable = false)
    private String eventType;
    @Column(nullable = false)
    private Instant processedAt;
    protected ProcessedEvent() {}
    public ProcessedEvent(UUID eventId, UUID orderId, String eventType) {
        this.eventId = eventId;
        this.orderId = orderId;
        this.eventType = eventType;
        this.processedAt = Instant.now();
    }
}
