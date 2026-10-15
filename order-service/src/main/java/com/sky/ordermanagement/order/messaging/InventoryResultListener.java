package com.sky.ordermanagement.order.messaging;

import com.sky.ordermanagement.common.event.InventoryReservationResultEvent;
import com.sky.ordermanagement.common.kafka.KafkaTopics;
import com.sky.ordermanagement.order.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Thin adapter: Kafka concerns here, business logic in {@link OrderService}. */
@Component
public class InventoryResultListener {

    private static final Logger log = LoggerFactory.getLogger(InventoryResultListener.class);

    private final OrderService orderService;

    public InventoryResultListener(OrderService orderService) {
        this.orderService = orderService;
    }

    @KafkaListener(topics = KafkaTopics.INVENTORY_RESERVATION_RESULT)
    public void onReservationResult(InventoryReservationResultEvent event) {
        try (MDC.MDCCloseable ignored = MDC.putCloseable("orderId", String.valueOf(event.orderId()))) {
            log.info("Received reservation result status={} eventId={}", event.status(), event.eventId());
            orderService.applyReservationResult(event);
        }
    }
}
