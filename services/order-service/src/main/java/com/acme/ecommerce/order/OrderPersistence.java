package com.acme.ecommerce.order;

import com.acme.ecommerce.common.OrderEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderPersistence {
    private final OrderRepository orders;
    private final OutboxRepository outbox;
    private final ObjectMapper objectMapper;

    public OrderPersistence(OrderRepository orders, OutboxRepository outbox, ObjectMapper objectMapper) {
        this.orders = orders;
        this.outbox = outbox;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Order createPending(String customerId, FulfillmentClient.ProductView product, int quantity) {
        return orders.save(new Order(customerId, product.id(), product.sku(), quantity,
                product.price().multiply(java.math.BigDecimal.valueOf(quantity))));
    }

    @Transactional
    public Order confirm(Order order) {
        order.confirm();
        Order saved = orders.save(order);
        enqueue(saved, "OrderConfirmed");
        return saved;
    }

    @Transactional
    public Order fail(Order order) {
        order.fail();
        Order saved = orders.save(order);
        enqueue(saved, "OrderFailed");
        return saved;
    }

    private void enqueue(Order order, String type) {
        try {
            OrderEvent event = new OrderEvent(UUID.randomUUID(), order.getId(), order.getCustomerId(),
                    type, order.getTotal(), order.getStatus(), Instant.now());
            outbox.save(new OutboxMessage(event.eventId(), objectMapper.writeValueAsString(event)));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize order event", e);
        }
    }
}
