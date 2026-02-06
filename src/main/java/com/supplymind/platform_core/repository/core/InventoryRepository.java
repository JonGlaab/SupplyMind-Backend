package com.supplymind.platform_core.repository.core;

import com.supplymind.platform_core.model.core.Inventory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    // Get total quantity for a product across all warehouses
    @Query("SELECT SUM(i.qtyOnHand) FROM Inventory i WHERE i.product.productId = :productId")
    Integer findTotalQuantityByProductId(Long productId);

    @Query("SELECT i FROM Inventory i JOIN i.product p " +
           "WHERE i.qtyOnHand < p.reorderPoint " +
           "AND (:warehouseId IS NULL OR i.warehouse.warehouseId = :warehouseId) " +
           "AND (:supplierId IS NULL OR EXISTS (SELECT sp FROM SupplierProduct sp WHERE sp.product = p AND sp.supplier.supplierId = :supplierId))")
    Page<Inventory> findLowStock(
            @Param("warehouseId") Long warehouseId,
            @Param("supplierId") Long supplierId,
            Pageable pageable
    );
}
