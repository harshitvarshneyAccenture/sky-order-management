package com.sky.ordermanagement.inventory.service;

import com.sky.ordermanagement.common.event.InventoryReservationResultEvent;
import com.sky.ordermanagement.common.event.OrderCreatedEvent;
import com.sky.ordermanagement.common.event.OrderLine;
import com.sky.ordermanagement.inventory.api.dto.InventoryItemResponse;
import com.sky.ordermanagement.inventory.domain.InventoryItem;
import com.sky.ordermanagement.inventory.domain.Reservation;
import com.sky.ordermanagement.inventory.exception.InventoryItemNotFoundException;
import com.sky.ordermanagement.inventory.repository.InventoryItemRepository;
import com.sky.ordermanagement.inventory.repository.ReservationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    private final InventoryItemRepository inventoryRepository;
    private final ReservationRepository reservationRepository;

    public InventoryService(InventoryItemRepository inventoryRepository, ReservationRepository reservationRepository) {
        this.inventoryRepository = inventoryRepository;
        this.reservationRepository = reservationRepository;
    }

    /**
     * Reserves stock for every line of the order - all or nothing. Runs in a single DB transaction:
     * stock decrements and the idempotency record commit together.
     *
     * @return the result event to publish (also returned for duplicates, so a lost publish can be retried)
     */
    @Transactional
    public InventoryReservationResultEvent reserveStock(OrderCreatedEvent event) {
        Optional<Reservation> existing = reservationRepository.findById(event.orderId());
        if (existing.isPresent()) {
            log.info("Duplicate OrderCreatedEvent for orderId={} - returning stored outcome {}",
                    event.orderId(), existing.get().getStatus());
            return toEvent(existing.get());
        }

        Reservation reservation = reservationRepository.save(tryReserve(event));
        log.info("Reservation for orderId={} -> {}{}", event.orderId(), reservation.getStatus(),
                reservation.getReason() == null ? "" : " (" + reservation.getReason() + ")");
        return toEvent(reservation);
    }

    @Transactional(readOnly = true)
    public InventoryItemResponse getItem(String sku) {
        return inventoryRepository.findById(sku)
                .map(InventoryService::toResponse)
                .orElseThrow(() -> new InventoryItemNotFoundException(sku));
    }

    @Transactional(readOnly = true)
    public List<InventoryItemResponse> listItems() {
        return inventoryRepository.findAll(Sort.by("sku")).stream().map(InventoryService::toResponse).toList();
    }

    @Transactional
    public InventoryItemResponse restock(String sku, int quantity) {
        InventoryItem item = inventoryRepository.findById(sku)
                .orElseThrow(() -> new InventoryItemNotFoundException(sku));
        item.restock(quantity);
        log.info("Restocked sku={} by {} -> available={}", sku, quantity, item.getAvailableQuantity());
        return toResponse(item);
    }

    private Reservation tryReserve(OrderCreatedEvent event) {
        UUID orderId = event.orderId();
        if (event.items() == null || event.items().isEmpty()) {
            return Reservation.rejected(orderId, "Order has no items");
        }

        // Aggregate quantities per SKU (defensive: the contract does not forbid repeated SKUs). TreeMap = sorted.
        Map<String, Integer> requested = event.items().stream()
                .collect(Collectors.toMap(OrderLine::sku, OrderLine::quantity, Integer::sum, TreeMap::new));

        Map<String, InventoryItem> stock = inventoryRepository.findAllForUpdate(requested.keySet()).stream()
                .collect(Collectors.toMap(InventoryItem::getSku, Function.identity()));

        List<String> problems = new ArrayList<>();
        requested.forEach((sku, qty) -> {
            InventoryItem item = stock.get(sku);
            if (qty <= 0) {
                problems.add("Invalid quantity %d for %s".formatted(qty, sku));
            } else if (item == null) {
                problems.add("Unknown SKU " + sku);
            } else if (!item.canReserve(qty)) {
                problems.add("Insufficient stock for %s (requested %d, available %d)"
                        .formatted(sku, qty, item.getAvailableQuantity()));
            }
        });

        if (!problems.isEmpty()) {
            return Reservation.rejected(orderId, String.join("; ", problems));
        }

        requested.forEach((sku, qty) -> stock.get(sku).reserve(qty));
        return Reservation.reserved(orderId);
    }

    private static InventoryReservationResultEvent toEvent(Reservation reservation) {
        return new InventoryReservationResultEvent(UUID.randomUUID(), reservation.getOrderId(),
                reservation.getStatus(), reservation.getReason(), Instant.now());
    }

    private static InventoryItemResponse toResponse(InventoryItem item) {
        return new InventoryItemResponse(item.getSku(), item.getName(), item.getAvailableQuantity(), item.getUpdatedAt());
    }
}
