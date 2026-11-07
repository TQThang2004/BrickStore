package com.brickstore.catalog.service;

import com.brickstore.catalog.dto.*;
import com.brickstore.common.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

public interface ProductService {

    PageResponse<ProductResponse> getProducts(
            String keyword,
            Long categoryId,
            String categorySlug,
            Long brandId,
            String brandSlug,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Boolean isFeatured,
            Pageable pageable
    );

    PageResponse<ProductResponse> getAdminProducts(
            String keyword,
            Long categoryId,
            Long brandId,
            Boolean isActive,
            Pageable pageable
    );

    ProductDetailResponse getProductBySlug(String slug);

    ProductDetailResponse getProductById(Long id);

    List<ProductResponse> getFeaturedProducts();

    List<ProductResponse> getNewestProducts();

    ProductDetailResponse createProduct(CreateProductRequest request);

    ProductDetailResponse updateProduct(Long id, UpdateProductRequest request);

    void deleteProduct(Long id);

    ProductVariantResponse addVariant(Long productId, CreateProductVariantRequest request);

    ProductVariantResponse updateVariant(Long productId, Long variantId, UpdateProductVariantRequest request);

    void deleteVariant(Long productId, Long variantId);

    ProductImageResponse addImage(Long productId, ProductImageRequest request);

    void deleteImage(Long productId, Long imageId);
}

