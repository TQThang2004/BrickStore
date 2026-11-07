package com.brickstore.inventory.service;

import com.brickstore.catalog.entity.ProductVariant;
import com.brickstore.catalog.repository.ProductVariantRepository;
import com.brickstore.common.exception.BusinessException;
import com.brickstore.inventory.dto.InventoryResponse;
import com.brickstore.inventory.dto.StockAdjustmentRequest;
import com.brickstore.inventory.dto.StockReservationRequest;
import com.brickstore.inventory.enums.InventoryActionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class InventoryConcurrencyTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private ProductVariantRepository productVariantRepository;

    @Test
    @DisplayName("Kiểm thử tranh chấp đa luồng (Concurrency): 20 luồng đồng thời tranh mua 5 sản phẩm tồn kho -> Chống Over-selling tuyệt đối")
    void testConcurrentReservation_PreventOverselling() throws Exception {
        ProductVariant variant = productVariantRepository.findAll().get(0);
        Long variantId = variant.getId();

        // Chuẩn bị dữ liệu: Đưa tồn kho khả dụng về chính xác 5 sản phẩm
        InventoryResponse currentInv = inventoryService.getInventoryByVariantId(variantId);
        int adjustment = 5 - currentInv.getAvailableStock();
        if (adjustment != 0) {
            inventoryService.adjustStock(variantId, StockAdjustmentRequest.builder()
                    .changeQuantity(adjustment)
                    .actionType(InventoryActionType.MANUAL_ADJUST)
                    .note("Setup initial stock 5 for concurrency test")
                    .build());
        }

        // Bắt đầu kiểm thử tranh chấp với 20 luồng song song
        int numberOfThreads = 20;
        int expectedSuccess = 5;
        int expectedFailures = 15;

        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch doneSignal = new CountDownLatch(numberOfThreads);

        AtomicInteger successCounter = new AtomicInteger(0);
        AtomicInteger failureCounter = new AtomicInteger(0);

        for (int i = 0; i < numberOfThreads; i++) {
            final int threadIndex = i;
            executorService.submit(() -> {
                try {
                    // Chờ tín hiệu xuất phát đồng loạt từ startSignal
                    startSignal.await();

                    StockReservationRequest request = StockReservationRequest.builder()
                            .variantId(variantId)
                            .quantity(1)
                            .orderReference("CONCURRENT-ORD-" + threadIndex)
                            .durationMinutes(15)
                            .build();

                    inventoryService.reserveStock(request);
                    successCounter.incrementAndGet();
                } catch (BusinessException e) {
                    // Bị từ chối do hết hàng tồn kho
                    failureCounter.incrementAndGet();
                } catch (Exception e) {
                    failureCounter.incrementAndGet();
                } finally {
                    doneSignal.countDown();
                }
            });
        }

        // Bắn tín hiệu xuất phát cho tất cả 20 luồng cùng lúc
        startSignal.countDown();

        // Chờ tất cả luồng hoàn tất tối đa 10 giây
        boolean completed = doneSignal.await(10, TimeUnit.SECONDS);
        assertTrue(completed, "Các luồng không hoàn thành kịp thời gian quy định");
        executorService.shutdown();

        // Kiểm tra kết quả
        assertEquals(expectedSuccess, successCounter.get(), "Số luồng thành công phải chính xác bằng 5");
        assertEquals(expectedFailures, failureCounter.get(), "Số luồng thất bại phải chính xác bằng 15");

        // Kiểm tra trạng thái tồn kho cuối cùng trong Database
        InventoryResponse finalInv = inventoryService.getInventoryByVariantId(variantId);
        assertEquals(0, finalInv.getAvailableStock(), "Tồn kho khả dụng phải còn chính xác 0");
        assertEquals(5, finalInv.getReservedStock(), "Tồn kho giữ chỗ phải chính xác bằng 5");
    }
}

