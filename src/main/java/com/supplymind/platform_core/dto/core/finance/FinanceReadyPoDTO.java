package com.supplymind.platform_core.dto.core.finance;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinanceReadyPoDTO {

    private Long poId;
    private String status;
    private BigDecimal totalAmount;

    private Long supplierId;
    private String supplierName;

    private Long warehouseId;
    private String warehouseName;
}

