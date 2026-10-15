package com.sky.ordermanagement.order.service;

import com.sky.ordermanagement.common.event.OrderCreatedEvent;
import com.sky.ordermanagement.common.event.OrderLine;
import com.sky.ordermanagement.order.api.dto.OrderItemResponse;
import com.sky.ordermanagement.order.api.dto.OrderResponse;
import com.sky.ordermanagement.order.domain.Order;
import com.sky.ordermanagement.order.domain.OrderItem;

import java.time.Instant;
import java.util.UUID;

/** Pure mapping between the domain model, API DTOs and Kafka event contracts. */
public final class OrderMapper {

    private OrderMapper() {
    }

    public static OrderResponse toResponse(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getCustomerId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getRejectionReason(),
                order.getItems().stream().map(OrderMapper::toItemResponse).toList(),
                order.getCreatedAt(),
                order.getUpdatedAt());
    }

    public static OrderCreatedEvent toOrderCreatedEvent(Order order) {
        return new OrderCreatedEvent(
                UUID.randomUUID(),
                order.getId(),
                order.getCustomerId(),
                order.getItems().stream().map(i -> new OrderLine(i.getSku(), i.getQuantity())).toList(),
                Instant.now());
    }

    private static OrderItemResponse toItemResponse(OrderItem item) {
        return new OrderItemResponse(item.getSku(), item.getQuantity(), item.getUnitPrice(), item.lineTotal());
    }
}
