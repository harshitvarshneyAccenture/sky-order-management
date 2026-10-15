package com.sky.ordermanagement.order.exception;

import com.sky.ordermanagement.order.domain.OrderStatus;

import java.util.UUID;

/** Illegal state transition, e.g. confirming an already rejected order (maps to HTTP 409). */
public class InvalidOrderStateException extends RuntimeException {
    public InvalidOrderStateException(UUID orderId, OrderStatus current, OrderStatus target) {
        super("Order %s cannot transition from %s to %s".formatted(orderId, current, target));
    }
}
