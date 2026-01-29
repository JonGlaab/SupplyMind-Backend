package com.supplymind.platform_core.model;

import com.supplymind.platform_core.model.core.PurchaseOrder;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "return_requests", schema = "defaultdb", indexes = {
        @Index(name = "po_id", columnList = "po_id")
})
public class ReturnRequest{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "return_id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "po_id")
    private PurchaseOrder po;

    @Lob
    @Column(name = "reason")
    private String reason;

    @Lob
    @Column(name = "status")
    private String status;

    @Column(name = "received_at")
    private Instant receivedAt;
}