package com.supplymind.platform_core.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "warehouses")
public class Warehouse {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "warehouse_id", nullable = false)
    private Long id;

    @Size(max = 255)
    @Column(name = "location_name")
    private String locationName;


    @Column(name = "address")
    private String address;

    @Column(name = "capacity")
    private Integer capacity;

    @OneToMany(mappedBy = "warehouse")
    private Set<DemandForecasting> demandForecastings = new LinkedHashSet<>();

    @OneToMany(mappedBy = "warehouse")
    private Set<Inventory> inventories = new LinkedHashSet<>();

    @OneToMany(mappedBy = "warehouse")
    private Set<InventoryTransaction> inventoryTransactions = new LinkedHashSet<>();

    @OneToMany(mappedBy = "warehouse")
    private Set<PurchaseOrder> purchaseOrders = new LinkedHashSet<>();

    @OneToMany(mappedBy = "warehouse")
    private Set<SalesOrder> salesOrders = new LinkedHashSet<>();

}