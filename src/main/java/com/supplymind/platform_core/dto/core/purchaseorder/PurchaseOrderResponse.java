package com.supplymind.platform_core.dto.core.purchaseorder;

import com.supplymind.platform_core.common.enums.PurchaseOrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record PurchaseOrderResponse(
        Long poId,
        Long supplierId,
        String supplierName,
        Long warehouseId,
        String warehouseName,
        Long buyerId,
        String buyerEmail,
        PurchaseOrderStatus status,
        BigDecimal totalAmount,
        Instant createdOn,
        String pdfUrl,
        List<PurchaseOrderItemResponse> items
) {}
