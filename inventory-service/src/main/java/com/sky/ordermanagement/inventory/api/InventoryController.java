package com.sky.ordermanagement.inventory.api;

import com.sky.ordermanagement.inventory.api.dto.InventoryItemResponse;
import com.sky.ordermanagement.inventory.api.dto.RestockRequest;
import com.sky.ordermanagement.inventory.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Read / restock endpoints - mainly to observe the effect of order events. Reservation happens via Kafka only. */
@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public List<InventoryItemResponse> listItems() {
        return inventoryService.listItems();
    }

    @GetMapping("/{sku}")
    public InventoryItemResponse getItem(@PathVariable String sku) {
        return inventoryService.getItem(sku);
    }

    @PostMapping("/{sku}/restock")
    public InventoryItemResponse restock(@PathVariable String sku, @Valid @RequestBody RestockRequest request) {
        return inventoryService.restock(sku, request.quantity());
    }
}
