package com.sky.ordermanagement.common.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Published by Inventory Service after it has processed an {@link OrderCreatedEvent}.
 * {@code reason} is populated only when {@code status == REJECTED}.
 */
public record InventoryReservationResultEvent(
        UUID eventId,
        UUID orderId,
        ReservationStatus status,
        String reason,
        Instant occurredAt) {
}
