package com.supplymind.platform_core.dto.core.inventory;

import com.supplymind.platform_core.common.enums.InventoryTransactionType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record InventoryTransactionRequest(
        @NotNull Long warehouseId,
        @NotNull Long productId,
        @NotNull InventoryTransactionType type,
        @NotNull @Min(1) Integer quantity
) {}

