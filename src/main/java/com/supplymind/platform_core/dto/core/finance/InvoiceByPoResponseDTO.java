package com.supplymind.platform_core.dto.core.finance;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record InvoiceByPoResponseDTO(
        Long invoiceId,
        Long poId,
        Long supplierId,
        String currency,
        BigDecimal totalAmount,
        BigDecimal paidAmount,
        BigDecimal remainingAmount,
        Long totalAmountCents,
        Long paidAmountCents,
        Long remainingAmountCents,
        String status,
        LocalDate dueDate,
        Instant createdAt,
        Instant approvedAt,
        Instant paidAt
) {}

