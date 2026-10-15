package com.sky.ordermanagement.inventory.api;

import com.sky.ordermanagement.inventory.api.dto.InventoryItemResponse;
import com.sky.ordermanagement.inventory.exception.InventoryItemNotFoundException;
import com.sky.ordermanagement.inventory.service.InventoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InventoryController.class)
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InventoryService inventoryService;

    @Test
    void getExistingItemReturns200() throws Exception {
        when(inventoryService.getItem("SKY-GLASS-55"))
                .thenReturn(new InventoryItemResponse("SKY-GLASS-55", "Sky Glass", 10, Instant.now()));

        mockMvc.perform(get("/api/v1/inventory/SKY-GLASS-55"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableQuantity").value(10));
    }

    @Test
    void getUnknownItemReturns404() throws Exception {
        when(inventoryService.getItem("NOPE")).thenThrow(new InventoryItemNotFoundException("NOPE"));

        mockMvc.perform(get("/api/v1/inventory/NOPE"))
                .andExpect(status().isNotFound());
    }

    @Test
    void restockWithNonPositiveQuantityReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/inventory/SKY-GLASS-55/restock")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.quantity").exists());
        verifyNoInteractions(inventoryService);
    }
}
