package com.acme.ecommerce.order;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_messages")
public class OutboxMessage {
    @Id
    private UUID id;
    @Column(nullable = false, columnDefinition = "text")
    private String payload;
    @Column(nullable = false)
    private Instant createdAt;

    protected OutboxMessage() {}
    public OutboxMessage(UUID id, String payload) {
        this.id = id;
        this.payload = payload;
        this.createdAt = Instant.now();
    }
    public UUID getId() { return id; }
    public String getPayload() { return payload; }
}
