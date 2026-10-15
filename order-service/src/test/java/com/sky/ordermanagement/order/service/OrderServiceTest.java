package com.sky.ordermanagement.order.service;

import com.sky.ordermanagement.common.event.InventoryReservationResultEvent;
import com.sky.ordermanagement.common.event.OrderCreatedEvent;
import com.sky.ordermanagement.common.event.OrderLine;
import com.sky.ordermanagement.common.event.ReservationStatus;
import com.sky.ordermanagement.order.api.dto.CreateOrderRequest;
import com.sky.ordermanagement.order.api.dto.OrderItemRequest;
import com.sky.ordermanagement.order.api.dto.OrderResponse;
import com.sky.ordermanagement.order.domain.Order;
import com.sky.ordermanagement.order.domain.OrderItem;
import com.sky.ordermanagement.order.domain.OrderStatus;
import com.sky.ordermanagement.order.exception.OrderNotFoundException;
import com.sky.ordermanagement.order.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private OrderService orderService;

    @Test
    void createOrderPersistsPendingOrderAndRaisesEvent() {
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        CreateOrderRequest request = new CreateOrderRequest("CUST-1", List.of(
                new OrderItemRequest("SKY-GLASS-55", 2, new BigDecimal("649.00"))));

        OrderResponse response = orderService.createOrder(request);

        assertThat(response.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(response.totalAmount()).isEqualByComparingTo("1298.00");

        ArgumentCaptor<OrderCreatedEvent> captor = ArgumentCaptor.forClass(OrderCreatedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        OrderCreatedEvent event = captor.getValue();
        assertThat(event.orderId()).isEqualTo(response.id());
        assertThat(event.customerId()).isEqualTo("CUST-1");
        assertThat(event.items()).containsExactly(new OrderLine("SKY-GLASS-55", 2));
    }

    @Test
    void getOrderThrowsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(orderRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrder(id))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    @Test
    void reservedResultConfirmsOrder() {
        Order order = pendingOrder();
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        orderService.applyReservationResult(result(order.getId(), ReservationStatus.RESERVED, null));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void rejectedResultRejectsOrderWithReason() {
        Order order = pendingOrder();
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        orderService.applyReservationResult(result(order.getId(), ReservationStatus.REJECTED, "Insufficient stock"));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.REJECTED);
        assertThat(order.getRejectionReason()).isEqualTo("Insufficient stock");
    }

    @Test
    void duplicateResultIsIgnored() {
        Order order = pendingOrder();
        order.confirm();
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        orderService.applyReservationResult(result(order.getId(), ReservationStatus.REJECTED, "late duplicate"));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(order.getRejectionReason()).isNull();
    }

    @Test
    void resultForUnknownOrderThrows() {
        UUID id = UUID.randomUUID();
        when(orderRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.applyReservationResult(result(id, ReservationStatus.RESERVED, null)))
                .isInstanceOf(OrderNotFoundException.class);
        verifyNoInteractions(eventPublisher);
    }

    private static Order pendingOrder() {
        return Order.create("CUST-1", List.of(new OrderItem("SKU-1", 1, BigDecimal.TEN)));
    }

    private static InventoryReservationResultEvent result(UUID orderId, ReservationStatus status, String reason) {
        return new InventoryReservationResultEvent(UUID.randomUUID(), orderId, status, reason, Instant.now());
    }
}
