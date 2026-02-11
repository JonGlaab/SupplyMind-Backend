package com.supplymind.platform_core.model.core;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "payments", indexes = {
        @Index(name = "idx_payments_po_id", columnList = "po_id"),
        @Index(name = "idx_payments_stripe_id", columnList = "stripe_id")
})
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "po_id")
    private PurchaseOrder po;

    // Store Stripe PaymentIntent id here: pi_...
    @Size(max = 255)
    @Column(name = "stripe_id", length = 255)
    private String stripeId;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    // PENDING, PAID, FAILED, PARTIALLY_REFUNDED, REFUNDED
    @Column(name = "status", nullable = false, length = 50)
    private String status = "PENDING";

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "payment_type", nullable = false, length = 20)
    private String paymentType; // CARD, etc.

    // Running total of refunded money (fast UI)
    @Column(name = "refunded_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal refundedAmount = BigDecimal.ZERO;

    // Keep it lowercase: "cad"
    @Column(name = "currency", nullable = false, length = 10)
    private String currency = "cad";

    // Refund history (multiple partial refunds)
    @OneToMany(mappedBy = "payment", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<PaymentRefund> refunds = new ArrayList<>();
}
