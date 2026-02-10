package com.supplymind.platform_core.repository.core;

import com.supplymind.platform_core.common.enums.InventoryTransactionType;
import com.supplymind.platform_core.model.core.InventoryTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {

    @Query("SELECT t FROM InventoryTransaction t " +
            "WHERE t.product.productId = :productId " +
            "AND t.type = :type " +
            "AND t.timestamp >= :since " +
            "ORDER BY t.timestamp ASC")
    List<InventoryTransaction> findSalesHistory(Long productId, InventoryTransactionType type, Instant since);

    // ✅ Fixed for UI: Added EntityGraph to fetch Product and Warehouse in 1 query
    @EntityGraph(attributePaths = {"product", "warehouse"})
    Page<InventoryTransaction> findAllByWarehouse_WarehouseId(Long warehouseId, Pageable pageable);

    @EntityGraph(attributePaths = {"product", "warehouse"})
    Page<InventoryTransaction> findAllByProduct_ProductId(Long productId, Pageable pageable);

    @EntityGraph(attributePaths = {"product", "warehouse"})
    Page<InventoryTransaction> findAllByWarehouse_WarehouseIdAndProduct_ProductId(
            Long warehouseId,
            Long productId,
            Pageable pageable
    );
}