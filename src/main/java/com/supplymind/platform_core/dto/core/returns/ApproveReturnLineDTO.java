package com.supplymind.platform_core.dto.core.returns;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ApproveReturnLineDTO {
    private Long returnLineId;
    private Integer qtyApproved; // can be <= requested (partial approval)
    private BigDecimal restockFee;   // optional
}
