package com.brickstore.inventory.repository;

import com.brickstore.inventory.entity.InventoryLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InventoryLogRepository extends JpaRepository<InventoryLog, Long> {

    @EntityGraph(attributePaths = {"variant"})
    Page<InventoryLog> findByVariantIdOrderByCreatedAtDesc(Long variantId, Pageable pageable);
}

