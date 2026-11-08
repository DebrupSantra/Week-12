package com.acme.ecommerce.order;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class Order {
    @Id
    private UUID id;
    private String customerId;
    private UUID productId;
    private String sku;
    private int quantity;
    private BigDecimal total;
    private String status;
    private Instant createdAt;

    protected Order() {}

    public Order(String customerId, UUID productId, String sku, int quantity, BigDecimal total) {
        this.id = UUID.randomUUID();
        this.customerId = customerId;
        this.productId = productId;
        this.sku = sku;
        this.quantity = quantity;
        this.total = total;
        this.status = "PENDING";
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getCustomerId() { return customerId; }
    public UUID getProductId() { return productId; }
    public String getSku() { return sku; }
    public int getQuantity() { return quantity; }
    public BigDecimal getTotal() { return total; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public void confirm() { status = "CONFIRMED"; }
    public void fail() { status = "FAILED"; }

    public record CreateOrderRequest(@NotNull UUID productId, @Min(1) int quantity) {}
}
