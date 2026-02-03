package com.supplymind.platform_core.repository.core;

import com.supplymind.platform_core.common.enums.InventoryTransactionType;
import com.supplymind.platform_core.model.core.InventoryTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {

    // ✅ Keep teammate method exactly as-is
    @Query("SELECT t FROM InventoryTransaction t " +
            "WHERE t.product.productId = :productId " +
            "AND t.type = :type " +
            "AND t.timestamp >= :since " +
            "ORDER BY t.timestamp ASC")
    List<InventoryTransaction> findSalesHistory(Long productId, InventoryTransactionType type, Instant since);

    // ✅ New: recent transactions paging for UI
    Page<InventoryTransaction> findAllByWarehouse_WarehouseId(Long warehouseId, Pageable pageable);

    Page<InventoryTransaction> findAllByProduct_ProductId(Long productId, Pageable pageable);

    Page<InventoryTransaction> findAllByWarehouse_WarehouseIdAndProduct_ProductId(
            Long warehouseId,
            Long productId,
            Pageable pageable
    );
}
