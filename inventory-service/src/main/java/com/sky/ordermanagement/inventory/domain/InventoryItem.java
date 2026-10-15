package com.sky.ordermanagement.inventory.domain;

import com.sky.ordermanagement.inventory.exception.InsufficientStockException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;

@Entity
@Table(name = "inventory_items")
public class InventoryItem {

    @Id
    @Column(length = 32)
    private String sku;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(name = "available_quantity", nullable = false)
    private int availableQuantity;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    protected InventoryItem() {
        // for JPA
    }

    public InventoryItem(String sku, String name, int availableQuantity) {
        if (availableQuantity < 0) {
            throw new IllegalArgumentException("Available quantity cannot be negative");
        }
        this.sku = sku;
        this.name = name;
        this.availableQuantity = availableQuantity;
        this.updatedAt = Instant.now();
    }

    public boolean canReserve(int quantity) {
        return quantity > 0 && availableQuantity >= quantity;
    }

    public void reserve(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity to reserve must be positive");
        }
        if (availableQuantity < quantity) {
            throw new InsufficientStockException(sku, quantity, availableQuantity);
        }
        availableQuantity -= quantity;
        updatedAt = Instant.now();
    }

    public void restock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity to restock must be positive");
        }
        availableQuantity = Math.addExact(availableQuantity, quantity);
        updatedAt = Instant.now();
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
