package com.sky.ordermanagement.common.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Published by Order Service when a new order has been persisted in PENDING state.
 * Kafka message key = orderId, so all events for one order land on the same partition (ordering).
 */
public record OrderCreatedEvent(
        UUID eventId,
        UUID orderId,
        String customerId,
        List<OrderLine> items,
        Instant occurredAt) {
}
