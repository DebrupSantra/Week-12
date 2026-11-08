package com.acme.ecommerce.inventory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "stock_items")
public class StockItem {
    @Id
    private String sku;
    @Column(nullable = false)
    private int available;
    protected StockItem() {}
    public StockItem(String sku, int available) { this.sku = sku; this.available = available; }
    public String getSku() { return sku; }
    public int getAvailable() { return available; }
    public void add(int amount) { available += amount; }
    public void reserve(int amount) { available -= amount; }
}
