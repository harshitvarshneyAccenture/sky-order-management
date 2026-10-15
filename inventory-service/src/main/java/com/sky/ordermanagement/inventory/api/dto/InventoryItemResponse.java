package com.sky.ordermanagement.inventory.api.dto;

import java.time.Instant;

public record InventoryItemResponse(String sku, String name, int availableQuantity, Instant updatedAt) {
}
