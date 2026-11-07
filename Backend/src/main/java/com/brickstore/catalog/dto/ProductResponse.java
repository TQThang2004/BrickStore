package com.brickstore.catalog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {

    private Long id;
    private String name;
    private String slug;
    private Long categoryId;
    private String categoryName;
    private Long brandId;
    private String brandName;
    private BigDecimal basePrice;
    private String thumbnailUrl;
    private Boolean isActive;
    private Boolean isFeatured;
    private BigDecimal avgRating;
    private Integer reviewCount;
    private Integer soldCount;
    private LocalDateTime createdAt;
}

