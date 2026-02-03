package com.supplymind.platform_core.model.core;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.supplymind.platform_core.model.intel.DemandForecasting;
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
@Table(name = "warehouses")
@SQLDelete(sql = "UPDATE warehouses SET is_deleted = true WHERE warehouse_id = ?")
@Where(clause = "is_deleted = false")
public class Warehouse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @NotNull
    @Size(max = 255)
    @Column(name = "location_name", nullable = false)
    private String locationName;

    @Column(name = "address")
    private String address;

    @Column(name = "capacity")
    private Integer capacity;

    // Soft delete
    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private boolean deleted = false;

    // Audit timestamps
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    // Relationships (hidden in JSON to prevent recursion)
    @JsonIgnore
    @OneToMany(mappedBy = "warehouse")
    @Builder.Default
    private Set<DemandForecasting> demandForecastings = new LinkedHashSet<>();

    @JsonIgnore
    @OneToMany(mappedBy = "warehouse")
    @Builder.Default
    private Set<Inventory> inventories = new LinkedHashSet<>();

    @JsonIgnore
    @OneToMany(mappedBy = "warehouse")
    @Builder.Default
    private Set<InventoryTransaction> inventoryTransactions = new LinkedHashSet<>();

    @JsonIgnore
    @OneToMany(mappedBy = "warehouse")
    @Builder.Default
    private Set<PurchaseOrder> purchaseOrders = new LinkedHashSet<>();

    @JsonIgnore
    @OneToMany(mappedBy = "warehouse")
    @Builder.Default
    private Set<SalesOrder> salesOrders = new LinkedHashSet<>();
}
