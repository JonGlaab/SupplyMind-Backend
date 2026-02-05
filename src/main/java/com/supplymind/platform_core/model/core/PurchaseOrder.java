package com.supplymind.platform_core.model.core;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.supplymind.platform_core.common.enums.PurchaseOrderStatus;
import com.supplymind.platform_core.model.auth.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "purchase_orders", indexes = {
        @Index(name = "idx_po_supplier_id", columnList = "supplier_id"),
        @Index(name = "idx_po_warehouse_id", columnList = "warehouse_id"),
        @Index(name = "idx_po_buyer_id", columnList = "buyer_id")
})
public class PurchaseOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "po_id", nullable = false)
    private Long poId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PurchaseOrderStatus status;

    @Column(name = "total_amount", precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "last_activity_at")
    private java.time.Instant lastActivityAt; // For sorting the Inbox list

    @Column(name = "expected_delivery_date")
    private java.time.Instant expectedDeliveryDate;

    @CreationTimestamp
    @Column(name = "created_on", updatable = false)
    private Instant createdOn;

    @JsonIgnore
    @OneToMany(mappedBy = "po", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<PurchaseOrderItem> purchaseOrderItems = new LinkedHashSet<>();

    @JsonIgnore
    @OneToMany(mappedBy = "po")
    @Builder.Default
    private Set<Payment> payments = new LinkedHashSet<>();

    @JsonIgnore
    @OneToMany(mappedBy = "po")
    @Builder.Default
    private Set<ReturnRequest> returns = new LinkedHashSet<>();
}
