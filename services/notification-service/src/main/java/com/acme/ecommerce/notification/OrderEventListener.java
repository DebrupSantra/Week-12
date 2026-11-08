package com.acme.ecommerce.notification;

import com.acme.ecommerce.common.OrderEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OrderEventListener {
    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);
    private final ObjectMapper objectMapper;
    private final ProcessedEventRepository processed;

    public OrderEventListener(ObjectMapper objectMapper, ProcessedEventRepository processed) {
        this.objectMapper = objectMapper;
        this.processed = processed;
    }

    @KafkaListener(topics = "orders.events", groupId = "notification-service")
    @Transactional
    public void onOrderEvent(String payload) throws Exception {
        OrderEvent event = objectMapper.readValue(payload, OrderEvent.class);
        if (processed.existsById(event.eventId())) return;
        processed.save(new ProcessedEvent(event.eventId(), event.orderId(), event.eventType()));
        log.info("Notification queued: type={}, orderId={}, customerId={}",
                event.eventType(), event.orderId(), event.customerId());
    }
}
