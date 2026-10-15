package com.sky.ordermanagement.order.api.dto;

import java.math.BigDecimal;

public record OrderItemResponse(String sku, int quantity, BigDecimal unitPrice, BigDecimal lineTotal) {
}
