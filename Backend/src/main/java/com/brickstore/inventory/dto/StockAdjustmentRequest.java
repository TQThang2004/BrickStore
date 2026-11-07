package com.brickstore.inventory.dto;

import com.brickstore.inventory.enums.InventoryActionType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockAdjustmentRequest {

    @NotNull(message = "Số lượng thay đổi không được để trống")
    private Integer changeQuantity;

    @Builder.Default
    private InventoryActionType actionType = InventoryActionType.RESTOCK;

    private String note;
}

