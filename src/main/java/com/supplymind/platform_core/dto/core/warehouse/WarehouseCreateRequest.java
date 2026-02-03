package com.supplymind.platform_core.dto.core.warehouse;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WarehouseCreateRequest(
        @NotBlank @Size(max = 255) String locationName,
        String address,
        Integer capacity
) {}

