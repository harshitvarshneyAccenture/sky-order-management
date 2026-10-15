package com.sky.ordermanagement.common.event;

/**
 * A single line of an order as seen by downstream consumers (no pricing - Inventory does not need it).
 */
public record OrderLine(String sku, int quantity) {
}
