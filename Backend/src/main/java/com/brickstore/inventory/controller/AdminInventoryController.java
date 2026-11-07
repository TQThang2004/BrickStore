package com.brickstore.inventory.controller;

import com.brickstore.common.response.ApiResponse;
import com.brickstore.common.response.PageResponse;
import com.brickstore.inventory.dto.InventoryLogResponse;
import com.brickstore.inventory.dto.InventoryResponse;
import com.brickstore.inventory.dto.StockAdjustmentRequest;
import com.brickstore.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/inventory")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Inventory - Admin", description = "Admin Inventory Management APIs")
public class AdminInventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    @Operation(summary = "Get list of all variant inventories with pagination")
    public ResponseEntity<ApiResponse<PageResponse<InventoryResponse>>> getAllInventory(
            @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<InventoryResponse> response = inventoryService.getAllInventory(pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{variantId}")
    @Operation(summary = "Get inventory details for a specific variant")
    public ResponseEntity<ApiResponse<InventoryResponse>> getInventoryByVariantId(@PathVariable Long variantId) {
        InventoryResponse response = inventoryService.getInventoryByVariantId(variantId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{variantId}/adjust")
    @Operation(summary = "Adjust or restock inventory for a specific variant")
    public ResponseEntity<ApiResponse<InventoryResponse>> adjustStock(
            @PathVariable Long variantId,
            @Valid @RequestBody StockAdjustmentRequest request
    ) {
        InventoryResponse response = inventoryService.adjustStock(variantId, request);
        return ResponseEntity.ok(ApiResponse.success("Điều chỉnh tồn kho thành công", response));
    }

    @GetMapping("/{variantId}/logs")
    @Operation(summary = "Get inventory history logs for a specific variant")
    public ResponseEntity<ApiResponse<PageResponse<InventoryLogResponse>>> getInventoryLogs(
            @PathVariable Long variantId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<InventoryLogResponse> response = inventoryService.getInventoryLogs(variantId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

