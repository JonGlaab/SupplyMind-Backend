package com.supplymind.platform_core.repository.core;

import com.supplymind.platform_core.model.core.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    boolean existsBySku(String sku);
    Optional<Product> findBySku(String sku);

    @Query("SELECT p, SUM(COALESCE(i.qtyOnHand, 0)) " +
            "FROM Product p " +
            "LEFT JOIN Inventory i ON p.productId = i.product.productId " +
            "GROUP BY p")
    Page<Object[]> findAllWithTotalStock(Pageable pageable);
}