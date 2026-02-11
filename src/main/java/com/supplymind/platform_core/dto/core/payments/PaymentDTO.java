package com.supplymind.platform_core.dto.core.payments;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class PaymentDTO {
    private Long id;
    private String status;
    private BigDecimal amount;
    private BigDecimal refundAmount;
    private String currency;
    private String paymentType;
}

