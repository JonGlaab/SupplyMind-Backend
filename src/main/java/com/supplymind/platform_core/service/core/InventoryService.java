package com.supplymind.platform_core.service.core;

import com.supplymind.platform_core.dto.core.inventory.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface InventoryService {

    Page<InventorySlimResponse> listByWarehouse(Long warehouseId, String sku, Pageable pageable);

    void executeImmediateTransfer(InventoryTransferRequest req);

    Page<InventoryResponse> listByWarehouse(Long warehouseId, Pageable pageable);

    void recordTransaction(InventoryTransactionRequest req);

    Page<InventoryTransactionResponse> listTransactions(Long warehouseId, Long productId, Pageable pageable);

    Page<InventoryResponse> findLowStock(Long warehouseId, Long supplierId, Pageable pageable);
}
