package com.sky.ordermanagement.inventory.messaging;

import com.sky.ordermanagement.common.event.InventoryReservationResultEvent;
import com.sky.ordermanagement.common.kafka.KafkaTopics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class InventoryEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(InventoryEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final long sendTimeoutMs;

    public InventoryEventPublisher(KafkaTemplate<String, Object> kafkaTemplate,
                                   @Value("${sky.oms.kafka.send-timeout-ms:10000}") long sendTimeoutMs) {
        this.kafkaTemplate = kafkaTemplate;
        this.sendTimeoutMs = sendTimeoutMs;
    }

    /**
     * Synchronous send (cheap on a virtual thread): a failure propagates to the listener, so the consumed
     * record is NOT committed and is redelivered. Because reservation is idempotent, the redelivery simply
     * re-publishes the stored outcome.
     */
    public void publish(InventoryReservationResultEvent event) {
        String key = event.orderId().toString();
        try {
            SendResult<String, Object> result = kafkaTemplate
                    .send(KafkaTopics.INVENTORY_RESERVATION_RESULT, key, event)
                    .get(sendTimeoutMs, TimeUnit.MILLISECONDS);
            log.info("Published reservation result status={} partition={} offset={}", event.status(),
                    result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while publishing result for order " + key, e);
        } catch (ExecutionException | TimeoutException e) {
            throw new IllegalStateException("Failed to publish result for order " + key, e);
        }
    }
}
