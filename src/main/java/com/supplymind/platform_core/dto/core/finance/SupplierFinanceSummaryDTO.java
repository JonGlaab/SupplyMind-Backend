package com.supplymind.platform_core.dto.core.finance;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class SupplierFinanceSummaryDTO {
    private Long supplierId;
    private BigDecimal totalPaid;
    private BigDecimal pending;
    private BigDecimal overdue;
    private double avgPaymentDelayDays;
    private long invoiceCount;
}
