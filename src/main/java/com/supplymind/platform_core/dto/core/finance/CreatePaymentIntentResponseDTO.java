package com.supplymind.platform_core.dto.core.finance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreatePaymentIntentResponseDTO {
    private Long supplierPaymentId;
    private String clientSecret;
    private String stripePaymentIntentId;
    private String status;
}

