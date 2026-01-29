package com.supplymind.platform_core.model.core;

import com.supplymind.platform_core.model.ReturnRequest;
import com.supplymind.platform_core.model.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "purchase_orders",indexes = {
        @Index(name = "supplier_id", columnList = "supplier_id"),
        @Index(name = "warehouse_id", columnList = "warehouse_id"),
        @Index(name = "buyer_id", columnList = "buyer_id")
})
public class PurchaseOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "po_id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id")
    private User buyer;


    @Column(name = "status")
    private String status;

    @Column(name = "total_amount", precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_on")
    private Instant createdOn;

    @OneToMany(mappedBy = "po")
    private Set<Payment> payments = new LinkedHashSet<>();

    @OneToMany(mappedBy = "po")
    private Set<PurchaseOrderItem> purchaseOrderItems = new LinkedHashSet<>();

    @OneToMany(mappedBy = "po")
    private Set<ReturnRequest> returns = new LinkedHashSet<>();

}