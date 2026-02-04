package com.supplymind.platform_core.model.core;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "purchase_order_items", indexes = {
        @Index(name = "idx_poi_po_id", columnList = "po_id"),
        @Index(name = "idx_poi_product_id", columnList = "product_id")
})
public class PurchaseOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "po_item_id", nullable = false)
    private Long poItemId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "po_id", nullable = false)
    private PurchaseOrder po;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "ordered_qty", nullable = false)
    private Integer orderedQty;

    @Column(name = "received_qty")
    @Builder.Default
    private Integer receivedQty = 0;

    @Column(name = "unit_cost", precision = 15, scale = 2)
    private BigDecimal unitCost;
}
