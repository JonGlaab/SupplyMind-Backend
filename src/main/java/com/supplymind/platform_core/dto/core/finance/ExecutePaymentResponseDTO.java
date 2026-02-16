package com.supplymind.platform_core.dto.core.finance;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExecutePaymentResponseDTO {

    private Long supplierPaymentId;

    private String status;

    private String stripePaymentIntentId;

    private String message;
}

