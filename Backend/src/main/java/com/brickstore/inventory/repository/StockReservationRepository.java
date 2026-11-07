package com.brickstore.inventory.repository;

import com.brickstore.inventory.entity.StockReservation;
import com.brickstore.inventory.enums.ReservationStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface StockReservationRepository extends JpaRepository<StockReservation, UUID> {

    @EntityGraph(attributePaths = {"variant"})
    List<StockReservation> findAllByStatusAndExpiresAtBefore(ReservationStatus status, LocalDateTime threshold);

    List<StockReservation> findByOrderReference(String orderReference);
}

