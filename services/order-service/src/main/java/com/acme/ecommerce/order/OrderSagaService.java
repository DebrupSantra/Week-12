package com.acme.ecommerce.order;

import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderSagaService {
    private static final Logger log = LoggerFactory.getLogger(OrderSagaService.class);
    private final OrderRepository orders;
    private final OrderPersistence persistence;
    private final FulfillmentClient fulfillment;

    public OrderSagaService(OrderRepository orders, OrderPersistence persistence, FulfillmentClient fulfillment) {
        this.orders = orders;
        this.persistence = persistence;
        this.fulfillment = fulfillment;
    }

    public Order place(Order.CreateOrderRequest request, String customerId) {
        FulfillmentClient.ProductView product = fulfillment.product(request.productId());
        Order order = persistence.createPending(customerId, product, request.quantity());
        boolean reservationAttempted = false;
        boolean paymentAttempted = false;
        try {
            reservationAttempted = true;
            fulfillment.reserve(order.getId(), product.sku(), request.quantity());
            paymentAttempted = true;
            fulfillment.authorize(order.getId(), customerId, order.getTotal());
            persistence.confirm(order);
            return order;
        } catch (RuntimeException failure) {
            if (paymentAttempted) {
                try {
                    fulfillment.refund(order.getId());
                } catch (RuntimeException compensationFailure) {
                    failure.addSuppressed(compensationFailure);
                    log.error("Payment compensation failed for order {}", order.getId(), compensationFailure);
                }
            }
            if (reservationAttempted) {
                try {
                    fulfillment.release(order.getId());
                } catch (RuntimeException compensationFailure) {
                    failure.addSuppressed(compensationFailure);
                    log.error("Inventory compensation failed for order {}", order.getId(), compensationFailure);
                }
            }
            persistence.fail(order);
            throw failure;
        }
    }

    @Transactional(readOnly = true)
    public List<Order> list(String customerId) { return orders.findByCustomerIdOrderByCreatedAtDesc(customerId); }

    @Transactional(readOnly = true)
    public Order get(UUID id, String customerId) {
        return orders.findByIdAndCustomerId(id, customerId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Order not found"));
    }

}
