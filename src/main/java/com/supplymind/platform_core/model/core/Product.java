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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "products", uniqueConstraints = {
        @UniqueConstraint(name = "sku", columnNames = {"sku"})
})
@SQLDelete(sql = "UPDATE products SET is_deleted = true WHERE product_id = ?")
@Where(clause = "is_deleted = false")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Size(max = 50)
    @NotNull
    @Column(name = "sku", nullable = false, length = 50, unique = true)
    private String sku;

    @Size(max = 255)
    @NotNull
    @Column(name = "name", nullable = false)
    private String name;

    @Size(max = 100)
    @Column(name = "category", length = 100)
    private String category;

    @Size(max = 1000)
    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "unit_price", precision = 15, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "reorder_point")
    private Integer reorderPoint;

    // --- Soft delete ---
    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;

    // --- Audit fields ---
    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // -------------------------
    // Relationships
    // -------------------------

    @JsonIgnore
    @OneToMany(mappedBy = "product")
    @Builder.Default
    private Set<DemandForecasting> demandForecastings = new LinkedHashSet<>();

    @JsonIgnore
    @OneToMany(mappedBy = "product")
    @Builder.Default
    private Set<Inventory> inventories = new LinkedHashSet<>();

    @JsonIgnore
    @OneToMany(mappedBy = "product")
    @Builder.Default
    private Set<InventoryTransaction> inventoryTransactions = new LinkedHashSet<>();

    @JsonIgnore
    @OneToMany(mappedBy = "product")
    @Builder.Default
    private Set<PurchaseOrderItem> purchaseOrderItems = new LinkedHashSet<>();

    @JsonIgnore
    @OneToMany(mappedBy = "product")
    @Builder.Default
    private Set<SalesOrderItem> salesOrderItems = new LinkedHashSet<>();

    @JsonIgnore
    @OneToMany(mappedBy = "product")
    @Builder.Default
    private Set<SupplierProduct> supplierProducts = new LinkedHashSet<>();
}
