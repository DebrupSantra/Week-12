package com.acme.ecommerce.inventory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "inventory_reservations")
public class Reservation {
    @Id
    private UUID orderId;
    @Column(nullable = false)
    private String sku;
    @Column(nullable = false)
    private int quantity;
    @Column(nullable = false)
    private String status;
    protected Reservation() {}
    public Reservation(UUID orderId, String sku, int quantity) {
        this.orderId = orderId;
        this.sku = sku;
        this.quantity = quantity;
        this.status = "RESERVED";
    }
    public UUID getOrderId() { return orderId; }
    public String getSku() { return sku; }
    public int getQuantity() { return quantity; }
    public String getStatus() { return status; }
    public void release() { status = "RELEASED"; }
}
