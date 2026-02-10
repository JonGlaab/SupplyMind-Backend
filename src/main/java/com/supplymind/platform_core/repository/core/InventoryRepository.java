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
    @Query("SELECT i FROM Inventory i " +
            "JOIN FETCH i.product " +
            "JOIN FETCH i.warehouse " +
            "WHERE i.warehouse.warehouseId = :warehouseId")
    Page<Inventory> findAllByWarehouse_WarehouseId(@Param("warehouseId") Long warehouseId, Pageable pageable);

    // ✅ New: get inventory row for a specific warehouse + product
    Optional<Inventory> findByWarehouse_WarehouseIdAndProduct_ProductId(
            Long warehouseId,
            Long productId
    );

    // Get total quantity for a product across all warehouses
    @Query("SELECT SUM(i.qtyOnHand) FROM Inventory i WHERE i.product.productId = :productId")
    Integer findTotalQuantityByProductId(Long productId);

    @Query("SELECT i FROM Inventory i " +
            "JOIN FETCH i.product p " +
            "JOIN FETCH i.warehouse w " +
            "WHERE i.qtyOnHand < p.reorderPoint " +
            "AND (:warehouseId IS NULL OR w.warehouseId = :warehouseId)")
    Page<Inventory> findLowStock(
            @Param("warehouseId") Long warehouseId,
            Pageable pageable
    );
    @Query("SELECT i FROM Inventory i " +
            "JOIN FETCH i.product p " +
            "JOIN FETCH i.warehouse w " +
            "LEFT JOIN FETCH p.supplierProducts sp " +
            "LEFT JOIN FETCH sp.supplier s " +
            "WHERE w.warehouseId = :warehouseId")
    Page<Inventory> findAllByWarehouseWithDetails(@Param("warehouseId") Long warehouseId, Pageable pageable);
}
