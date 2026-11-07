package com.brickstore.catalog.service;

import com.brickstore.catalog.dto.CategoryResponse;
import com.brickstore.catalog.dto.CategoryTreeResponse;
import com.brickstore.catalog.dto.CreateCategoryRequest;
import com.brickstore.catalog.dto.UpdateCategoryRequest;

import java.util.List;

public interface CategoryService {

    List<CategoryResponse> getAllCategories();

    List<CategoryTreeResponse> getCategoryTree();

    CategoryResponse getCategoryById(Long id);

    CategoryResponse getCategoryBySlug(String slug);

    CategoryResponse createCategory(CreateCategoryRequest request);

    CategoryResponse updateCategory(Long id, UpdateCategoryRequest request);

    void deleteCategory(Long id);
}

