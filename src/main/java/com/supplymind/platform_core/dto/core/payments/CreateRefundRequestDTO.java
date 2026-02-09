package com.supplymind.platform_core.dto.core.payments;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CreateRefundRequestDTO {
    private Long paymentId;
    private BigDecimal amount; // null => full refund, otherwise partial
}

