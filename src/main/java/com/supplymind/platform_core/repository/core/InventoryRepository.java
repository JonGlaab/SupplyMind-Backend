package com.supplymind.platform_core.repository.core;

import com.supplymind.platform_core.model.core.Inventory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    // ✅ Existing (keep this)
    Optional<Inventory> findByProduct_ProductId(Long productId);

    // ✅ New: list inventory per warehouse (paginated)
    Page<Inventory> findAllByWarehouse_WarehouseId(Long warehouseId, Pageable pageable);

    // ✅ New: get inventory row for a specific warehouse + product
    Optional<Inventory> findByWarehouse_WarehouseIdAndProduct_ProductId(
            Long warehouseId,
            Long productId
    );
}
