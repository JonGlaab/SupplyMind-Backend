package com.supplymind.platform_core.service.impl.core;

import com.supplymind.platform_core.common.enums.PurchaseOrderStatus;
import com.supplymind.platform_core.common.enums.SupplierInvoiceStatus;
import com.supplymind.platform_core.common.enums.SupplierPaymentStatus;
import com.supplymind.platform_core.dto.core.finance.*;
import com.supplymind.platform_core.model.core.PurchaseOrder;
import com.supplymind.platform_core.model.core.Supplier;
import com.supplymind.platform_core.model.core.SupplierInvoice;
import com.supplymind.platform_core.model.core.SupplierPayment;
import com.supplymind.platform_core.repository.core.PurchaseOrderRepository;
import com.supplymind.platform_core.repository.core.SupplierInvoiceRepository;
import com.supplymind.platform_core.repository.core.SupplierPaymentRepository;
import com.supplymind.platform_core.repository.core.SupplierRepository;
import com.supplymind.platform_core.service.core.FinanceService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.supplymind.platform_core.dto.core.finance.FinanceReadyPoDTO;
import com.supplymind.platform_core.common.enums.PurchaseOrderStatus;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.net.RequestOptions;
import com.stripe.param.PaymentIntentCreateParams;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class FinanceServiceImpl implements FinanceService {

    private final SupplierInvoiceRepository invoiceRepo;
    private final SupplierPaymentRepository supplierPaymentRepo;
    private final PurchaseOrderRepository poRepo;
    private final SupplierInvoiceRepository supplierInvoiceRepo;
    private final SupplierRepository supplierRepository;

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
    public List<FinanceReadyPoDTO> getReadyPos() {

        var statuses = java.util.List.of(
                PurchaseOrderStatus.DELIVERED,
                PurchaseOrderStatus.COMPLETED
        );

        return poRepo.findByStatusIn(statuses).stream()
                .map(po -> FinanceReadyPoDTO.builder()
                        .poId(po.getPoId())
                        .status(po.getStatus().name())
                        .totalAmount(po.getTotalAmount())
                        .supplierId(po.getSupplier() != null ? po.getSupplier().getSupplierId() : null)
                        .supplierName(po.getSupplier() != null ? po.getSupplier().getName() : null)
                        .warehouseId(po.getWarehouse() != null ? po.getWarehouse().getWarehouseId() : null)
                        .warehouseName(po.getWarehouse() != null ? po.getWarehouse().getLocationName() : null)
                        .build())
                .toList();
    }



    @Override
    @Transactional(readOnly = true)
    public InvoiceByPoResponseDTO getInvoiceByPoId(Long poId) {

        SupplierInvoice inv = invoiceRepo.findByPo_PoId(poId)
                .orElseThrow(() -> new IllegalArgumentException("No invoice exists for PO: " + poId));

        return new InvoiceByPoResponseDTO(
                inv.getInvoiceId(),
                inv.getPo().getPoId(),
                inv.getSupplier().getSupplierId(),
                inv.getCurrency(),
                inv.getTotalAmount(),
                inv.getPaidAmount(),
                inv.getRemainingAmount(),
                inv.getTotalAmountCents(),
                inv.getPaidAmountCents(),
                inv.getRemainingAmountCents(),
                inv.getStatus().name(),
                inv.getDueDate(),
                inv.getCreatedAt(),
                inv.getApprovedAt(),
                inv.getPaidAt()
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
    public ExecutePaymentResponseDTO executePayment(Long supplierPaymentId) {

        SupplierPayment sp = supplierPaymentRepo.findById(supplierPaymentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "SupplierPayment not found: " + supplierPaymentId));

        if (sp.getStatus() != SupplierPaymentStatus.SCHEDULED &&
                sp.getStatus() != SupplierPaymentStatus.PROCESSING) {

            throw new IllegalStateException(
                    "Payment must be SCHEDULED or PROCESSING to execute.");
        }

        SupplierInvoice invoice = sp.getInvoice();
        PurchaseOrder po = invoice.getPo();

        BigDecimal amount = sp.getAmount();

        long amountCents = amount.movePointRight(2).longValueExact();

        try {

            sp.setStatus(SupplierPaymentStatus.PROCESSING);
            supplierPaymentRepo.save(sp);

            PaymentIntentCreateParams params =
                    PaymentIntentCreateParams.builder()
                            .setAmount(amountCents)
                            .setCurrency("cad")
                            .putMetadata("supplierPaymentId",
                                    String.valueOf(sp.getSupplierPaymentId()))
                            .putMetadata("invoiceId",
                                    String.valueOf(invoice.getInvoiceId()))
                            .putMetadata("poId",
                                    String.valueOf(po.getPoId()))
                            .build();

            RequestOptions opts = RequestOptions.builder()
                    .setIdempotencyKey("exec_pay_" + sp.getSupplierPaymentId())
                    .build();

            PaymentIntent pi = PaymentIntent.create(params, opts);

            sp.setStripePaymentIntentId(pi.getId());

            if ("succeeded".equals(pi.getStatus())) {

                sp.setStatus(SupplierPaymentStatus.PAID);
                sp.setExecutedAt(Instant.now());

                BigDecimal paid =
                        invoice.getPaidAmount().add(amount);

                invoice.setPaidAmount(paid);

                if (paid.compareTo(invoice.getTotalAmount()) >= 0) {
                    invoice.setStatus(SupplierInvoiceStatus.PAID);
                } else {
                    invoice.setStatus(SupplierInvoiceStatus.PARTIALLY_PAID);
                }

                supplierInvoiceRepo.save(invoice);

                supplierPaymentRepo.save(sp);

                return ExecutePaymentResponseDTO.builder()
                        .supplierPaymentId(sp.getSupplierPaymentId())
                        .status("PAID")
                        .stripePaymentIntentId(pi.getId())
                        .message("Payment completed successfully")
                        .build();
            }

            supplierPaymentRepo.save(sp);

            return ExecutePaymentResponseDTO.builder()
                    .supplierPaymentId(sp.getSupplierPaymentId())
                    .status("PROCESSING")
                    .stripePaymentIntentId(pi.getId())
                    .message("Payment is processing in Stripe")
                    .build();

        } catch (StripeException e) {

            sp.setStatus(SupplierPaymentStatus.FAILED);
            sp.setFailureReason(e.getMessage());

            supplierPaymentRepo.save(sp);

            return ExecutePaymentResponseDTO.builder()
                    .supplierPaymentId(sp.getSupplierPaymentId())
                    .status("FAILED")
                    .message(e.getMessage())
                    .build();
        }
    }


    @Override
    public List<SupplierPayment> getPaymentsByInvoice(Long invoiceId) {
        return supplierPaymentRepo.findByInvoice_InvoiceIdOrderBySupplierPaymentIdDesc(invoiceId);
    }

    @Override
    public List<SupplierPaymentTimelineItemDTO> getSupplierPaymentTimeline(Long supplierId) {
        return supplierPaymentRepo.findBySupplier_SupplierIdOrderByCreatedAtDesc(supplierId)
                .stream()
                .map(sp -> SupplierPaymentTimelineItemDTO.builder()
                        .supplierPaymentId(sp.getSupplierPaymentId())
                        .supplierId(sp.getSupplier().getSupplierId())
                        .poId(sp.getPo().getPoId())
                        .invoiceId(sp.getInvoice().getInvoiceId())
                        .status(sp.getStatus())
                        .amount(sp.getAmount())
                        .currency(sp.getCurrency())
                        .createdAt(sp.getCreatedAt())
                        .scheduledFor(sp.getScheduledFor())
                        .executedAt(sp.getExecutedAt())
                        .completedAt(sp.getCompletedAt())
                        .stripePaymentIntentId(sp.getStripePaymentIntentId())
                        .retryCount(sp.getRetryCount())
                        .failureReason(sp.getFailureReason())
                        .build())
                .toList();
    }

    @Override
    @Transactional
    public Long scheduleSupplierPayment(ScheduleSupplierPaymentRequestDTO dto) {

        SupplierInvoice inv = invoiceRepo.findById(dto.getInvoiceId())
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + dto.getInvoiceId()));

        if (inv.getStatus() != SupplierInvoiceStatus.APPROVED && inv.getStatus() != SupplierInvoiceStatus.SCHEDULED) {
            throw new IllegalStateException("Invoice must be APPROVED to schedule payment.");
        }

        BigDecimal remaining = inv.getRemainingAmount();
        if (remaining == null || remaining.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Invoice remaining amount must be > 0.");
        }

        BigDecimal amountToPay = (dto.getAmount() == null) ? remaining : dto.getAmount();

        if (amountToPay == null || amountToPay.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be > 0");
        }

        // Cap to remaining amount
        if (amountToPay.compareTo(remaining) > 0) {
            amountToPay = remaining;
        }

        // Convert to cents (safe rounding)
        long amountCents = amountToPay
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();

        SupplierPayment sp = SupplierPayment.builder()
                .invoice(inv)
                .po(inv.getPo())
                .supplier(inv.getSupplier())
                .amount(amountToPay)
                .amountCents(amountCents)
                .currency(inv.getCurrency() == null ? "cad" : inv.getCurrency())
                .status(SupplierPaymentStatus.SCHEDULED)
                .scheduledFor(dto.getScheduledFor() == null ? Instant.now() : dto.getScheduledFor())
                .retryCount(0)
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

    @Transactional
    public void demoEnable(Long supplierId) {

        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found"));

        // if already enabled → do nothing
        if (supplier.getStripeConnectedAccountId() != null) {
            return;
        }

        // create fake Stripe Connect account for demo
        String fakeAccountId = "acct_demo_" + supplierId + "_" + System.currentTimeMillis();

        supplier.setStripeConnectedAccountId(fakeAccountId);

        supplierRepository.save(supplier);
    }
}