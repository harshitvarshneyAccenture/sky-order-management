package com.sky.ordermanagement.inventory.exception;

public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(String sku, int requested, int available) {
        super("Insufficient stock for %s (requested %d, available %d)".formatted(sku, requested, available));
    }
}
