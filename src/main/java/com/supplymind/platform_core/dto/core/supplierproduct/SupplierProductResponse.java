package com.supplymind.platform_core.dto.core.supplierproduct;

import java.math.BigDecimal;

public record SupplierProductResponse(
        Long id,
        Long supplierId,
        String supplierName,
        Long productId,
        String productSku,
        String productName,
        Integer leadTimeDays,
        BigDecimal costPrice
) {}

