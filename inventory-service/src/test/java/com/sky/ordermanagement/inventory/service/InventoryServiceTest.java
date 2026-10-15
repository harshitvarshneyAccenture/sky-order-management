package com.sky.ordermanagement.inventory.service;

import com.sky.ordermanagement.common.event.InventoryReservationResultEvent;
import com.sky.ordermanagement.common.event.OrderCreatedEvent;
import com.sky.ordermanagement.common.event.OrderLine;
import com.sky.ordermanagement.common.event.ReservationStatus;
import com.sky.ordermanagement.inventory.domain.InventoryItem;
import com.sky.ordermanagement.inventory.domain.Reservation;
import com.sky.ordermanagement.inventory.exception.InventoryItemNotFoundException;
import com.sky.ordermanagement.inventory.repository.InventoryItemRepository;
import com.sky.ordermanagement.inventory.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InventoryServiceTest {

    @Mock
    private InventoryItemRepository inventoryRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @InjectMocks
    private InventoryService inventoryService;

    private InventoryItem glass;
    private InventoryItem puck;

    @BeforeEach
    void setUp() {
        glass = new InventoryItem("SKY-GLASS-55", "Sky Glass", 10);
        puck = new InventoryItem("SKY-STREAM-PUCK", "Sky Stream", 1);
        when(reservationRepository.findById(any())).thenReturn(Optional.empty());
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));
        when(inventoryRepository.findAllForUpdate(anyCollection())).thenAnswer(inv -> {
            Collection<String> skus = inv.getArgument(0);
            return List.of(glass, puck).stream().filter(i -> skus.contains(i.getSku())).toList();
        });
    }

    @Test
    void reservesAllLinesWhenStockIsSufficient() {
        InventoryReservationResultEvent result = inventoryService.reserveStock(
                order(new OrderLine("SKY-GLASS-55", 3), new OrderLine("SKY-STREAM-PUCK", 1)));

        assertThat(result.status()).isEqualTo(ReservationStatus.RESERVED);
        assertThat(result.reason()).isNull();
        assertThat(glass.getAvailableQuantity()).isEqualTo(7);
        assertThat(puck.getAvailableQuantity()).isZero();
    }

    @Test
    void rejectsWholeOrderWhenOneLineIsShort_andDoesNotTouchOtherStock() {
        InventoryReservationResultEvent result = inventoryService.reserveStock(
                order(new OrderLine("SKY-GLASS-55", 3), new OrderLine("SKY-STREAM-PUCK", 2)));

        assertThat(result.status()).isEqualTo(ReservationStatus.REJECTED);
        assertThat(result.reason()).contains("Insufficient stock for SKY-STREAM-PUCK");
        assertThat(glass.getAvailableQuantity()).isEqualTo(10);   // all-or-nothing
        assertThat(puck.getAvailableQuantity()).isEqualTo(1);
    }

    @Test
    void rejectsUnknownSku() {
        InventoryReservationResultEvent result = inventoryService.reserveStock(order(new OrderLine("NOPE-1", 1)));

        assertThat(result.status()).isEqualTo(ReservationStatus.REJECTED);
        assertThat(result.reason()).contains("Unknown SKU NOPE-1");
    }

    @Test
    void rejectsOrderWithNoItems() {
        InventoryReservationResultEvent result = inventoryService.reserveStock(order());

        assertThat(result.status()).isEqualTo(ReservationStatus.REJECTED);
        assertThat(result.reason()).isEqualTo("Order has no items");
    }

    @Test
    void rejectsNonPositiveQuantity() {
        InventoryReservationResultEvent result = inventoryService.reserveStock(order(new OrderLine("SKY-GLASS-55", 0)));

        assertThat(result.status()).isEqualTo(ReservationStatus.REJECTED);
        assertThat(glass.getAvailableQuantity()).isEqualTo(10);
    }

    @Test
    void aggregatesRepeatedSkuLinesBeforeChecking() {
        InventoryReservationResultEvent result = inventoryService.reserveStock(
                order(new OrderLine("SKY-STREAM-PUCK", 1), new OrderLine("SKY-STREAM-PUCK", 1)));

        // 1 + 1 = 2 > 1 available: must be rejected, not reserved line-by-line
        assertThat(result.status()).isEqualTo(ReservationStatus.REJECTED);
        assertThat(puck.getAvailableQuantity()).isEqualTo(1);
    }

    @Test
    void persistsOutcomeAsIdempotencyRecord() {
        OrderCreatedEvent event = order(new OrderLine("SKY-GLASS-55", 1));

        inventoryService.reserveStock(event);

        ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationRepository).save(captor.capture());
        assertThat(captor.getValue().getOrderId()).isEqualTo(event.orderId());
        assertThat(captor.getValue().getStatus()).isEqualTo(ReservationStatus.RESERVED);
    }

    @Test
    void duplicateEventReturnsStoredOutcomeWithoutReservingAgain() {
        OrderCreatedEvent event = order(new OrderLine("SKY-GLASS-55", 3));
        when(reservationRepository.findById(event.orderId()))
                .thenReturn(Optional.of(Reservation.rejected(event.orderId(), "previous outcome")));

        InventoryReservationResultEvent result = inventoryService.reserveStock(event);

        assertThat(result.status()).isEqualTo(ReservationStatus.REJECTED);
        assertThat(result.reason()).isEqualTo("previous outcome");
        assertThat(glass.getAvailableQuantity()).isEqualTo(10);
        verify(inventoryRepository, never()).findAllForUpdate(anyCollection());
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void restockUnknownSkuThrows() {
        when(inventoryRepository.findById("NOPE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.restock("NOPE", 5))
                .isInstanceOf(InventoryItemNotFoundException.class);
    }

    private static OrderCreatedEvent order(OrderLine... lines) {
        return new OrderCreatedEvent(UUID.randomUUID(), UUID.randomUUID(), "CUST-1", List.of(lines), Instant.now());
    }
}
