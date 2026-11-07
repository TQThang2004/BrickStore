package com.brickstore.catalog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDetailResponse {

    private Long id;
    private String name;
    private String slug;
    private Long categoryId;
    private String categoryName;
    private Long brandId;
    private String brandName;
    private String description;
    private String shortDescription;
    private BigDecimal basePrice;
    private String thumbnailUrl;
    private Boolean isActive;
    private Boolean isFeatured;
    private BigDecimal avgRating;
    private Integer reviewCount;
    private Integer soldCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder.Default
    private List<ProductVariantResponse> variants = new ArrayList<>();

    @Builder.Default
    private List<ProductImageResponse> images = new ArrayList<>();
}

