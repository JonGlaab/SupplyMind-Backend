package com.supplymind.platform_core.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "payments",indexes = {
        @Index(name = "po_id", columnList = "po_id")
})
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "po_id")
    private PurchaseOrder po;

    @Size(max = 255)
    @Column(name = "stripe_id")
    private String stripeId;

    @Column(name = "amount", precision = 15, scale = 2)
    private BigDecimal amount;


    @Column(name = "status")
    private String status;

    @Column(name = "paid_at")
    private Instant paidAt;


    @Column(name = "payment_type", nullable = false, length = 20)
    private String paymentType;

}