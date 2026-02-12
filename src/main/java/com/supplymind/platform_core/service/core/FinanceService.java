package com.supplymind.platform_core.service.core;

import com.supplymind.platform_core.dto.core.finance.*;

public interface FinanceService {

    CreateInvoiceFromPoResponseDTO createInvoiceFromPo(Long poId);

    void approveInvoice(Long invoiceId);

    Long scheduleSupplierPayment(ScheduleSupplierPaymentRequestDTO dto);

    SupplierFinanceSummaryDTO getSupplierSummary(Long supplierId);
}
