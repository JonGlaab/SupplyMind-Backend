package com.supplymind.platform_core.dto.core.purchaseorder;

import jakarta.validation.constraints.NotNull;

public record PurchaseOrderCreateRequest(
        @NotNull Long supplierId,
        @NotNull Long warehouseId
) {}

