package com.sky.ordermanagement.inventory.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RestockRequest(@NotNull @Min(1) @Max(100_000) Integer quantity) {
}
