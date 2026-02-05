package com.supplymind.platform_core.dto.core.purchaseorder;

import com.supplymind.platform_core.common.enums.PurchaseOrderStatus;
import jakarta.validation.constraints.NotNull;

public record PurchaseOrderStatusUpdateRequest(
        @NotNull PurchaseOrderStatus status,
        String note
) {}

