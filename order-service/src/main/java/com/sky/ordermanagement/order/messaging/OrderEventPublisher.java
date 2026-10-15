package com.sky.ordermanagement.order.messaging;

import com.sky.ordermanagement.common.event.OrderCreatedEvent;
import com.sky.ordermanagement.common.kafka.KafkaTopics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Bridges the in-process domain event to Kafka strictly AFTER the DB commit.
 * Trade-off: if Kafka is down at this moment the event is lost (order stays PENDING).
 * A transactional outbox would close this gap - see README "Known limitations".
 */
@Component
public class OrderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public OrderEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderCreated(OrderCreatedEvent event) {
        String key = event.orderId().toString();
        kafkaTemplate.send(KafkaTopics.ORDER_CREATED, key, event).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish OrderCreatedEvent orderId={} eventId={}", key, event.eventId(), ex);
            } else {
                log.info("Published OrderCreatedEvent orderId={} partition={} offset={}", key,
                        result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
            }
        });
    }
}
