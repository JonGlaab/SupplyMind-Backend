package com.supplymind.platform_core.dto.core.warehouse;

import java.time.Instant;

public record WarehouseResponse(
        Long warehouseId,
        String locationName,
        String address,
        Integer capacity,
        Instant createdAt,
        Instant updatedAt
) {}

