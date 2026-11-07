package com.brickstore.inventory.dto;

import com.brickstore.inventory.enums.InventoryActionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryLogResponse {

    private Long id;
    private Long variantId;
    private String sku;
    private Integer changeQuantity;
    private Integer previousStock;
    private Integer currentStock;
    private InventoryActionType actionType;
    private String referenceId;
    private String note;
    private LocalDateTime createdAt;
}

