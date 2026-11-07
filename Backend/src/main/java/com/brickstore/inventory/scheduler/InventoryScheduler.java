package com.brickstore.inventory.scheduler;

import com.brickstore.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryScheduler {

    private final InventoryService inventoryService;

    /**
     * Định kỳ quét và giải phóng các yêu cầu giữ hàng (reservations) đã hết hạn.
     * Chạy mỗi 60 giây sau lần quét trước.
     */
    @Scheduled(fixedDelayString = "${app.inventory.reservation-cleanup-interval-ms:60000}", initialDelay = 10000)
    public void cleanupExpiredReservations() {
        try {
            int releasedCount = inventoryService.releaseExpiredReservations();
            if (releasedCount > 0) {
                log.info("Auto-cleanup completed: Successfully released {} expired stock reservations", releasedCount);
            }
        } catch (Exception e) {
            log.error("Error occurred during expired stock reservations cleanup: {}", e.getMessage(), e);
        }
    }
}

