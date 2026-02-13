package com.supplymind.platform_core.model.core;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "return_line_items", indexes = {
        @Index(name = "idx_rli_return_id", columnList = "return_id"),
        @Index(name = "idx_rli_po_item_id", columnList = "po_item_id")
})
public class ReturnLineItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "return_line_id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "return_id", nullable = false)
    private ReturnRequest returnRequest;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "po_item_id", nullable = false)
    private PurchaseOrderItem poItem;

    @Column(name = "ordered_qty", nullable = false)
    private Integer orderedQty;

    @Column(name = "received_qty_on_po", nullable = false)
    private Integer receivedQtyOnPo;

    @Column(name = "qty_return_requested", nullable = false)
    private Integer qtyReturnRequested;

    @Column(name = "qty_approved", nullable = false)
    private Integer qtyApproved = 0;

    @Column(name = "qty_received", nullable = false)
    private Integer qtyReceived = 0;

    @Column(name = "unit_cost", precision = 15, scale = 2, nullable = false)
    private BigDecimal unitCost;

    @Column(name = "restock_fee", precision = 15, scale = 2, nullable = false)
    private BigDecimal restockFee = BigDecimal.ZERO;

    @Column(name = "condition_notes")
    private String conditionNotes;

    @Column(name = "evidence_url")
    private String evidenceUrl;

    public int remainingToReceive() {
        return Math.max(0, qtyApproved - qtyReceived);
    }
}
