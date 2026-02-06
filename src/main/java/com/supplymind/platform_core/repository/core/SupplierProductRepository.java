package com.supplymind.platform_core.repository.core;

import com.supplymind.platform_core.model.core.SupplierProduct;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupplierProductRepository extends JpaRepository<SupplierProduct, Long> {

    boolean existsBySupplier_SupplierIdAndProduct_ProductId(Long supplierId, Long productId);

    List<SupplierProduct> findAllBySupplier_SupplierId(Long supplierId);

    List<SupplierProduct> findAllByProduct_ProductId(Long productId);
}
