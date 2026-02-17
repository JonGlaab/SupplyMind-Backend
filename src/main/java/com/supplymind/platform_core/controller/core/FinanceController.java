package com.supplymind.platform_core.controller.core;

import com.supplymind.platform_core.dto.core.finance.*;
import com.supplymind.platform_core.model.core.PurchaseOrder;
import com.supplymind.platform_core.model.core.SupplierInvoice;
import com.supplymind.platform_core.model.core.SupplierPayment;
import com.supplymind.platform_core.service.core.FinanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<ExecutePaymentResponseDTO> executePayment(@PathVariable Long supplierPaymentId) {

        ExecutePaymentResponseDTO res = financeService.executePayment(supplierPaymentId);

        if ("PROCESSING".equalsIgnoreCase(res.getStatus())) {
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(res); // 202
        }

        if ("FAILED".equalsIgnoreCase(res.getStatus())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(res); // 400
        }

        return ResponseEntity.ok(res); // 200 for PAID
    }



    @GetMapping("/invoices/{invoiceId}/payments")
    public List<SupplierPayment> invoicePayments(@PathVariable Long invoiceId) {
        return financeService.getPaymentsByInvoice(invoiceId);
    }

    @PostMapping("/payments/{supplierPaymentId}/intent")
    public CreatePaymentIntentResponseDTO createPaymentIntent(@PathVariable Long supplierPaymentId) {
        return financeService.createPaymentIntent(supplierPaymentId);
    }

    @PostMapping("/payments/{supplierPaymentId}/finalize")
    public ExecutePaymentResponseDTO finalizePayment(@PathVariable Long supplierPaymentId) {
        return financeService.finalizePayment(supplierPaymentId);
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
    public List<FinanceReadyPoDTO> readyPos() {
        return financeService.getReadyPos();
    }



    @GetMapping("/invoices/by-po/{poId}")
    public ResponseEntity<InvoiceByPoResponseDTO> invoiceByPo(@PathVariable Long poId) {
        try {
            return ResponseEntity.ok(financeService.getInvoiceByPoId(poId));
        } catch (IllegalArgumentException ex) {
            // No invoice for this PO yet
            return ResponseEntity.noContent().build(); // 204
        }
    }

    @PostMapping("/suppliers/{supplierId}/demo-enable")
    public ResponseEntity<Void> demoEnable(@PathVariable Long supplierId) {

        financeService.demoEnable(supplierId);

        return ResponseEntity.ok().build();
    }


}
