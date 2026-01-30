package com.supplymind.platform_core.dto.core.supplier;

import java.time.Instant;

public record SupplierResponse(
        Long supplierId,
        String name,
        String contactEmail,
        String phone,
        String address,
        Instant createdAt,
        Instant updatedAt
) {}

