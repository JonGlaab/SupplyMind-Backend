package com.supplymind.platform_core.dto.core.payments;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CreatePaymentIntentResponseDTO {
    private Long paymentId;
    private String paymentIntentId;
    private String clientSecret;
}

