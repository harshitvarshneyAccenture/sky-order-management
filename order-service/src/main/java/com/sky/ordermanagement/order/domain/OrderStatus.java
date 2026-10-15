package com.sky.ordermanagement.order.domain;

public enum OrderStatus {
    /** Persisted, waiting for Inventory Service to reserve stock. */
    PENDING,
    /** Stock reserved successfully. */
    CONFIRMED,
    /** Stock could not be reserved (unknown SKU / insufficient stock). */
    REJECTED
}
