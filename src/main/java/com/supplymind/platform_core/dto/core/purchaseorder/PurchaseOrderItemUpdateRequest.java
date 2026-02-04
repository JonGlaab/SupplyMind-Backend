package com.supplymind.platform_core.dto.core.purchaseorder;

import jakarta.validation.constraints.Min;

import java.math.BigDecimal;

public record PurchaseOrderItemUpdateRequest(
        @Min(1) Integer orderedQty,
        BigDecimal unitCost
) {}

