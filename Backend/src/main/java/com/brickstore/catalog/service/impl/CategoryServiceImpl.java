package com.brickstore.catalog.service.impl;

import com.brickstore.catalog.dto.CategoryResponse;
import com.brickstore.catalog.dto.CategoryTreeResponse;
import com.brickstore.catalog.dto.CreateCategoryRequest;
import com.brickstore.catalog.dto.UpdateCategoryRequest;
import com.brickstore.catalog.entity.Category;
import com.brickstore.catalog.mapper.CategoryMapper;
import com.brickstore.catalog.repository.CategoryRepository;
import com.brickstore.catalog.service.CategoryService;
import com.brickstore.common.enums.ErrorCode;
import com.brickstore.common.exception.BusinessException;
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
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Override
    public List<CategoryResponse> getAllCategories() {
        return categoryMapper.toResponseList(categoryRepository.findAllByIsActiveTrueOrderBySortOrderAsc());
    }

    @Override
    public List<CategoryTreeResponse> getCategoryTree() {
        List<Category> rootCategories = categoryRepository.findAllByParentIsNullAndIsActiveTrueOrderBySortOrderAsc();
        return categoryMapper.toTreeResponseList(rootCategories);
    }

    @Override
    public CategoryResponse getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));
        return categoryMapper.toResponse(category);
    }

    @Override
    public CategoryResponse getCategoryBySlug(String slug) {
        Category category = categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "slug", slug));
        return categoryMapper.toResponse(category);
    }

    @Override
    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        Category category = categoryMapper.toEntity(request);
        category.setSlug(generateUniqueSlug(request.getName(), null));

        if (request.getParentId() != null) {
            Category parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", "parentId", request.getParentId()));
            category.setParent(parent);
        }

        Category saved = categoryRepository.save(category);
        log.info("Created category with ID: {}, slug: {}", saved.getId(), saved.getSlug());
        return categoryMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(Long id, UpdateCategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));

        if (!category.getName().equalsIgnoreCase(request.getName())) {
            category.setSlug(generateUniqueSlug(request.getName(), id));
        }

        categoryMapper.updateEntityFromRequest(request, category);

        if (request.getParentId() != null) {
            if (request.getParentId().equals(id)) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "Danh mục cha không thể là chính nó");
            }
            Category parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", "parentId", request.getParentId()));
            category.setParent(parent);
        } else {
            category.setParent(null);
        }

        Category updated = categoryRepository.save(category);
        log.info("Updated category with ID: {}", updated.getId());
        return categoryMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));
        categoryRepository.delete(category);
        log.info("Deleted category with ID: {}", id);
    }

    private String generateUniqueSlug(String name, Long existingId) {
        String baseSlug = SlugUtils.toSlug(name);
        String slug = baseSlug;
        int counter = 1;

        while (true) {
            var optionalCategory = categoryRepository.findBySlug(slug);
            if (optionalCategory.isEmpty() || (existingId != null && optionalCategory.get().getId().equals(existingId))) {
                return slug;
            }
            slug = baseSlug + "-" + counter++;
        }
    }
}

