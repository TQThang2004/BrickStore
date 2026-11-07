package com.brickstore.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryResponse {

    private Long id;
    private Long variantId;
    private String sku;
    private String productName;
    private String variantName;
    private Integer availableStock;
    private Integer reservedStock;
    private Integer totalStock;
    private LocalDateTime updatedAt;
}

