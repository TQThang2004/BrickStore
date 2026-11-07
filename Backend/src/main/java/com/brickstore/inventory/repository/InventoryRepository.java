package com.brickstore.inventory.repository;

import com.brickstore.inventory.entity.Inventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    @EntityGraph(attributePaths = {"variant", "variant.product"})
    Optional<Inventory> findByVariantId(Long variantId);

    boolean existsByVariantId(Long variantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.variant.id = :variantId")
    Optional<Inventory> findByVariantIdWithLock(@Param("variantId") Long variantId);

    @EntityGraph(attributePaths = {"variant", "variant.product"})
    @Query("SELECT i FROM Inventory i")
    Page<Inventory> findAllWithVariant(Pageable pageable);

    @Modifying
    @Query("UPDATE Inventory i SET i.availableStock = i.availableStock - :qty, i.reservedStock = i.reservedStock + :qty, i.updatedAt = CURRENT_TIMESTAMP WHERE i.variant.id = :variantId AND i.availableStock >= :qty")
    int reserveStockAtomic(@Param("variantId") Long variantId, @Param("qty") int qty);

    @Modifying
    @Query("UPDATE Inventory i SET i.reservedStock = i.reservedStock - :qty, i.updatedAt = CURRENT_TIMESTAMP WHERE i.variant.id = :variantId AND i.reservedStock >= :qty")
    int commitStockAtomic(@Param("variantId") Long variantId, @Param("qty") int qty);

    @Modifying
    @Query("UPDATE Inventory i SET i.availableStock = i.availableStock + :qty, i.reservedStock = i.reservedStock - :qty, i.updatedAt = CURRENT_TIMESTAMP WHERE i.variant.id = :variantId AND i.reservedStock >= :qty")
    int releaseStockAtomic(@Param("variantId") Long variantId, @Param("qty") int qty);
}

