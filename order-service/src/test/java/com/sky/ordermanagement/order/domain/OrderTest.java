package com.sky.ordermanagement.order.domain;

import com.sky.ordermanagement.order.exception.InvalidOrderException;
import com.sky.ordermanagement.order.exception.InvalidOrderStateException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {

    private static OrderItem item(String sku, int qty, String price) {
        return new OrderItem(sku, qty, new BigDecimal(price));
    }

    @Test
    void createsPendingOrderAndCalculatesTotal() {
        Order order = Order.create("CUST-1", List.of(item("SKY-GLASS-55", 2, "649.00"), item("SKY-STREAM-PUCK", 1, "39.99")));

        assertThat(order.getId()).isNotNull();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getTotalAmount()).isEqualByComparingTo("1337.99");
        assertThat(order.getItems()).hasSize(2);
        assertThat(order.getCreatedAt()).isNotNull();
    }

    @Test
    void rejectsEmptyOrder() {
        assertThatThrownBy(() -> Order.create("CUST-1", List.of()))
                .isInstanceOf(InvalidOrderException.class)
                .hasMessageContaining("at least one item");
    }

    @Test
    void rejectsBlankCustomer() {
        assertThatThrownBy(() -> Order.create(" ", List.of(item("SKU-1", 1, "1.00"))))
                .isInstanceOf(InvalidOrderException.class);
    }

    @Test
    void rejectsDuplicateSkuLines() {
        assertThatThrownBy(() -> Order.create("CUST-1", List.of(item("SKU-1", 1, "1.00"), item("SKU-1", 2, "1.00"))))
                .isInstanceOf(InvalidOrderException.class)
                .hasMessageContaining("Duplicate SKU");
    }

    @Test
    void rejectsNonPositiveQuantityOrPrice() {
        assertThatThrownBy(() -> item("SKU-1", 0, "1.00")).isInstanceOf(InvalidOrderException.class);
        assertThatThrownBy(() -> item("SKU-1", 1, "0.00")).isInstanceOf(InvalidOrderException.class);
    }

    @Test
    void confirmMovesPendingToConfirmed() {
        Order order = Order.create("CUST-1", List.of(item("SKU-1", 1, "1.00")));

        order.confirm();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(order.isPending()).isFalse();
    }

    @Test
    void rejectStoresReasonAndDefaultsWhenBlank() {
        Order order = Order.create("CUST-1", List.of(item("SKU-1", 1, "1.00")));

        order.reject("  ");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.REJECTED);
        assertThat(order.getRejectionReason()).isEqualTo("Rejected by inventory");
    }

    @Test
    void rejectTruncatesVeryLongReason() {
        Order order = Order.create("CUST-1", List.of(item("SKU-1", 1, "1.00")));

        order.reject("x".repeat(1000));

        assertThat(order.getRejectionReason()).hasSize(Order.MAX_REASON_LENGTH);
    }

    @Test
    void cannotTransitionTwice() {
        Order order = Order.create("CUST-1", List.of(item("SKU-1", 1, "1.00")));
        order.confirm();

        assertThatThrownBy(order::confirm).isInstanceOf(InvalidOrderStateException.class);
        assertThatThrownBy(() -> order.reject("late")).isInstanceOf(InvalidOrderStateException.class);
    }
}
