package com.supplymind.platform_core.dto.core.purchaseorder;

public record PurchaseOrderUpdateRequest(
        Long supplierId,
        Long warehouseId
) {}

