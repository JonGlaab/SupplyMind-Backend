package com.supplymind.platform_core.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "supplier_products",indexes = {
        @Index(name = "supplier_id", columnList = "supplier_id"),
        @Index(name = "product_id", columnList = "product_id")
})
public class SupplierProduct {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "lead_time_days")
    private Integer leadTimeDays;

    @Column(name = "cost_price", precision = 15, scale = 2)
    private BigDecimal costPrice;

}