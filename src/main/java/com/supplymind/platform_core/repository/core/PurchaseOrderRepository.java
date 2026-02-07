package com.supplymind.platform_core.repository.core;

import com.supplymind.platform_core.common.enums.PurchaseOrderStatus;
import com.supplymind.platform_core.model.core.PurchaseOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    String FIND_PO_WITH_DETAILS = "SELECT po FROM PurchaseOrder po " +
            "LEFT JOIN FETCH po.supplier s " +
            "LEFT JOIN FETCH po.warehouse w " +
            "LEFT JOIN FETCH po.buyer b ";

    @Query(value = FIND_PO_WITH_DETAILS,
           countQuery = "SELECT count(po) FROM PurchaseOrder po")
    Page<PurchaseOrder> findAllWithDetails(Pageable pageable);

    @Query(value = FIND_PO_WITH_DETAILS + "WHERE po.status = :status",
           countQuery = "SELECT count(po) FROM PurchaseOrder po WHERE po.status = :status")
    Page<PurchaseOrder> findAllByStatus(@Param("status") PurchaseOrderStatus status, Pageable pageable);

    @Query(value = FIND_PO_WITH_DETAILS + "WHERE s.supplierId = :supplierId",
           countQuery = "SELECT count(po) FROM PurchaseOrder po WHERE po.supplier.supplierId = :supplierId")
    Page<PurchaseOrder> findAllBySupplier_SupplierId(@Param("supplierId") Long supplierId, Pageable pageable);

    @Query(value = FIND_PO_WITH_DETAILS + "WHERE w.warehouseId = :warehouseId",
           countQuery = "SELECT count(po) FROM PurchaseOrder po WHERE po.warehouse.warehouseId = :warehouseId")
    Page<PurchaseOrder> findAllByWarehouse_WarehouseId(@Param("warehouseId") Long warehouseId, Pageable pageable);

    @Query(value = FIND_PO_WITH_DETAILS + "WHERE po.status = :status AND s.supplierId = :supplierId",
           countQuery = "SELECT count(po) FROM PurchaseOrder po WHERE po.status = :status AND po.supplier.supplierId = :supplierId")
    Page<PurchaseOrder> findAllByStatusAndSupplier_SupplierId(
            @Param("status") PurchaseOrderStatus status,
            @Param("supplierId") Long supplierId,
            Pageable pageable
    );

    @Query(value = FIND_PO_WITH_DETAILS + "WHERE po.status = :status AND w.warehouseId = :warehouseId",
           countQuery = "SELECT count(po) FROM PurchaseOrder po WHERE po.status = :status AND po.warehouse.warehouseId = :warehouseId")
    Page<PurchaseOrder> findAllByStatusAndWarehouse_WarehouseId(
            @Param("status") PurchaseOrderStatus status,
            @Param("warehouseId") Long warehouseId,
            Pageable pageable
    );

    @Query(value = FIND_PO_WITH_DETAILS + "WHERE s.supplierId = :supplierId AND w.warehouseId = :warehouseId",
           countQuery = "SELECT count(po) FROM PurchaseOrder po WHERE po.supplier.supplierId = :supplierId AND po.warehouse.warehouseId = :warehouseId")
    Page<PurchaseOrder> findAllBySupplier_SupplierIdAndWarehouse_WarehouseId(
            @Param("supplierId") Long supplierId,
            @Param("warehouseId") Long warehouseId,
            Pageable pageable
    );

    @Query(value = FIND_PO_WITH_DETAILS + "WHERE po.status = :status AND s.supplierId = :supplierId AND w.warehouseId = :warehouseId",
           countQuery = "SELECT count(po) FROM PurchaseOrder po WHERE po.status = :status AND po.supplier.supplierId = :supplierId AND po.warehouse.warehouseId = :warehouseId")
    Page<PurchaseOrder> findAllByStatusAndSupplier_SupplierIdAndWarehouse_WarehouseId(
            @Param("status") PurchaseOrderStatus status,
            @Param("supplierId") Long supplierId,
            @Param("warehouseId") Long warehouseId,
            Pageable pageable
    );
}
