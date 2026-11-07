package com.brickstore.inventory.entity;

import com.brickstore.catalog.entity.ProductVariant;
import com.brickstore.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "inventory")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inventory extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = false, unique = true)
    private ProductVariant variant;

    @Column(name = "available_stock", nullable = false)
    @Builder.Default
    private Integer availableStock = 0;

    @Column(name = "reserved_stock", nullable = false)
    @Builder.Default
    private Integer reservedStock = 0;

    @Version
    @Column(name = "version")
    @Builder.Default
    private Long version = 0L;

    public Integer getTotalStock() {
        return (availableStock != null ? availableStock : 0) + (reservedStock != null ? reservedStock : 0);
    }
}

