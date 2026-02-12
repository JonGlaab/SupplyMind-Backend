package com.supplymind.platform_core.controller.core;

import com.supplymind.platform_core.dto.core.finance.*;
import com.supplymind.platform_core.service.core.FinanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/core/finance")
public class FinanceController {

    private final FinanceService financeService;

    @PostMapping("/invoices/from-po/{poId}")
    public CreateInvoiceFromPoResponseDTO createInvoiceFromPo(@PathVariable Long poId) {
        return financeService.createInvoiceFromPo(poId);
    }

    @PostMapping("/invoices/{invoiceId}/approve")
    public void approveInvoice(@PathVariable Long invoiceId) {
        financeService.approveInvoice(invoiceId);
    }

    @PostMapping("/payments/schedule")
    public Long schedulePayment(@RequestBody ScheduleSupplierPaymentRequestDTO dto) {
        return financeService.scheduleSupplierPayment(dto);
    }

    @GetMapping("/suppliers/{supplierId}/summary")
    public SupplierFinanceSummaryDTO summary(@PathVariable Long supplierId) {
        return financeService.getSupplierSummary(supplierId);
    }
}
