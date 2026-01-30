package com.supplymind.platform_core.dto.core.supplierproduct;

import java.math.BigDecimal;

public record SupplierProductUpdateRequest(
        Integer leadTimeDays,
        BigDecimal costPrice
) {}

