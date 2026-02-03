package com.supplymind.platform_core.dto.core.warehouse;

import jakarta.validation.constraints.Size;

public record WarehouseUpdateRequest(
        @Size(max = 255) String locationName,
        String address,
        Integer capacity
) {}

