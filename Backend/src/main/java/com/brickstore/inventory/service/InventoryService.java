package com.brickstore.inventory.service;

import com.brickstore.common.response.PageResponse;
import com.brickstore.inventory.dto.*;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface InventoryService {

    InventoryResponse getInventoryByVariantId(Long variantId);

    PageResponse<InventoryResponse> getAllInventory(Pageable pageable);

    boolean checkStock(Long variantId, int quantity);

    InventoryResponse adjustStock(Long variantId, StockAdjustmentRequest request);

    StockReservationResponse reserveStock(StockReservationRequest request);

    void commitReservation(UUID reservationId);

    void releaseReservation(UUID reservationId);

    int releaseExpiredReservations();

    PageResponse<InventoryLogResponse> getInventoryLogs(Long variantId, Pageable pageable);
}

