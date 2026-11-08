package com.acme.ecommerce.order;

import java.math.BigDecimal;
import java.util.UUID;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.stereotype.Component;

@Component
public class FulfillmentClient {
    private final ProductClient products;
    private final InventoryClient inventory;
    private final PaymentClient payments;

    public FulfillmentClient(ProductClient products, InventoryClient inventory, PaymentClient payments) {
        this.products = products;
        this.inventory = inventory;
        this.payments = payments;
    }

    @CircuitBreaker(name = "product", fallbackMethod = "productFallback")
    public ProductView product(UUID id) {
        ProductClient.ProductResponse product = products.get(id);
        return new ProductView(product.id(), product.sku(), product.name(), product.price());
    }

    @CircuitBreaker(name = "inventory", fallbackMethod = "inventoryFallback")
    @Retry(name = "inventory")
    public void reserve(UUID orderId, String sku, int quantity) {
        inventory.reserve(new ReservationRequest(orderId, sku, quantity));
    }

    @CircuitBreaker(name = "inventory", fallbackMethod = "releaseFallback")
    public void release(UUID orderId) {
        inventory.release(orderId);
    }

    @CircuitBreaker(name = "payment", fallbackMethod = "paymentFallback")
    @Retry(name = "payment")
    public void authorize(UUID orderId, String customerId, BigDecimal amount) {
        payments.authorize(new PaymentRequest(orderId, customerId, amount));
    }

    @CircuitBreaker(name = "payment", fallbackMethod = "refundFallback")
    public void refund(UUID orderId) {
        payments.refund(orderId);
    }

    public record ProductView(UUID id, String sku, String name, BigDecimal price) {}
    public record ReservationRequest(UUID orderId, String sku, int quantity) {}
    public record PaymentRequest(UUID orderId, String customerId, BigDecimal amount) {}

    private ProductView productFallback(UUID id, Throwable failure) {
        throw new IllegalStateException("Product service is unavailable", failure);
    }

    private void inventoryFallback(UUID orderId, String sku, int quantity, Throwable failure) {
        throw new IllegalStateException("Inventory service is unavailable", failure);
    }

    private void releaseFallback(UUID orderId, Throwable failure) {
        throw new IllegalStateException("Inventory release failed", failure);
    }

    private void paymentFallback(UUID orderId, String customerId, BigDecimal amount, Throwable failure) {
        throw new IllegalStateException("Payment service is unavailable", failure);
    }

    private void refundFallback(UUID orderId, Throwable failure) {
        throw new IllegalStateException("Payment refund failed", failure);
    }
}
