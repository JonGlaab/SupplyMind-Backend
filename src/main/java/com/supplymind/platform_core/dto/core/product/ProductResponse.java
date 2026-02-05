package com.supplymind.platform_core.dto.core.product;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductResponse(
        Long productId,
        String sku,
        String name,
        String category,
        BigDecimal unitPrice,
        Integer reorderPoint,
        String description,
        Instant createdAt,
        Instant updatedAt,
        Integer qtyOnHand,
        Integer minStockLevel
) {}