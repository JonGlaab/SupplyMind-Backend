package com.supplymind.platform_core.model.core;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "supplier_products",
        uniqueConstraints = @UniqueConstraint(columnNames = {"supplier_id", "product_id"}),
        indexes = {
                @Index(name = "idx_supplier_products_supplier_id", columnList = "supplier_id"),
                @Index(name = "idx_supplier_products_product_id", columnList = "product_id")
        }
)
public class SupplierProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "lead_time_days")
    private Integer leadTimeDays;

    @Column(name = "cost_price", precision = 15, scale = 2)
    private BigDecimal costPrice;
}
