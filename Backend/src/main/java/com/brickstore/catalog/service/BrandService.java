package com.brickstore.catalog.service;

import com.brickstore.catalog.dto.BrandResponse;
import com.brickstore.catalog.dto.CreateBrandRequest;
import com.brickstore.catalog.dto.UpdateBrandRequest;

import java.util.List;

public interface BrandService {

    List<BrandResponse> getAllActiveBrands();

    List<BrandResponse> getAllBrands();

    BrandResponse getBrandById(Long id);

    BrandResponse getBrandBySlug(String slug);

    BrandResponse createBrand(CreateBrandRequest request);

    BrandResponse updateBrand(Long id, UpdateBrandRequest request);

    void deleteBrand(Long id);
}

