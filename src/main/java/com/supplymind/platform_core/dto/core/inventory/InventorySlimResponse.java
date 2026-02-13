package com.supplymind.platform_core.dto.core.inventory;

import java.math.BigDecimal;
import java.time.Instant;

public record InventorySlimResponse(
        Long inventoryId,
        Long warehouseId,
        String warehouseName,
        Long productId,
        String sku,
        String productName,
        Integer qtyOnHand,
        Integer reorderPoint,
        BigDecimal unitPrice,
        Instant updatedAt
) {}