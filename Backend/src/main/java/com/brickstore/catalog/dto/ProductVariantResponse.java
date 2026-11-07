package com.brickstore.catalog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariantResponse {

    private Long id;
    private Long productId;
    private String sku;
    private String name;
    private BigDecimal price;
    private BigDecimal comparePrice;
    private Integer stockQuantity;
    private Map<String, Object> attributes;
    private String imageUrl;
    private Boolean isActive;
}

