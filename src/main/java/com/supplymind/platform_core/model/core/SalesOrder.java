package com.supplymind.platform_core.model.core;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "sales_orders",indexes = {
        @Index(name = "customer_id", columnList = "customer_id"),
        @Index(name = "warehouse_id", columnList = "warehouse_id")
})
public class SalesOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "so_id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;


    @Column(name = "status")
    private String status;

    @OneToMany(mappedBy = "so")
    private Set<SalesOrderItem> salesOrderItems = new LinkedHashSet<>();

}