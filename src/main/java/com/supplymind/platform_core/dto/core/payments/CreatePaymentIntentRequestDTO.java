package com.supplymind.platform_core.dto.core.payments;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CreatePaymentIntentRequestDTO {
    private Long poId;
    private String currency;       // optional: "cad"    // CAD dollars
    private String paymentType;    // CARD / BANK_TRANSFER (your field)
    private BigDecimal amountOverride; // optional

}

