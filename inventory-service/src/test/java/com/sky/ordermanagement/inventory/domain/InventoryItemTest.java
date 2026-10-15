package com.sky.ordermanagement.inventory.domain;

import com.sky.ordermanagement.inventory.exception.InsufficientStockException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InventoryItemTest {

    @Test
    void reserveDecrementsStock() {
        InventoryItem item = new InventoryItem("SKU-1", "Item", 5);

        item.reserve(5);

        assertThat(item.getAvailableQuantity()).isZero();
    }

    @Test
    void reserveMoreThanAvailableFailsAndLeavesStockUntouched() {
        InventoryItem item = new InventoryItem("SKU-1", "Item", 2);

        assertThatThrownBy(() -> item.reserve(3)).isInstanceOf(InsufficientStockException.class);
        assertThat(item.getAvailableQuantity()).isEqualTo(2);
    }

    @Test
    void canReserveRejectsNonPositiveQuantities() {
        InventoryItem item = new InventoryItem("SKU-1", "Item", 2);

        assertThat(item.canReserve(0)).isFalse();
        assertThat(item.canReserve(-1)).isFalse();
        assertThat(item.canReserve(2)).isTrue();
        assertThatThrownBy(() -> item.reserve(0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void restockIncreasesStockAndRejectsNonPositive() {
        InventoryItem item = new InventoryItem("SKU-1", "Item", 2);

        item.restock(3);

        assertThat(item.getAvailableQuantity()).isEqualTo(5);
        assertThatThrownBy(() -> item.restock(0)).isInstanceOf(IllegalArgumentException.class);
    }
}
