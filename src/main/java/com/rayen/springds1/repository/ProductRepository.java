package com.rayen.springds1.repository;

import com.rayen.springds1.model.Product;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsBySkuIgnoreCase(String sku);

    boolean existsByCategoryId(Long categoryId);

    @Override
    @EntityGraph(attributePaths = {"category", "suppliers"})
    List<Product> findAll();

    @Override
    @EntityGraph(attributePaths = {"category", "suppliers"})
    Optional<Product> findById(Long id);

    /**
     * Custom query: active products at or below their reorder level.
     * The entity graph uses outer joins, so a product with no supplier is still returned.
     */
    @EntityGraph(attributePaths = {"category", "suppliers"})
    @Query("""
            select p
            from Product p
            where p.active = true
              and p.stockQuantity <= p.reorderLevel
            order by p.stockQuantity asc, p.id asc
            """)
    List<Product> findProductsNeedingReorder();
}
