package com.supplymind.platform_core.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "returns", schema = "defaultdb", indexes = {
        @Index(name = "po_id", columnList = "po_id")
})
public class Return {
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