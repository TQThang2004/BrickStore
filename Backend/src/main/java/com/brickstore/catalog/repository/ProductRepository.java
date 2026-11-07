package com.brickstore.catalog.repository;

import com.brickstore.catalog.entity.Product;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    Optional<Product> findBySlug(String slug);

    Optional<Product> findBySlugAndIsActiveTrue(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    @EntityGraph(attributePaths = {"category", "brand"})
    Optional<Product> findWithDetailsBySlug(String slug);

    @EntityGraph(attributePaths = {"category", "brand"})
    Optional<Product> findWithDetailsBySlugAndIsActiveTrue(String slug);

    List<Product> findTop10ByIsFeaturedTrueAndIsActiveTrueOrderByCreatedAtDesc();

    List<Product> findTop10ByIsActiveTrueOrderByCreatedAtDesc();
}
