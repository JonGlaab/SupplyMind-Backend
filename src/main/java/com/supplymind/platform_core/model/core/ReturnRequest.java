package com.supplymind.platform_core.model.core;

import com.supplymind.platform_core.common.enums.ReturnStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "return_requests", indexes = {
        @Index(name = "idx_return_po_id", columnList = "po_id"),
        @Index(name = "idx_return_status", columnList = "status")
})
public class ReturnRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "return_id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "po_id", nullable = false)
    private PurchaseOrder po;

    @Lob
    @Column(name = "reason", nullable = false, columnDefinition = "LONGTEXT")
    private String reason;


    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private ReturnStatus status = ReturnStatus.REQUESTED;

    @Column(name = "requested_by")
    private String requestedBy;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private Instant requestedAt = Instant.now();

    @Column(name = "approved_by")
    private String approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "received_at")
    private Instant receivedAt;

    @Column(name = "refunded_at")
    private Instant refundedAt;

    @OneToMany(mappedBy = "returnRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReturnLineItem> items = new ArrayList<>();

    public void addItem(ReturnLineItem item) {
        items.add(item);
        item.setReturnRequest(this);
    }
}
