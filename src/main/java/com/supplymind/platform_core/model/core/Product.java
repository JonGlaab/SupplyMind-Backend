package com.supplymind.platform_core.model.core;

import com.supplymind.platform_core.model.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "products", uniqueConstraints = {
        @UniqueConstraint(name = "sku", columnNames = {"sku"})
})
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id", nullable = false)
    private Long id;

    @Size(max = 50)
    @NotNull
    @Column(name = "sku", nullable = false, length = 50)
    private String sku;

    @Size(max = 255)
    @NotNull
    @Column(name = "name", nullable = false)
    private String name;

    @Size(max = 100)
    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "unit_price", precision = 15, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "reorder_point")
    private Integer reorderPoint;

    @OneToMany(mappedBy = "product")
    private Set<DemandForecasting> demandForecastings = new LinkedHashSet<>();

    @OneToMany(mappedBy = "product")
    private Set<Inventory> inventories = new LinkedHashSet<>();

    @OneToMany(mappedBy = "product")
    private Set<InventoryTransaction> inventoryTransactions = new LinkedHashSet<>();

    @OneToMany(mappedBy = "product")
    private Set<PurchaseOrderItem> purchaseOrderItems = new LinkedHashSet<>();

    @OneToMany(mappedBy = "product")
    private Set<SalesOrderItem> salesOrderItems = new LinkedHashSet<>();

    @OneToMany(mappedBy = "product")
    private Set<SupplierProduct> supplierProducts = new LinkedHashSet<>();

}