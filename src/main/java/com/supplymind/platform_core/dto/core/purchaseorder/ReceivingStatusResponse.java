package com.supplymind.platform_core.dto.core.purchaseorder;

import java.util.List;

public record ReceivingStatusResponse(
        Long poId,
        List<ReceivingLineStatus> items
) {
    public record ReceivingLineStatus(
            Long poItemId,
            Long productId,
            String sku,
            String productName,
            Integer orderedQty,
            Integer receivedQty,
            Integer remainingQty
    ) {}
}

