package com.supplymind.platform_core.model.core;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "payment_refunds", indexes = {
        @Index(name = "idx_refund_payment_id", columnList = "payment_id"),
        @Index(name = "idx_refund_stripe_refund_id", columnList = "stripe_refund_id")
})
public class PaymentRefund {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "refund_id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    // Stripe refund id: re_...
    @Column(name = "stripe_refund_id", nullable = false, unique = true, length = 255)
    private String stripeRefundId;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    // PENDING, SUCCEEDED, FAILED
    @Column(name = "status", nullable = false, length = 50)
    private String status = "PENDING";

    // Optional but recommended (audit/UI)
    @Column(name = "reason", length = 255)
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
