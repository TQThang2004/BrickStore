package com.brickstore.catalog.controller;

import com.brickstore.catalog.dto.*;
import com.brickstore.catalog.service.ProductService;
import com.brickstore.common.response.ApiResponse;
import com.brickstore.common.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/products")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Product - Admin", description = "Admin Product Management APIs")
public class AdminProductController {

    private final ProductService productService;

    @GetMapping
    @Operation(summary = "Get list of all products for admin with filters and pagination")
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getAdminProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) Boolean isActive,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<ProductResponse> response = productService.getAdminProducts(
                keyword, categoryId, brandId, isActive, pageable
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product detail by ID")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(productService.getProductById(id)));
    }

    @PostMapping
    @Operation(summary = "Create product with initial variants and images")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> createProduct(
            @Valid @RequestBody CreateProductRequest request
    ) {
        ProductDetailResponse response = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo sản phẩm thành công", response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update product details")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProductRequest request
    ) {
        ProductDetailResponse response = productService.updateProduct(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật sản phẩm thành công", response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete product")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa sản phẩm thành công", null));
    }

    @PostMapping("/{id}/variants")
    @Operation(summary = "Add variant to product")
    public ResponseEntity<ApiResponse<ProductVariantResponse>> addVariant(
            @PathVariable Long id,
            @Valid @RequestBody CreateProductVariantRequest request
    ) {
        ProductVariantResponse response = productService.addVariant(id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Thêm biến thể thành công", response));
    }

    @PutMapping("/{id}/variants/{variantId}")
    @Operation(summary = "Update product variant")
    public ResponseEntity<ApiResponse<ProductVariantResponse>> updateVariant(
            @PathVariable Long id,
            @PathVariable Long variantId,
            @Valid @RequestBody UpdateProductVariantRequest request
    ) {
        ProductVariantResponse response = productService.updateVariant(id, variantId, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật biến thể thành công", response));
    }

    @DeleteMapping("/{id}/variants/{variantId}")
    @Operation(summary = "Delete product variant")
    public ResponseEntity<ApiResponse<Void>> deleteVariant(
            @PathVariable Long id,
            @PathVariable Long variantId
    ) {
        productService.deleteVariant(id, variantId);
        return ResponseEntity.ok(ApiResponse.success("Xóa biến thể thành công", null));
    }

    @PostMapping("/{id}/images")
    @Operation(summary = "Add image to product")
    public ResponseEntity<ApiResponse<ProductImageResponse>> addImage(
            @PathVariable Long id,
            @Valid @RequestBody ProductImageRequest request
    ) {
        ProductImageResponse response = productService.addImage(id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Thêm hình ảnh thành công", response));
    }

    @DeleteMapping("/{id}/images/{imageId}")
    @Operation(summary = "Delete product image")
    public ResponseEntity<ApiResponse<Void>> deleteImage(
            @PathVariable Long id,
            @PathVariable Long imageId
    ) {
        productService.deleteImage(id, imageId);
        return ResponseEntity.ok(ApiResponse.success("Xóa hình ảnh thành công", null));
    }
}
