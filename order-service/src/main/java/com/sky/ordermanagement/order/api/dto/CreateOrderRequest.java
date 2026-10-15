package com.sky.ordermanagement.order.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateOrderRequest(
        @NotBlank
        @Size(max = 64)
        String customerId,

        @NotEmpty
        @Size(max = 50)
        List<@NotNull @Valid OrderItemRequest> items) {
}
