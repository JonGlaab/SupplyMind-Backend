package com.supplymind.platform_core.dto.core.returns;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class RefundReturnDTO {
    private String refundedBy;
    private BigDecimal refundAmount;
    private String refundReference; // transaction id / reference
}
