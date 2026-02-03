package com.supplymind.platform_core.model.core;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "inventory",
        uniqueConstraints = @UniqueConstraint(columnNames = {"warehouse_id", "product_id"}),
        indexes = {
                @Index(name = "idx_inventory_warehouse_id", columnList = "warehouse_id"),
                @Index(name = "idx_inventory_product_id", columnList = "product_id")
        }
)
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inventory_id", nullable = false)
    private Long inventoryId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "qty_on_hand", nullable = false)
    @Builder.Default
    private Integer qtyOnHand = 0;
}
