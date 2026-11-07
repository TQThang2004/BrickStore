package com.brickstore.catalog.service.impl;

import com.brickstore.catalog.dto.BrandResponse;
import com.brickstore.catalog.dto.CreateBrandRequest;
import com.brickstore.catalog.dto.UpdateBrandRequest;
import com.brickstore.catalog.entity.Brand;
import com.brickstore.catalog.mapper.BrandMapper;
import com.brickstore.catalog.repository.BrandRepository;
import com.brickstore.catalog.service.BrandService;
import com.brickstore.common.exception.ResourceNotFoundException;
import com.brickstore.common.util.SlugUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BrandServiceImpl implements BrandService {

    private final BrandRepository brandRepository;
    private final BrandMapper brandMapper;

    @Override
    public List<BrandResponse> getAllActiveBrands() {
        return brandMapper.toResponseList(brandRepository.findAllByIsActiveTrueOrderByNameAsc());
    }

    @Override
    public List<BrandResponse> getAllBrands() {
        return brandMapper.toResponseList(brandRepository.findAll());
    }

    @Override
    public BrandResponse getBrandById(Long id) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Brand", "id", id));
        return brandMapper.toResponse(brand);
    }

    @Override
    public BrandResponse getBrandBySlug(String slug) {
        Brand brand = brandRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Brand", "slug", slug));
        return brandMapper.toResponse(brand);
    }

    @Override
    @Transactional
    public BrandResponse createBrand(CreateBrandRequest request) {
        Brand brand = brandMapper.toEntity(request);
        brand.setSlug(generateUniqueSlug(request.getName(), null));

        Brand saved = brandRepository.save(brand);
        log.info("Created brand with ID: {}, slug: {}", saved.getId(), saved.getSlug());
        return brandMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public BrandResponse updateBrand(Long id, UpdateBrandRequest request) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Brand", "id", id));

        if (!brand.getName().equalsIgnoreCase(request.getName())) {
            brand.setSlug(generateUniqueSlug(request.getName(), id));
        }

        brandMapper.updateEntityFromRequest(request, brand);
        Brand updated = brandRepository.save(brand);
        log.info("Updated brand with ID: {}", updated.getId());
        return brandMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void deleteBrand(Long id) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Brand", "id", id));
        brandRepository.delete(brand);
        log.info("Deleted brand with ID: {}", id);
    }

    private String generateUniqueSlug(String name, Long existingId) {
        String baseSlug = SlugUtils.toSlug(name);
        String slug = baseSlug;
        int counter = 1;

        while (true) {
            var optionalBrand = brandRepository.findBySlug(slug);
            if (optionalBrand.isEmpty() || (existingId != null && optionalBrand.get().getId().equals(existingId))) {
                return slug;
            }
            slug = baseSlug + "-" + counter++;
        }
    }
}

