package com.sky.ordermanagement.order.domain;

import com.sky.ordermanagement.order.exception.InvalidOrderException;
import com.sky.ordermanagement.order.exception.InvalidOrderStateException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Order aggregate root. All state changes go through intention-revealing methods
 * ({@link #create}, {@link #confirm}, {@link #reject}) so invariants live in one place.
 */
@Entity
@Table(name = "orders")
public class Order {

    static final int MAX_REASON_LENGTH = 255;

    @Id
    private UUID id;

    @Column(name = "customer_id", nullable = false, length = 64)
    private String customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "rejection_reason", length = MAX_REASON_LENGTH)
    private String rejectionReason;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @BatchSize(size = 50)
    private List<OrderItem> items = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    protected Order() {
        // for JPA
    }

    public static Order create(String customerId, List<OrderItem> items) {
        if (customerId == null || customerId.isBlank()) {
            throw new InvalidOrderException("Customer id must not be blank");
        }
        if (items == null || items.isEmpty()) {
            throw new InvalidOrderException("Order must contain at least one item");
        }
        Set<String> seen = new HashSet<>();
        for (OrderItem item : items) {
            if (!seen.add(item.getSku())) {
                throw new InvalidOrderException("Duplicate SKU in order: " + item.getSku()
                        + ". Combine quantities into a single line.");
            }
        }

        Order order = new Order();
        order.id = UUID.randomUUID();
        order.customerId = customerId;
        order.status = OrderStatus.PENDING;
        items.forEach(order::addItem);
        order.totalAmount = items.stream().map(OrderItem::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        order.createdAt = Instant.now();
        order.updatedAt = order.createdAt;
        return order;
    }

    public void confirm() {
        ensurePending(OrderStatus.CONFIRMED);
        this.status = OrderStatus.CONFIRMED;
        this.updatedAt = Instant.now();
    }

    public void reject(String reason) {
        ensurePending(OrderStatus.REJECTED);
        this.status = OrderStatus.REJECTED;
        this.rejectionReason = truncate(reason == null || reason.isBlank() ? "Rejected by inventory" : reason);
        this.updatedAt = Instant.now();
    }

    public boolean isPending() {
        return status == OrderStatus.PENDING;
    }

    private void addItem(OrderItem item) {
        item.attachTo(this);
        items.add(item);
    }

    private void ensurePending(OrderStatus target) {
        if (!isPending()) {
            throw new InvalidOrderStateException(id, status, target);
        }
    }

    private static String truncate(String value) {
        return value.length() <= MAX_REASON_LENGTH ? value : value.substring(0, MAX_REASON_LENGTH);
    }

    public UUID getId() {
        return id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
