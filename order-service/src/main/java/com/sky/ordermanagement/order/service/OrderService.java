package com.sky.ordermanagement.order.service;

import com.sky.ordermanagement.common.event.InventoryReservationResultEvent;
import com.sky.ordermanagement.order.api.dto.CreateOrderRequest;
import com.sky.ordermanagement.order.api.dto.OrderResponse;
import com.sky.ordermanagement.order.api.dto.PageResponse;
import com.sky.ordermanagement.order.domain.Order;
import com.sky.ordermanagement.order.domain.OrderItem;
import com.sky.ordermanagement.order.exception.OrderNotFoundException;
import com.sky.ordermanagement.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher eventPublisher;

    public OrderService(OrderRepository orderRepository, ApplicationEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Persists the order as PENDING and raises an {@code OrderCreatedEvent}. The event is only sent to Kafka
     * once the DB transaction commits (see {@code OrderEventPublisher}), so consumers never see an order
     * that was rolled back.
     */
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        List<OrderItem> items = request.items().stream()
                .map(i -> new OrderItem(i.sku(), i.quantity(), i.unitPrice()))
                .toList();

        Order order = orderRepository.save(Order.create(request.customerId(), items));
        eventPublisher.publishEvent(OrderMapper.toOrderCreatedEvent(order));

        log.info("Order created orderId={} customerId={} items={} total={}",
                order.getId(), order.getCustomerId(), items.size(), order.getTotalAmount());
        return OrderMapper.toResponse(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID orderId) {
        return orderRepository.findById(orderId)
                .map(OrderMapper::toResponse)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> listOrders(String customerId, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return PageResponse.from(orderRepository.findByCustomerId(customerId, pageRequest), OrderMapper::toResponse);
    }

    /**
     * Applies the outcome of the inventory reservation. Idempotent: Kafka delivers at-least-once, so a
     * re-delivered result for an order that is no longer PENDING is logged and ignored.
     */
    @Transactional
    public void applyReservationResult(InventoryReservationResultEvent event) {
        Order order = orderRepository.findById(event.orderId())
                .orElseThrow(() -> new OrderNotFoundException(event.orderId()));

        if (!order.isPending()) {
            log.warn("Ignoring reservation result for orderId={} - already {}", order.getId(), order.getStatus());
            return;
        }

        switch (event.status()) {
            case RESERVED -> order.confirm();
            case REJECTED -> order.reject(event.reason());
        }
        log.info("Order {} -> {} {}", order.getId(), order.getStatus(),
                order.getRejectionReason() == null ? "" : "(" + order.getRejectionReason() + ")");
    }
}
