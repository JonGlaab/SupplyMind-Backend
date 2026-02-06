package com.supplymind.platform_core.dto.core.inventory;

import java.time.Instant;

public record InventoryResponse(
        Long inventoryId,
        Long warehouseId,
        String warehouseName,
        Long productId,
        String sku,
        String productName,
        Integer qtyOnHand,
        Integer reorderPoint,
        Long supplierId,
        String supplierName,
        Instant createdAt,
        Instant updatedAt
) {}
