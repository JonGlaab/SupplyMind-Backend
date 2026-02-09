package com.supplymind.platform_core.repository.core;

import com.supplymind.platform_core.model.core.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
    boolean existsBySku(String sku);
}