package com.brickstore.inventory.service;

import com.brickstore.catalog.entity.ProductVariant;
import com.brickstore.catalog.repository.ProductVariantRepository;
import com.brickstore.common.exception.BusinessException;
import com.brickstore.inventory.dto.*;
import com.brickstore.inventory.entity.StockReservation;
import com.brickstore.inventory.enums.InventoryActionType;
import com.brickstore.inventory.enums.ReservationStatus;
import com.brickstore.inventory.repository.InventoryRepository;
import com.brickstore.inventory.repository.StockReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class InventoryServiceTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private StockReservationRepository stockReservationRepository;

    @Autowired
    private ProductVariantRepository productVariantRepository;

    private Long variantId;

    @BeforeEach
    void setUp() {
        ProductVariant variant = productVariantRepository.findAll().get(0);
        variantId = variant.getId();
        // Reset stock to 10 for deterministic testing
        StockAdjustmentRequest resetRequest = StockAdjustmentRequest.builder()
                .changeQuantity(10 - inventoryService.getInventoryByVariantId(variantId).getAvailableStock())
                .actionType(InventoryActionType.MANUAL_ADJUST)
                .note("Reset stock for test")
                .build();
        inventoryService.adjustStock(variantId, resetRequest);
    }

    @Test
    @DisplayName("Kiểm tra tồn kho: Đủ hàng và Không đủ hàng")
    void checkStock_Success() {
        assertTrue(inventoryService.checkStock(variantId, 5));
        assertTrue(inventoryService.checkStock(variantId, 10));
        assertFalse(inventoryService.checkStock(variantId, 11));
    }

    @Test
    @DisplayName("Điều chỉnh tăng tồn kho (Restock) thành công")
    void adjustStock_Restock_Success() {
        StockAdjustmentRequest request = StockAdjustmentRequest.builder()
                .changeQuantity(5)
                .actionType(InventoryActionType.RESTOCK)
                .note("Nhập thêm 5 kiện hàng")
                .build();

        InventoryResponse response = inventoryService.adjustStock(variantId, request);
        assertEquals(15, response.getAvailableStock());
    }

    @Test
    @DisplayName("Điều chỉnh giảm tồn kho vượt quá số lượng hiện có -> Ném lỗi INSUFFICIENT_STOCK")
    void adjustStock_ExceedStock_ThrowsException() {
        StockAdjustmentRequest request = StockAdjustmentRequest.builder()
                .changeQuantity(-20)
                .actionType(InventoryActionType.MANUAL_ADJUST)
                .note("Giảm vượt tồn kho")
                .build();

        assertThrows(BusinessException.class, () -> inventoryService.adjustStock(variantId, request));
    }

    @Test
    @DisplayName("Giữ chỗ tồn kho (Stock Reservation) thành công")
    void reserveStock_Success() {
        StockReservationRequest request = StockReservationRequest.builder()
                .variantId(variantId)
                .quantity(3)
                .orderReference("ORD-TEST-001")
                .durationMinutes(15)
                .build();

        StockReservationResponse response = inventoryService.reserveStock(request);
        assertNotNull(response.getReservationId());
        assertEquals(ReservationStatus.RESERVED, response.getStatus());
        assertEquals(3, response.getQuantity());

        InventoryResponse inv = inventoryService.getInventoryByVariantId(variantId);
        assertEquals(7, inv.getAvailableStock());
        assertEquals(3, inv.getReservedStock());
    }

    @Test
    @DisplayName("Giữ chỗ khi không đủ hàng -> Ném lỗi INSUFFICIENT_STOCK")
    void reserveStock_Insufficient_ThrowsException() {
        StockReservationRequest request = StockReservationRequest.builder()
                .variantId(variantId)
                .quantity(15)
                .orderReference("ORD-TEST-002")
                .build();

        assertThrows(BusinessException.class, () -> inventoryService.reserveStock(request));
    }

    @Test
    @DisplayName("Commit Reservation (Xác nhận xuất kho khi thanh toán thành công)")
    void commitReservation_Success() {
        StockReservationRequest request = StockReservationRequest.builder()
                .variantId(variantId)
                .quantity(4)
                .orderReference("ORD-TEST-COMMIT")
                .build();

        StockReservationResponse res = inventoryService.reserveStock(request);

        inventoryService.commitReservation(res.getReservationId());

        InventoryResponse inv = inventoryService.getInventoryByVariantId(variantId);
        assertEquals(6, inv.getAvailableStock());
        assertEquals(0, inv.getReservedStock());

        StockReservation updatedRes = stockReservationRepository.findById(res.getReservationId()).orElseThrow();
        assertEquals(ReservationStatus.COMMITTED, updatedRes.getStatus());
    }

    @Test
    @DisplayName("Release Reservation (Hủy hoặc nhả hàng khi hết hạn)")
    void releaseReservation_Success() {
        StockReservationRequest request = StockReservationRequest.builder()
                .variantId(variantId)
                .quantity(4)
                .orderReference("ORD-TEST-RELEASE")
                .build();

        StockReservationResponse res = inventoryService.reserveStock(request);

        inventoryService.releaseReservation(res.getReservationId());

        InventoryResponse inv = inventoryService.getInventoryByVariantId(variantId);
        assertEquals(10, inv.getAvailableStock());
        assertEquals(0, inv.getReservedStock());

        StockReservation updatedRes = stockReservationRepository.findById(res.getReservationId()).orElseThrow();
        assertEquals(ReservationStatus.RELEASED, updatedRes.getStatus());
    }

    @Test
    @DisplayName("Giải phóng các Reservation đã quá hạn (Expired Reservations)")
    void releaseExpiredReservations_Success() {
        StockReservationRequest request = StockReservationRequest.builder()
                .variantId(variantId)
                .quantity(2)
                .orderReference("ORD-TEST-EXPIRED")
                .durationMinutes(15)
                .build();

        StockReservationResponse res = inventoryService.reserveStock(request);

        // Giả lập reservation đã hết hạn trong quá khứ
        StockReservation reservation = stockReservationRepository.findById(res.getReservationId()).orElseThrow();
        reservation.setExpiresAt(LocalDateTime.now().minusMinutes(5));
        stockReservationRepository.save(reservation);

        int released = inventoryService.releaseExpiredReservations();
        assertTrue(released >= 1);

        InventoryResponse inv = inventoryService.getInventoryByVariantId(variantId);
        assertEquals(10, inv.getAvailableStock());
        assertEquals(0, inv.getReservedStock());
    }
}

