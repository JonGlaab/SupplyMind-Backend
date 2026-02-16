package com.supplymind.platform_core.model.core;

import com.supplymind.platform_core.common.enums.SupplierInvoiceStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "supplier_invoices")
public class SupplierInvoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "invoice_id", nullable = false)
    private Long invoiceId;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "po_id", nullable = false)
    private PurchaseOrder po;

    @Builder.Default
    @Column(nullable = false)
    private String currency = "cad";

    // ---- DECIMAL AMOUNTS ----
    @Column(name = "total_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    @Builder.Default
    @Column(name = "paid_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(name = "remaining_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal remainingAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SupplierInvoiceStatus status;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "paid_at")
    private Instant paidAt;

    // ---- CENTS (Stripe-safe) ----
    @Builder.Default
    @Column(name = "total_amount_cents", nullable = false)
    private Long totalAmountCents = 0L;

    @Builder.Default
    @Column(name = "paid_amount_cents", nullable = false)
    private Long paidAmountCents = 0L;

    @Builder.Default
    @Column(name = "remaining_amount_cents", nullable = false)
    private Long remainingAmountCents = 0L;

    @PrePersist
    @PreUpdate
    private void normalizeAndSync() {

        if (paidAmount == null) paidAmount = BigDecimal.ZERO;

        // If remainingAmount not set, calculate it
        if (totalAmount != null && remainingAmount == null) {
            remainingAmount = totalAmount.subtract(paidAmount);
        }

        // Extra safety (never null)
        if (totalAmount == null) totalAmount = BigDecimal.ZERO;
        if (remainingAmount == null) remainingAmount = BigDecimal.ZERO;

        totalAmountCents = toCents(totalAmount);
        paidAmountCents = toCents(paidAmount);
        remainingAmountCents = toCents(remainingAmount);
    }

    private long toCents(BigDecimal amount) {
        if (amount == null) return 0L;
        return amount.movePointRight(2)
                .setScale(0, RoundingMode.HALF_UP)
                .longValue();
    }
}
