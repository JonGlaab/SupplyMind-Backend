package com.supplymind.platform_core.dto.core.finance;

import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
public class ScheduleSupplierPaymentRequestDTO {
    private Long invoiceId;
    private Instant scheduledFor;   // null = now
    private BigDecimal amount;      // null = pay remaining (partial supported)
}
