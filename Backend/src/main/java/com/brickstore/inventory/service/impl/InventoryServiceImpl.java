package com.brickstore.inventory.service.impl;

import com.brickstore.catalog.entity.ProductVariant;
import com.brickstore.catalog.repository.ProductVariantRepository;
import com.brickstore.common.enums.ErrorCode;
import com.brickstore.common.exception.BusinessException;
import com.brickstore.common.exception.ResourceNotFoundException;
import com.brickstore.common.response.PageResponse;
import com.brickstore.inventory.dto.*;
import com.brickstore.inventory.entity.Inventory;
import com.brickstore.inventory.entity.InventoryLog;
import com.brickstore.inventory.entity.StockReservation;
import com.brickstore.inventory.enums.InventoryActionType;
import com.brickstore.inventory.enums.ReservationStatus;
import com.brickstore.inventory.mapper.InventoryMapper;
import com.brickstore.inventory.repository.InventoryLogRepository;
import com.brickstore.inventory.repository.InventoryRepository;
import com.brickstore.inventory.repository.StockReservationRepository;
import com.brickstore.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final StockReservationRepository stockReservationRepository;
    private final InventoryLogRepository inventoryLogRepository;
    private final ProductVariantRepository productVariantRepository;
    private final InventoryMapper inventoryMapper;

    @Override
    public InventoryResponse getInventoryByVariantId(Long variantId) {
        Inventory inventory = getOrInitInventory(variantId);
        return inventoryMapper.toResponse(inventory);
    }

    @Override
    public PageResponse<InventoryResponse> getAllInventory(Pageable pageable) {
        Page<Inventory> page = inventoryRepository.findAllWithVariant(pageable);
        return PageResponse.<InventoryResponse>builder()
                .content(inventoryMapper.toResponseList(page.getContent()))
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    @Override
    public boolean checkStock(Long variantId, int quantity) {
        if (quantity <= 0) {
            return false;
        }
        return inventoryRepository.findByVariantId(variantId)
                .map(inv -> inv.getAvailableStock() >= quantity)
                .orElse(false);
    }

    @Override
    @Transactional
    public InventoryResponse adjustStock(Long variantId, StockAdjustmentRequest request) {
        Inventory inventory = getOrInitInventoryWithLock(variantId);
        int previousStock = inventory.getAvailableStock();
        int newStock = previousStock + request.getChangeQuantity();

        if (newStock < 0) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK,
                    String.format("Tồn kho hiện tại (%d) không đủ để giảm %d sản phẩm", previousStock, Math.abs(request.getChangeQuantity())));
        }

        inventory.setAvailableStock(newStock);
        inventoryRepository.save(inventory);

        // Sync sang product_variants
        ProductVariant variant = inventory.getVariant();
        variant.setStockQuantity(newStock);
        productVariantRepository.save(variant);

        // Lưu log biến động kho
        InventoryLog inventoryLog = InventoryLog.builder()
                .variant(variant)
                .changeQuantity(request.getChangeQuantity())
                .previousStock(previousStock)
                .currentStock(newStock)
                .actionType(request.getActionType() != null ? request.getActionType() : InventoryActionType.MANUAL_ADJUST)
                .note(request.getNote())
                .build();
        inventoryLogRepository.save(inventoryLog);

        log.info("Adjusted stock for variant ID {}: {} -> {} (change: {})", variantId, previousStock, newStock, request.getChangeQuantity());
        return inventoryMapper.toResponse(inventory);
    }

    @Override
    @Transactional
    public StockReservationResponse reserveStock(StockReservationRequest request) {
        Inventory inventory = getOrInitInventoryWithLock(request.getVariantId());
        int available = inventory.getAvailableStock();
        int requested = request.getQuantity();

        if (available < requested) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK,
                    String.format("Không đủ hàng trong kho. Số lượng khả dụng: %d, yêu cầu: %d", available, requested));
        }

        int previousStock = available;
        int newAvailable = available - requested;
        int newReserved = inventory.getReservedStock() + requested;

        inventory.setAvailableStock(newAvailable);
        inventory.setReservedStock(newReserved);
        inventoryRepository.save(inventory);

        // Cập nhật stockQuantity trong ProductVariant
        ProductVariant variant = inventory.getVariant();
        variant.setStockQuantity(newAvailable);
        productVariantRepository.save(variant);

        int duration = request.getDurationMinutes() != null && request.getDurationMinutes() > 0 ? request.getDurationMinutes() : 15;
        StockReservation reservation = StockReservation.builder()
                .variant(variant)
                .quantity(requested)
                .orderReference(request.getOrderReference())
                .status(ReservationStatus.RESERVED)
                .expiresAt(LocalDateTime.now().plusMinutes(duration))
                .build();

        StockReservation savedReservation = stockReservationRepository.save(reservation);

        // Ghi log
        InventoryLog logEntry = InventoryLog.builder()
                .variant(variant)
                .changeQuantity(-requested)
                .previousStock(previousStock)
                .currentStock(newAvailable)
                .actionType(InventoryActionType.RESERVE)
                .referenceId(savedReservation.getId().toString())
                .note("Giữ hàng đặt đơn: " + (request.getOrderReference() != null ? request.getOrderReference() : "N/A"))
                .build();
        inventoryLogRepository.save(logEntry);

        log.info("Reserved {} units of variant ID {}. Reservation ID: {}", requested, variant.getId(), savedReservation.getId());
        return inventoryMapper.toReservationResponse(savedReservation);
    }

    @Override
    @Transactional
    public void commitReservation(UUID reservationId) {
        StockReservation reservation = stockReservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("StockReservation", "id", reservationId));

        if (reservation.getStatus() != ReservationStatus.RESERVED) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST,
                    "Chỉ có thể commit reservation ở trạng thái RESERVED. Trạng thái hiện tại: " + reservation.getStatus());
        }

        Inventory inventory = getOrInitInventoryWithLock(reservation.getVariant().getId());
        int currentReserved = inventory.getReservedStock();
        int quantity = reservation.getQuantity();

        inventory.setReservedStock(Math.max(0, currentReserved - quantity));
        inventoryRepository.save(inventory);

        reservation.setStatus(ReservationStatus.COMMITTED);
        stockReservationRepository.save(reservation);

        // Ghi log
        InventoryLog logEntry = InventoryLog.builder()
                .variant(reservation.getVariant())
                .changeQuantity(0)
                .previousStock(inventory.getAvailableStock())
                .currentStock(inventory.getAvailableStock())
                .actionType(InventoryActionType.COMMIT)
                .referenceId(reservationId.toString())
                .note("Xác nhận xuất kho thành công cho reservation: " + reservationId)
                .build();
        inventoryLogRepository.save(logEntry);

        log.info("Committed reservation ID: {} for variant ID: {}", reservationId, reservation.getVariant().getId());
    }

    @Override
    @Transactional
    public void releaseReservation(UUID reservationId) {
        StockReservation reservation = stockReservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("StockReservation", "id", reservationId));

        if (reservation.getStatus() != ReservationStatus.RESERVED) {
            log.warn("Reservation ID {} is already {}. Skipping release.", reservationId, reservation.getStatus());
            return;
        }

        Inventory inventory = getOrInitInventoryWithLock(reservation.getVariant().getId());
        int quantity = reservation.getQuantity();
        int previousAvailable = inventory.getAvailableStock();
        int newAvailable = previousAvailable + quantity;
        int newReserved = Math.max(0, inventory.getReservedStock() - quantity);

        inventory.setAvailableStock(newAvailable);
        inventory.setReservedStock(newReserved);
        inventoryRepository.save(inventory);

        // Hoàn lại stockQuantity trong ProductVariant
        ProductVariant variant = inventory.getVariant();
        variant.setStockQuantity(newAvailable);
        productVariantRepository.save(variant);

        reservation.setStatus(ReservationStatus.RELEASED);
        stockReservationRepository.save(reservation);

        // Ghi log
        InventoryLog logEntry = InventoryLog.builder()
                .variant(variant)
                .changeQuantity(quantity)
                .previousStock(previousAvailable)
                .currentStock(newAvailable)
                .actionType(InventoryActionType.RELEASE)
                .referenceId(reservationId.toString())
                .note("Giải phóng giữ chỗ tồn kho (hủy/hết hạn reservation)")
                .build();
        inventoryLogRepository.save(logEntry);

        log.info("Released reservation ID: {} (restored {} units to variant ID {})", reservationId, quantity, variant.getId());
    }

    @Override
    @Transactional
    public int releaseExpiredReservations() {
        List<StockReservation> expiredList = stockReservationRepository
                .findAllByStatusAndExpiresAtBefore(ReservationStatus.RESERVED, LocalDateTime.now());

        if (expiredList.isEmpty()) {
            return 0;
        }

        log.info("Found {} expired reservations to release", expiredList.size());
        for (StockReservation res : expiredList) {
            try {
                releaseReservation(res.getId());
            } catch (Exception e) {
                log.error("Error releasing expired reservation ID {}: {}", res.getId(), e.getMessage());
            }
        }
        return expiredList.size();
    }

    @Override
    public PageResponse<InventoryLogResponse> getInventoryLogs(Long variantId, Pageable pageable) {
        Page<InventoryLog> page = inventoryLogRepository.findByVariantIdOrderByCreatedAtDesc(variantId, pageable);
        return PageResponse.<InventoryLogResponse>builder()
                .content(inventoryMapper.toLogResponseList(page.getContent()))
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    private Inventory getOrInitInventory(Long variantId) {
        return inventoryRepository.findByVariantId(variantId)
                .orElseGet(() -> initInventoryForVariant(variantId));
    }

    private Inventory getOrInitInventoryWithLock(Long variantId) {
        return inventoryRepository.findByVariantIdWithLock(variantId)
                .orElseGet(() -> initInventoryForVariant(variantId));
    }

    private Inventory initInventoryForVariant(Long variantId) {
        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("ProductVariant", "id", variantId));

        int initialStock = variant.getStockQuantity() != null ? variant.getStockQuantity() : 0;
        Inventory inventory = Inventory.builder()
                .variant(variant)
                .availableStock(initialStock)
                .reservedStock(0)
                .build();
        return inventoryRepository.save(inventory);
    }
}

