package com.brickstore.catalog.controller;

import com.brickstore.catalog.dto.ProductDetailResponse;
import com.brickstore.catalog.dto.ProductResponse;
import com.brickstore.catalog.service.ProductService;
import com.brickstore.common.response.ApiResponse;
import com.brickstore.common.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Product - Public", description = "Public Product APIs")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    @Operation(summary = "Search & filter products with pagination")
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String categorySlug,
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) String brandSlug,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Boolean isFeatured,
            @PageableDefault(size = 12, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<ProductResponse> response = productService.getProducts(
                keyword, categoryId, categorySlug, brandId, brandSlug, minPrice, maxPrice, isFeatured, pageable
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/featured")
    @Operation(summary = "Get featured products")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getFeaturedProducts() {
        return ResponseEntity.ok(ApiResponse.success(productService.getFeaturedProducts()));
    }

    @GetMapping("/newest")
    @Operation(summary = "Get newest products")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getNewestProducts() {
        return ResponseEntity.ok(ApiResponse.success(productService.getNewestProducts()));
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Get product detail by slug")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> getProductBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.success(productService.getProductBySlug(slug)));
    }
}

