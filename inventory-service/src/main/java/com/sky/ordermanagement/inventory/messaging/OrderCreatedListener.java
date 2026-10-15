package com.sky.ordermanagement.inventory.messaging;

import com.sky.ordermanagement.common.event.InventoryReservationResultEvent;
import com.sky.ordermanagement.common.event.OrderCreatedEvent;
import com.sky.ordermanagement.common.kafka.KafkaTopics;
import com.sky.ordermanagement.inventory.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consume -> reserve (DB tx commits) -> publish result. Publishing happens after the commit, outside the
 * DB transaction, so we never announce a reservation that was rolled back.
 */
@Component
public class OrderCreatedListener {

    private static final Logger log = LoggerFactory.getLogger(OrderCreatedListener.class);

    private final InventoryService inventoryService;
    private final InventoryEventPublisher publisher;

    public OrderCreatedListener(InventoryService inventoryService, InventoryEventPublisher publisher) {
        this.inventoryService = inventoryService;
        this.publisher = publisher;
    }

    @KafkaListener(topics = KafkaTopics.ORDER_CREATED)
    public void onOrderCreated(OrderCreatedEvent event) {
        try (MDC.MDCCloseable ignored = MDC.putCloseable("orderId", String.valueOf(event.orderId()))) {
            log.info("Received OrderCreatedEvent eventId={} lines={}", event.eventId(),
                    event.items() == null ? 0 : event.items().size());
            InventoryReservationResultEvent result = inventoryService.reserveStock(event);
            publisher.publish(result);
        }
    }
}
