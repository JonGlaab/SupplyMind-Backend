package com.supplymind.platform_core.model.core;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.supplymind.platform_core.common.enums.SupplierConnectStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.Where;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "suppliers")
@SQLDelete(sql = "UPDATE suppliers SET is_deleted = true WHERE supplier_id = ?")
@Where(clause = "is_deleted = false")
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "supplier_id", nullable = false)
    private Long supplierId;

    @Size(max = 255)
    @NotNull
    @Column(name = "name", nullable = false)
    private String name;

    @Size(max = 255)
    @Column(name = "contact_email")
    private String contactEmail;

    @Size(max = 20)
    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "address")
    private String address;

    @Column(name = "stripe_connected_account_id")
    private String stripeConnectedAccountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "connect_status", nullable = false)
    @Builder.Default
    private SupplierConnectStatus connectStatus = SupplierConnectStatus.NOT_STARTED;

    // ---------- Soft delete ----------
    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;

    // ---------- Audit timestamps ----------
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    // ---------- Relationships ----------
    @JsonIgnore
    @OneToMany(mappedBy = "supplier")
    @Builder.Default
    private Set<PurchaseOrder> purchaseOrders = new LinkedHashSet<>();

    @JsonIgnore
    @OneToMany(mappedBy = "supplier")
    @Builder.Default
    private Set<SupplierProduct> supplierProducts = new LinkedHashSet<>();
}
