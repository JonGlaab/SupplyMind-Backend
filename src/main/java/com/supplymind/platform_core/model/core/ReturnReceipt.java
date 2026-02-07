package com.supplymind.platform_core.model.core;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "return_receipts", schema = "defaultdb", indexes = {
        @Index(name = "idx_rr_return_id", columnList = "return_id")
})
public class ReturnReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "return_receipt_id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "return_id", nullable = false)
    private ReturnRequest returnRequest;

    @Column(name = "received_by")
    private String receivedBy;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt = Instant.now();

    @OneToMany(mappedBy = "receipt", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReturnReceiptItem> items = new ArrayList<>();

    public void addItem(ReturnReceiptItem item) {
        items.add(item);
        item.setReceipt(this);
    }
}
