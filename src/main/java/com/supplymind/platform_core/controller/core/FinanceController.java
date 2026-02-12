package com.supplymind.platform_core.controller.core;

import com.supplymind.platform_core.dto.core.finance.*;
import com.supplymind.platform_core.model.core.PurchaseOrder;
import com.supplymind.platform_core.model.core.SupplierInvoice;
import com.supplymind.platform_core.model.core.SupplierPayment;
import com.supplymind.platform_core.service.core.FinanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/core/finance")
public class FinanceController {

    private final FinanceService financeService;

    @PostMapping("/invoices/from-po/{poId}")
    public CreateInvoiceFromPoResponseDTO createInvoiceFromPo(@PathVariable Long poId) {
        return financeService.createInvoiceFromPo(poId);
    }

    @PostMapping("/payments/{supplierPaymentId}/execute")
    public void executePayment(@PathVariable Long supplierPaymentId) {
        financeService.executePayment(supplierPaymentId);
    }

    @GetMapping("/invoices/{invoiceId}/payments")
    public List<SupplierPayment> invoicePayments(@PathVariable Long invoiceId) {
        return financeService.getPaymentsByInvoice(invoiceId);
    }


    @PostMapping("/invoices/{invoiceId}/approve")
    public void approveInvoice(@PathVariable Long invoiceId) {
        financeService.approveInvoice(invoiceId);
    }

    @GetMapping("/suppliers/{supplierId}/payments/timeline")
    public List<SupplierPaymentTimelineItemDTO> supplierTimeline(@PathVariable Long supplierId) {
        return financeService.getSupplierPaymentTimeline(supplierId);
    }


    @PostMapping("/payments/schedule")
    public Long schedulePayment(@RequestBody ScheduleSupplierPaymentRequestDTO dto) {
        return financeService.scheduleSupplierPayment(dto);
    }

    @GetMapping("/suppliers/{supplierId}/summary")
    public SupplierFinanceSummaryDTO summary(@PathVariable Long supplierId) {
        return financeService.getSupplierSummary(supplierId);
    }

    @GetMapping("/po/ready")
    public List<PurchaseOrder> readyPos() {
        return financeService.getReadyPos();
    }

    @GetMapping("/invoices/by-po/{poId}")
    public SupplierInvoice invoiceByPo(@PathVariable Long poId) {
        return financeService.getInvoiceByPoId(poId);
    }

}
