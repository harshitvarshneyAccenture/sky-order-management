package com.sky.ordermanagement.inventory.domain;

import com.sky.ordermanagement.common.event.ReservationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.UUID;

/**
 * Outcome of processing one order. The primary key (orderId) doubles as the idempotency key:
 * a re-delivered OrderCreatedEvent finds the existing row and re-publishes the same outcome
 * instead of decrementing stock twice.
 */
@Entity
@Table(name = "reservations")
public class Reservation implements Persistable<UUID> {

    @Id
    @Column(name = "order_id")
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status;

    @Column(length = 1000)
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Transient
    private boolean isNew;

    protected Reservation() {
        // for JPA
    }

    private Reservation(UUID orderId, ReservationStatus status, String reason) {
        this.orderId = orderId;
        this.status = status;
        this.reason = reason;
        this.createdAt = Instant.now();
        this.isNew = true;
    }

    public static Reservation reserved(UUID orderId) {
        return new Reservation(orderId, ReservationStatus.RESERVED, null);
    }

    public static Reservation rejected(UUID orderId, String reason) {
        return new Reservation(orderId, ReservationStatus.REJECTED, reason);
    }

    /** Forces INSERT (not merge) so a concurrent duplicate fails fast on the PK instead of overwriting. */
    @Override
    public boolean isNew() {
        return isNew;
    }

    @Override
    public UUID getId() {
        return orderId;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
