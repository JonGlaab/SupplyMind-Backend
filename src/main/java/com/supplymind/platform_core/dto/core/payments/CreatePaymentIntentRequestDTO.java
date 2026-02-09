package com.supplymind.platform_core.dto.core.payments;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CreatePaymentIntentRequestDTO {
    private Long poId;
    private BigDecimal amount;     // CAD dollars
    private String paymentType;    // CARD / BANK_TRANSFER (your field)
}

