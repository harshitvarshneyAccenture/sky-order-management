package com.sky.ordermanagement.order.api;

import com.sky.ordermanagement.order.api.dto.CreateOrderRequest;
import com.sky.ordermanagement.order.api.dto.OrderItemResponse;
import com.sky.ordermanagement.order.api.dto.OrderResponse;
import com.sky.ordermanagement.order.domain.OrderStatus;
import com.sky.ordermanagement.order.exception.InvalidOrderException;
import com.sky.ordermanagement.order.exception.OrderNotFoundException;
import com.sky.ordermanagement.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    private static final String VALID_BODY = """
            {"customerId":"CUST-1","items":[{"sku":"SKY-GLASS-55","quantity":1,"unitPrice":649.00}]}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @Test
    void createReturns201WithLocation() throws Exception {
        UUID id = UUID.randomUUID();
        when(orderService.createOrder(any(CreateOrderRequest.class))).thenReturn(response(id));

        mockMvc.perform(post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/v1/orders/" + id)))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void createWithNoItemsReturns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"\",\"items\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors.customerId").exists())
                .andExpect(jsonPath("$.errors.items").exists());
        verifyNoInteractions(orderService);
    }

    @Test
    void createWithInvalidItemReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"C1\",\"items\":[{\"sku\":\"bad sku\",\"quantity\":0,\"unitPrice\":-1}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors['items[0].sku']").exists())
                .andExpect(jsonPath("$.errors['items[0].quantity']").exists())
                .andExpect(jsonPath("$.errors['items[0].unitPrice']").exists());
    }

    @Test
    void malformedJsonReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON).content("{not-json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void businessRuleViolationReturns400() throws Exception {
        when(orderService.createOrder(any())).thenThrow(new InvalidOrderException("Duplicate SKU in order: X"));

        mockMvc.perform(post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Duplicate SKU in order: X"));
    }

    @Test
    void getUnknownOrderReturns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(orderService.getOrder(id)).thenThrow(new OrderNotFoundException(id));

        mockMvc.perform(get("/api/v1/orders/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Order not found"));
    }

    @Test
    void getWithInvalidUuidReturns400() throws Exception {
        mockMvc.perform(get("/api/v1/orders/not-a-uuid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listWithoutCustomerIdReturns400() throws Exception {
        mockMvc.perform(get("/api/v1/orders"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unexpectedErrorReturns500WithoutLeakingDetails() throws Exception {
        UUID id = UUID.randomUUID();
        when(orderService.getOrder(id)).thenThrow(new IllegalStateException("db password is hunter2"));

        mockMvc.perform(get("/api/v1/orders/{id}", id))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred"));
    }

    private static OrderResponse response(UUID id) {
        return new OrderResponse(id, "CUST-1", OrderStatus.PENDING, new BigDecimal("649.00"), null,
                List.of(new OrderItemResponse("SKY-GLASS-55", 1, new BigDecimal("649.00"), new BigDecimal("649.00"))),
                Instant.now(), Instant.now());
    }
}
