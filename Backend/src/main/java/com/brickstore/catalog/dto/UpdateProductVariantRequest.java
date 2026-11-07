package com.brickstore.catalog.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
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
public class UpdateProductVariantRequest {

    @Size(max = 100, message = "SKU must not exceed 100 characters")
    private String sku;

    @Size(max = 255, message = "Variant name must not exceed 255 characters")
    private String name;

    @DecimalMin(value = "0.0", message = "Price cannot be negative")
    private BigDecimal price;

    private BigDecimal comparePrice;

    private Integer stockQuantity;

    private Map<String, Object> attributes;

    private String imageUrl;

    private Boolean isActive;
}

