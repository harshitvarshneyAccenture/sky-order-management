package com.sky.ordermanagement.order.api.dto;

import com.sky.ordermanagement.order.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        String customerId,
        OrderStatus status,
        BigDecimal totalAmount,
        String rejectionReason,
        List<OrderItemResponse> items,
        Instant createdAt,
        Instant updatedAt) {
}
