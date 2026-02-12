package com.supplymind.platform_core.dto.core.finance;

import com.supplymind.platform_core.common.enums.SupplierPaymentStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class SupplierPaymentTimelineItemDTO {
    private Long supplierPaymentId;
    private Long supplierId;

    private Long poId;
    private Long invoiceId;

    private SupplierPaymentStatus status;

    private BigDecimal amount;
    private String currency;

    private Instant createdAt;
    private Instant scheduledFor;
    private Instant executedAt;
    private Instant completedAt;

    private String stripePaymentIntentId;

    private Integer retryCount;
    private String failureReason;
}

