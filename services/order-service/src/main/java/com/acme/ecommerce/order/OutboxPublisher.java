package com.acme.ecommerce.order;

import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OutboxPublisher {
    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);
    private final OutboxRepository outbox;
    private final KafkaTemplate<String, String> kafka;

    public OutboxPublisher(OutboxRepository outbox, KafkaTemplate<String, String> kafka) {
        this.outbox = outbox;
        this.kafka = kafka;
    }

    @Scheduled(fixedDelayString = "${outbox.publish-delay-ms:1000}")
    public void publishPending() {
        for (OutboxMessage message : outbox.findTop100ByOrderByCreatedAtAsc()) {
            try {
                kafka.send("orders.events", message.getId().toString(), message.getPayload()).get();
                outbox.deleteById(message.getId());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Outbox publish interrupted for {}; it will be retried", message.getId(), e);
                return;
            } catch (ExecutionException | RuntimeException e) {
                log.warn("Outbox publish failed for {}; it will be retried", message.getId(), e);
            }
        }
    }
}
