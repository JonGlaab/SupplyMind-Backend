package com.supplymind.platform_core.dto.core.purchaseorder;

import java.math.BigDecimal;

public record PurchaseOrderItemResponse(
        Long poItemId,
        Long productId,
        String sku,
        String productName,
        Integer orderedQty,
        Integer receivedQty,
        BigDecimal unitCost
) {}

