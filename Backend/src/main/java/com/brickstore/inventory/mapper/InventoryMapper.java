package com.brickstore.inventory.mapper;

import com.brickstore.inventory.dto.InventoryLogResponse;
import com.brickstore.inventory.dto.InventoryResponse;
import com.brickstore.inventory.dto.StockReservationResponse;
import com.brickstore.inventory.entity.Inventory;
import com.brickstore.inventory.entity.InventoryLog;
import com.brickstore.inventory.entity.StockReservation;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        builder = @Builder(disableBuilder = true),
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface InventoryMapper {

    @Mapping(target = "variantId", source = "variant.id")
    @Mapping(target = "sku", source = "variant.sku")
    @Mapping(target = "productName", source = "variant.product.name")
    @Mapping(target = "variantName", source = "variant.name")
    @Mapping(target = "totalStock", expression = "java(inventory.getTotalStock())")
    InventoryResponse toResponse(Inventory inventory);

    List<InventoryResponse> toResponseList(List<Inventory> inventories);

    @Mapping(target = "reservationId", source = "id")
    @Mapping(target = "variantId", source = "variant.id")
    @Mapping(target = "sku", source = "variant.sku")
    StockReservationResponse toReservationResponse(StockReservation reservation);

    @Mapping(target = "variantId", source = "variant.id")
    @Mapping(target = "sku", source = "variant.sku")
    InventoryLogResponse toLogResponse(InventoryLog log);

    List<InventoryLogResponse> toLogResponseList(List<InventoryLog> logs);
}

