package com.supplymind.platform_core.service.core;

import com.supplymind.platform_core.dto.core.finance.*;
import com.supplymind.platform_core.model.core.PurchaseOrder;
import com.supplymind.platform_core.model.core.SupplierInvoice;
import com.supplymind.platform_core.model.core.SupplierPayment;

import java.util.List;

public interface FinanceService {

    CreateInvoiceFromPoResponseDTO createInvoiceFromPo(Long poId);

    void approveInvoice(Long invoiceId);

    Long scheduleSupplierPayment(ScheduleSupplierPaymentRequestDTO dto);

    SupplierFinanceSummaryDTO getSupplierSummary(Long supplierId);

    InvoiceByPoResponseDTO getInvoiceByPoId(Long poId);

    List<FinanceReadyPoDTO> getReadyPos();

    void executePayment(Long supplierPaymentId);

    List<SupplierPayment> getPaymentsByInvoice(Long invoiceId);

    List<SupplierPaymentTimelineItemDTO> getSupplierPaymentTimeline(Long supplierId);


    public void demoEnable(Long supplierId);
}
