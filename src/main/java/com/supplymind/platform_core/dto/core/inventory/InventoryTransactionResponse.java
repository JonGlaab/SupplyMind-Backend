package com.supplymind.platform_core.dto.core.inventory;

import com.supplymind.platform_core.common.enums.InventoryTransactionType;

import java.time.Instant;

public record InventoryTransactionResponse(
        Long transactionId,
        Long warehouseId,
        Long productId,
        String sku,
        String productName,
        InventoryTransactionType type,
        Integer quantity,
        Instant timestamp
) {}

