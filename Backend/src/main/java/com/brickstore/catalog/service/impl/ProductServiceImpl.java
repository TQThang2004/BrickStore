package com.brickstore.catalog.service.impl;

import com.brickstore.catalog.dto.*;
import com.brickstore.catalog.entity.Brand;
import com.brickstore.catalog.entity.Category;
import com.brickstore.catalog.entity.Product;
import com.brickstore.catalog.entity.ProductImage;
import com.brickstore.catalog.entity.ProductVariant;
import com.brickstore.catalog.mapper.ProductMapper;
import com.brickstore.catalog.repository.BrandRepository;
import com.brickstore.catalog.repository.CategoryRepository;
import com.brickstore.catalog.repository.ProductImageRepository;
import com.brickstore.catalog.repository.ProductRepository;
import com.brickstore.catalog.repository.ProductVariantRepository;
import com.brickstore.catalog.service.ProductService;
import com.brickstore.catalog.specification.ProductSpecification;
import com.brickstore.common.enums.ErrorCode;
import com.brickstore.common.exception.BusinessException;
import com.brickstore.common.exception.ResourceNotFoundException;
import com.brickstore.common.response.PageResponse;
import com.brickstore.common.util.SlugUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductImageRepository productImageRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final ProductMapper productMapper;

    @Override
    public PageResponse<ProductResponse> getProducts(
            String keyword,
            Long categoryId,
            String categorySlug,
            Long brandId,
            String brandSlug,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Boolean isFeatured,
            Pageable pageable
    ) {
        Specification<Product> spec = ProductSpecification.filter(
                keyword, categoryId, categorySlug, brandId, brandSlug, minPrice, maxPrice, isFeatured, true
        );

        Page<Product> page = productRepository.findAll(spec, pageable);
        return PageResponse.<ProductResponse>builder()
                .content(productMapper.toResponseList(page.getContent()))
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    @Override
    public PageResponse<ProductResponse> getAdminProducts(
            String keyword,
            Long categoryId,
            Long brandId,
            Boolean isActive,
            Pageable pageable
    ) {
        Specification<Product> spec = ProductSpecification.filter(
                keyword, categoryId, null, brandId, null, null, null, null, isActive
        );

        Page<Product> page = productRepository.findAll(spec, pageable);
        return PageResponse.<ProductResponse>builder()
                .content(productMapper.toResponseList(page.getContent()))
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    @Override
    public ProductDetailResponse getProductBySlug(String slug) {
        Product product = productRepository.findWithDetailsBySlugAndIsActiveTrue(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "slug", slug));
        return productMapper.toDetailResponse(product);
    }

    @Override
    public ProductDetailResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
        return productMapper.toDetailResponse(product);
    }

    @Override
    public List<ProductResponse> getFeaturedProducts() {
        return productMapper.toResponseList(productRepository.findTop10ByIsFeaturedTrueAndIsActiveTrueOrderByCreatedAtDesc());
    }

    @Override
    public List<ProductResponse> getNewestProducts() {
        return productMapper.toResponseList(productRepository.findTop10ByIsActiveTrueOrderByCreatedAtDesc());
    }

    @Override
    @Transactional
    public ProductDetailResponse createProduct(CreateProductRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));

        Brand brand = null;
        if (request.getBrandId() != null) {
            brand = brandRepository.findById(request.getBrandId())
                    .orElseThrow(() -> new ResourceNotFoundException("Brand", "id", request.getBrandId()));
        }

        Product product = productMapper.toEntity(request);
        product.setCategory(category);
        product.setBrand(brand);
        product.setSlug(generateUniqueSlug(request.getName(), null));

        // Validate SKU list uniqueness in request
        if (request.getVariants() != null && !request.getVariants().isEmpty()) {
            Set<String> skus = new HashSet<>();
            for (CreateProductVariantRequest variantReq : request.getVariants()) {
                if (!skus.add(variantReq.getSku())) {
                    throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "SKU bị trùng lặp trong request: " + variantReq.getSku());
                }
                if (productVariantRepository.existsBySku(variantReq.getSku())) {
                    throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "SKU đã tồn tại trong hệ thống: " + variantReq.getSku());
                }
                ProductVariant variant = productMapper.toVariantEntity(variantReq);
                product.addVariant(variant);
            }
        }

        if (request.getImages() != null && !request.getImages().isEmpty()) {
            for (ProductImageRequest imgReq : request.getImages()) {
                ProductImage image = productMapper.toImageEntity(imgReq);
                product.addImage(image);
            }
        }

        Product saved = productRepository.save(product);
        log.info("Created product with ID: {}, slug: {}", saved.getId(), saved.getSlug());
        return productMapper.toDetailResponse(saved);
    }

    @Override
    @Transactional
    public ProductDetailResponse updateProduct(Long id, UpdateProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));

        if (!product.getCategory().getId().equals(request.getCategoryId())) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));
            product.setCategory(category);
        }

        if (request.getBrandId() != null) {
            if (product.getBrand() == null || !product.getBrand().getId().equals(request.getBrandId())) {
                Brand brand = brandRepository.findById(request.getBrandId())
                        .orElseThrow(() -> new ResourceNotFoundException("Brand", "id", request.getBrandId()));
                product.setBrand(brand);
            }
        } else {
            product.setBrand(null);
        }

        if (!product.getName().equalsIgnoreCase(request.getName())) {
            product.setSlug(generateUniqueSlug(request.getName(), id));
        }

        productMapper.updateEntityFromRequest(request, product);
        Product updated = productRepository.save(product);
        log.info("Updated product with ID: {}", updated.getId());
        return productMapper.toDetailResponse(updated);
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
        productRepository.delete(product);
        log.info("Deleted product with ID: {}", id);
    }

    @Override
    @Transactional
    public ProductVariantResponse addVariant(Long productId, CreateProductVariantRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

        if (productVariantRepository.existsBySku(request.getSku())) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "SKU đã tồn tại: " + request.getSku());
        }

        ProductVariant variant = productMapper.toVariantEntity(request);
        product.addVariant(variant);
        ProductVariant saved = productVariantRepository.save(variant);
        log.info("Added variant SKU: {} to product ID: {}", saved.getSku(), productId);
        return productMapper.toVariantResponse(saved);
    }

    @Override
    @Transactional
    public ProductVariantResponse updateVariant(Long productId, Long variantId, UpdateProductVariantRequest request) {
        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("ProductVariant", "id", variantId));

        if (!variant.getProduct().getId().equals(productId)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Biến thể không thuộc sản phẩm này");
        }

        if (request.getSku() != null && productVariantRepository.existsBySkuAndIdNot(request.getSku(), variantId)) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "SKU đã tồn tại: " + request.getSku());
        }

        productMapper.updateVariantFromRequest(request, variant);
        ProductVariant updated = productVariantRepository.save(variant);
        log.info("Updated variant ID: {} of product ID: {}", variantId, productId);
        return productMapper.toVariantResponse(updated);
    }

    @Override
    @Transactional
    public void deleteVariant(Long productId, Long variantId) {
        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("ProductVariant", "id", variantId));

        if (!variant.getProduct().getId().equals(productId)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Biến thể không thuộc sản phẩm này");
        }

        Product product = variant.getProduct();
        product.removeVariant(variant);
        productVariantRepository.delete(variant);
        log.info("Deleted variant ID: {} of product ID: {}", variantId, productId);
    }

    @Override
    @Transactional
    public ProductImageResponse addImage(Long productId, ProductImageRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

        ProductImage image = productMapper.toImageEntity(request);
        product.addImage(image);
        ProductImage saved = productImageRepository.save(image);
        log.info("Added image ID: {} to product ID: {}", saved.getId(), productId);
        return productMapper.toImageResponse(saved);
    }

    @Override
    @Transactional
    public void deleteImage(Long productId, Long imageId) {
        ProductImage image = productImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("ProductImage", "id", imageId));

        if (!image.getProduct().getId().equals(productId)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Hình ảnh không thuộc sản phẩm này");
        }

        Product product = image.getProduct();
        product.removeImage(image);
        productImageRepository.delete(image);
        log.info("Deleted image ID: {} of product ID: {}", imageId, productId);
    }

    private String generateUniqueSlug(String name, Long existingId) {
        String baseSlug = SlugUtils.toSlug(name);
        String slug = baseSlug;
        int counter = 1;

        while (true) {
            var optionalProduct = productRepository.findBySlug(slug);
            if (optionalProduct.isEmpty() || (existingId != null && optionalProduct.get().getId().equals(existingId))) {
                return slug;
            }
            slug = baseSlug + "-" + counter++;
        }
    }
}

