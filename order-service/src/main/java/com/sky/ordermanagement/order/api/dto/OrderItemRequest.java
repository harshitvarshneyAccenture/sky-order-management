package com.sky.ordermanagement.order.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record OrderItemRequest(
        @NotBlank
        @Pattern(regexp = "^[A-Z0-9-]{3,32}$", message = "must be 3-32 chars: upper-case letters, digits or '-'")
        String sku,

        @NotNull
        @Min(1)
        @Max(1000)
        Integer quantity,

        @NotNull
        @DecimalMin(value = "0.01")
        @Digits(integer = 17, fraction = 2)
        BigDecimal unitPrice) {
}
