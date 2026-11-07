package com.brickstore.inventory.controller;

import com.brickstore.common.response.ApiResponse;
import com.brickstore.inventory.dto.StockReservationRequest;
import com.brickstore.inventory.dto.StockReservationResponse;
import com.brickstore.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
@Tag(name = "Inventory - Public", description = "Public / Customer Inventory APIs")
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/{variantId}/check")
    @Operation(summary = "Check if variant has enough stock available")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkStock(
            @PathVariable Long variantId,
            @RequestParam(defaultValue = "1") int quantity
    ) {
        boolean available = inventoryService.checkStock(variantId, quantity);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "variantId", variantId,
                "quantity", quantity,
                "available", available
        )));
    }

    @PostMapping("/reserve")
    @Operation(summary = "Reserve stock temporarily for order checkout (Internal/Public)")
    public ResponseEntity<ApiResponse<StockReservationResponse>> reserveStock(
            @Valid @RequestBody StockReservationRequest request
    ) {
        StockReservationResponse response = inventoryService.reserveStock(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Giữ hàng thành công", response));
    }
}

