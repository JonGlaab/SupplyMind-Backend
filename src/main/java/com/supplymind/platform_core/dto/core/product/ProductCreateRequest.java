package com.supplymind.platform_core.dto.core.product;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record ProductCreateRequest(
        @NotBlank @Size(max = 50) String sku,
        @NotBlank @Size(max = 255) String name,
        @Size(max = 100) String category,
        @DecimalMin(value = "0.0", inclusive = true) BigDecimal unitPrice,
        @Min(0) Integer reorderPoint,
        @Size(max = 1000) String description
) {}

