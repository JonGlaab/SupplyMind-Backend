package com.supplymind.platform_core.dto.core.inventory;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record InventoryTransferRequest(
        @NotNull Long fromWarehouseId,
        @NotNull Long toWarehouseId,
        @NotNull Long productId,
        @NotNull @Min(1) Integer quantity,
        String notes
) {}