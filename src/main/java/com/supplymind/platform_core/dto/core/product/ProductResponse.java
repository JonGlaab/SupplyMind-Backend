package com.supplymind.platform_core.dto.core.product;

import java.math.BigDecimal;

public record ProductResponse(
        Long productId,
        String sku,
        String name,
        String category,
        BigDecimal unitPrice,
        Integer reorderPoint
) {}

