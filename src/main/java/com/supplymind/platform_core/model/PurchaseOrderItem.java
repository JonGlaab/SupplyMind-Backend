package com.supplymind.platform_core.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "purchase_order_items", indexes = {
        @Index(name = "po_id", columnList = "po_id"),
        @Index(name = "product_id", columnList = "product_id")
})
public class PurchaseOrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "po_item_id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "po_id")
    private PurchaseOrder po;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "ordered_qty")
    private Integer orderedQty;

    @Column(name = "received_qty")
    private Integer receivedQty;

    @Column(name = "unit_cost", precision = 15, scale = 2)
    private BigDecimal unitCost;

}