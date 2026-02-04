package com.supplymind.platform_core.dto.core.purchaseorder;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PurchaseOrderItemCreateRequest(
        @NotNull Long productId,
        @NotNull @Min(1) Integer orderedQty,
        BigDecimal unitCost
) {}

