package com.supplymind.platform_core.dto.core.purchaseorder;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ReceivePurchaseOrderRequest(
        @NotEmpty List<ReceiveLine> lines
) {
    public record ReceiveLine(
            @NotNull Long poItemId,
            @NotNull @Min(1) Integer receiveQty
    ) {}
}

