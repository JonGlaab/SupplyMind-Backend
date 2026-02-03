package com.supplymind.platform_core.dto.core.inventory;

import java.time.Instant;

public record InventoryResponse(
        Long inventoryId,
        Long warehouseId,
        Long productId,
        String sku,
        String productName,
        Integer qtyOnHand,
        Instant createdAt,
        Instant updatedAt
) {}
