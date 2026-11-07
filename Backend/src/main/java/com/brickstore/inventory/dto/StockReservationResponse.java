package com.brickstore.inventory.dto;

import com.brickstore.inventory.enums.ReservationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockReservationResponse {

    private UUID reservationId;
    private Long variantId;
    private String sku;
    private Integer quantity;
    private String orderReference;
    private ReservationStatus status;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
}

