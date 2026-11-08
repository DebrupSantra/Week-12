package com.acme.ecommerce.payment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments")
public class Payment {
    @Id
    private UUID id;
    @Column(nullable = false, unique = true)
    private UUID orderId;
    @Column(nullable = false)
    private String customerId;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;
    @Column(nullable = false)
    private String status;
    @Column(nullable = false)
    private Instant createdAt;

    protected Payment() {}
    public Payment(UUID orderId, String customerId, BigDecimal amount) {
        this.id = UUID.randomUUID();
        this.orderId = orderId;
        this.customerId = customerId;
        this.amount = amount;
        this.status = "AUTHORIZED";
        this.createdAt = Instant.now();
    }
    public UUID getId() { return id; }
    public UUID getOrderId() { return orderId; }
    public String getCustomerId() { return customerId; }
    public BigDecimal getAmount() { return amount; }
    public String getStatus() { return status; }
    public void refund() { status = "REFUNDED"; }
}
