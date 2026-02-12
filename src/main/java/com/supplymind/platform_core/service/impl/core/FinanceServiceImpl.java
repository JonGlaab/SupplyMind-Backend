package com.supplymind.platform_core.service.impl.core;

import com.supplymind.platform_core.common.enums.PurchaseOrderStatus;
import com.supplymind.platform_core.common.enums.SupplierInvoiceStatus;
import com.supplymind.platform_core.common.enums.SupplierPaymentStatus;
import com.supplymind.platform_core.dto.core.finance.*;
import com.supplymind.platform_core.model.core.PurchaseOrder;
import com.supplymind.platform_core.model.core.SupplierInvoice;
import com.supplymind.platform_core.model.core.SupplierPayment;
import com.supplymind.platform_core.repository.core.PurchaseOrderRepository;
import com.supplymind.platform_core.repository.core.SupplierInvoiceRepository;
import com.supplymind.platform_core.repository.core.SupplierPaymentRepository;
import com.supplymind.platform_core.service.core.FinanceService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class FinanceServiceImpl implements FinanceService {

    private final SupplierInvoiceRepository invoiceRepo;
    private final SupplierPaymentRepository supplierPaymentRepo;
    private final PurchaseOrderRepository poRepo;

    @Value("${stripe.currency:cad}")
    private String defaultCurrency;

    @Override
    @Transactional
    public CreateInvoiceFromPoResponseDTO createInvoiceFromPo(Long poId) {

        PurchaseOrder po = poRepo.findById(poId)
                .orElseThrow(() -> new IllegalArgumentException("PO not found: " + poId));

        PurchaseOrderStatus status = po.getStatus();

        if (status != PurchaseOrderStatus.DELIVERED
                && status != PurchaseOrderStatus.COMPLETED) {

            throw new IllegalStateException(
                    "Cannot create invoice. PO status must be DELIVERED or COMPLETED. Current status: " + status
            );
        }



        invoiceRepo.findByPo_PoId(poId).ifPresent(existing -> {
            throw new IllegalStateException("Invoice already exists for PO: " + poId);
        });

        BigDecimal total = po.getTotalAmount();
        if (total == null || total.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("PO totalAmount must be > 0");
        }

        SupplierInvoice inv = SupplierInvoice.builder()
                .po(po)
                .supplier(po.getSupplier())
                .currency(defaultCurrency.toLowerCase())
                .totalAmount(total)
                .paidAmount(BigDecimal.ZERO)
                .remainingAmount(total)
                .status(SupplierInvoiceStatus.PENDING_APPROVAL)
                .dueDate(LocalDate.now().plusDays(30))
                .build();

        inv = invoiceRepo.save(inv);

        return new CreateInvoiceFromPoResponseDTO(
                inv.getInvoiceId(),
                po.getPoId(),
                po.getSupplier().getSupplierId(),
                inv.getTotalAmount(),
                inv.getStatus().name()
        );
    }

    @Override
    @Transactional
    public void approveInvoice(Long invoiceId) {
        SupplierInvoice inv = invoiceRepo.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + invoiceId));

        if (inv.getStatus() != SupplierInvoiceStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Invoice must be PENDING_APPROVAL to approve.");
        }

        inv.setStatus(SupplierInvoiceStatus.APPROVED);
        inv.setApprovedAt(Instant.now());
        invoiceRepo.save(inv);
    }

    @Override
    @Transactional
    public Long scheduleSupplierPayment(ScheduleSupplierPaymentRequestDTO dto) {
        SupplierInvoice inv = invoiceRepo.findById(dto.getInvoiceId())
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + dto.getInvoiceId()));

        if (inv.getStatus() != SupplierInvoiceStatus.APPROVED && inv.getStatus() != SupplierInvoiceStatus.SCHEDULED) {
            throw new IllegalStateException("Invoice must be APPROVED to schedule payment.");
        }

        BigDecimal amountToPay = (dto.getAmount() == null) ? inv.getRemainingAmount() : dto.getAmount();

        if (amountToPay == null || amountToPay.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("amount must be > 0");

        if (amountToPay.compareTo(inv.getRemainingAmount()) > 0) {
            amountToPay = inv.getRemainingAmount();
        }

        SupplierPayment sp = SupplierPayment.builder()
                .invoice(inv)
                .po(inv.getPo())
                .supplier(inv.getSupplier())
                .amount(amountToPay)
                .currency(inv.getCurrency())
                .status(SupplierPaymentStatus.SCHEDULED)
                .scheduledFor(dto.getScheduledFor() == null ? Instant.now() : dto.getScheduledFor())
                .build();

        sp = supplierPaymentRepo.save(sp);

        inv.setStatus(SupplierInvoiceStatus.SCHEDULED);
        invoiceRepo.save(inv);

        return sp.getSupplierPaymentId();
    }

    @Override
    public SupplierFinanceSummaryDTO getSupplierSummary(Long supplierId) {
        var invoices = invoiceRepo.findBySupplier_SupplierId(supplierId);

        BigDecimal totalPaid = invoices.stream()
                .map(SupplierInvoice::getPaidAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal pending = invoices.stream()
                .map(SupplierInvoice::getRemainingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal overdue = invoices.stream()
                .filter(i -> i.getDueDate() != null && i.getDueDate().isBefore(LocalDate.now()))
                .map(SupplierInvoice::getRemainingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        double avgDelay = invoices.stream()
                .filter(i -> i.getPaidAt() != null && i.getCreatedAt() != null)
                .mapToDouble(i -> Duration.between(i.getCreatedAt(), i.getPaidAt()).toDays())
                .average().orElse(0.0);

        return new SupplierFinanceSummaryDTO(supplierId, totalPaid, pending, overdue, avgDelay, invoices.size());
    }
}
