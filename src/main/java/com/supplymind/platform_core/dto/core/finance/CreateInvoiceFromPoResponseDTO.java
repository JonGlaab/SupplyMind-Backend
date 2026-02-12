package com.supplymind.platform_core.dto.core.finance;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class CreateInvoiceFromPoResponseDTO {
    private Long invoiceId;
    private Long poId;
    private Long supplierId;
    private BigDecimal totalAmount;
    private String status;
}
