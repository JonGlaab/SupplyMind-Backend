package com.supplymind.platform_core.dto.core.purchaseorder;

public record ApprovalResponse(
    PurchaseOrderResponse po,
    String presignedUrl
) {}
