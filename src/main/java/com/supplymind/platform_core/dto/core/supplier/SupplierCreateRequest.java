package com.supplymind.platform_core.dto.core.supplier;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SupplierCreateRequest(
        @NotBlank @Size(max = 255) String name,
        @Email @Size(max = 255) String contactEmail,
        @Size(max = 20) String phone,
        String address
) {}

