package com.supplymind.platform_core.repository.core;

import com.supplymind.platform_core.common.enums.PurchaseOrderStatus;
import com.supplymind.platform_core.model.core.PurchaseOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    @Query("SELECT po FROM PurchaseOrder po " +
           "LEFT JOIN FETCH po.supplier " +
           "LEFT JOIN FETCH po.warehouse " +
           "LEFT JOIN FETCH po.buyer " +
           "ORDER BY po.poId DESC")
    Page<PurchaseOrder> findAllWithDetails(Pageable pageable);

    // ---------- Single filters ----------

    Page<PurchaseOrder> findAllByStatus(
            PurchaseOrderStatus status,
            Pageable pageable
    );

    Page<PurchaseOrder> findAllBySupplier_SupplierId(
            Long supplierId,
            Pageable pageable
    );

    Page<PurchaseOrder> findAllByWarehouse_WarehouseId(
            Long warehouseId,
            Pageable pageable
    );

    // ---------- Double filters ----------

    Page<PurchaseOrder> findAllByStatusAndSupplier_SupplierId(
            PurchaseOrderStatus status,
            Long supplierId,
            Pageable pageable
    );

    Page<PurchaseOrder> findAllByStatusAndWarehouse_WarehouseId(
            PurchaseOrderStatus status,
            Long warehouseId,
            Pageable pageable
    );

    Page<PurchaseOrder> findAllBySupplier_SupplierIdAndWarehouse_WarehouseId(
            Long supplierId,
            Long warehouseId,
            Pageable pageable
    );

    // ---------- Triple filter ----------

    Page<PurchaseOrder> findAllByStatusAndSupplier_SupplierIdAndWarehouse_WarehouseId(
            PurchaseOrderStatus status,
            Long supplierId,
            Long warehouseId,
            Pageable pageable
    );
}
