package com.acme.ecommerce.order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OrderSagaServiceTest {
    private OrderSagaService saga;
    private UUID productId;
    private FulfillmentClient.ProductView product;
    private Order order;
    private StubPersistence persistence;
    private StubFulfillmentClient fulfillment;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        product = new FulfillmentClient.ProductView(productId, "SKU-1", "Test product", new BigDecimal("12.50"));
        order = new Order("customer-1", productId, "SKU-1", 2, new BigDecimal("25.00"));
        persistence = new StubPersistence(order);
        fulfillment = new StubFulfillmentClient(product);
        saga = new OrderSagaService(null, persistence, fulfillment);
    }

    @Test
    void confirmsOrderWhenInventoryAndPaymentSucceed() {
        Order result = saga.place(new Order.CreateOrderRequest(productId, 2), "customer-1");

        assertEquals("CONFIRMED", result.getStatus());
        assertEquals(1, fulfillment.reservations);
        assertEquals(1, fulfillment.authorizations);
        assertEquals(0, persistence.failures);
    }

    @Test
    void compensatesPaymentAndInventoryWhenOrderConfirmationFails() {
        persistence.failConfirmation = true;

        assertThrows(IllegalStateException.class,
                () -> saga.place(new Order.CreateOrderRequest(productId, 2), "customer-1"));

        assertEquals(1, fulfillment.refunds);
        assertEquals(1, fulfillment.releases);
        assertEquals(1, persistence.failures);
        assertEquals("FAILED", order.getStatus());
    }

    private static class StubFulfillmentClient extends FulfillmentClient {
        private final ProductView product;
        private int reservations;
        private int authorizations;
        private int refunds;
        private int releases;

        StubFulfillmentClient(ProductView product) {
            super(null, null, null);
            this.product = product;
        }

        @Override public ProductView product(UUID id) { return product; }
        @Override public void reserve(UUID orderId, String sku, int quantity) { reservations++; }
        @Override public void authorize(UUID orderId, String customerId, BigDecimal amount) { authorizations++; }
        @Override public void refund(UUID orderId) { refunds++; }
        @Override public void release(UUID orderId) { releases++; }
    }

    private static class StubPersistence extends OrderPersistence {
        private final Order order;
        private boolean failConfirmation;
        private int failures;

        StubPersistence(Order order) {
            super(null, null, null);
            this.order = order;
        }

        @Override public Order createPending(String customerId, FulfillmentClient.ProductView product, int quantity) {
            return order;
        }

        @Override public Order confirm(Order order) {
            if (failConfirmation) throw new IllegalStateException("database unavailable");
            order.confirm();
            return order;
        }

        @Override public Order fail(Order order) {
            failures++;
            order.fail();
            return order;
        }
    }
}
