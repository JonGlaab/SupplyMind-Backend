package com.supplymind.platform_core.model.core;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "return_receipt_items", schema = "defaultdb", indexes = {
        @Index(name = "idx_rri_receipt_id", columnList = "return_receipt_id"),
        @Index(name = "idx_rri_return_line_id", columnList = "return_line_id")
})
public class ReturnReceiptItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "return_receipt_item_id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "return_receipt_id", nullable = false)
    private ReturnReceipt receipt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "return_line_id", nullable = false)
    private ReturnLineItem returnLine;

    @Column(name = "qty_received_now", nullable = false)
    private Integer qtyReceivedNow;


}
