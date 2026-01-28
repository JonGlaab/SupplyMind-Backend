package com.supplymind.platform_core.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "demand_forecasting",indexes = {
        @Index(name = "product_id", columnList = "product_id"),
        @Index(name = "warehouse_id", columnList = "warehouse_id")
})
public class DemandForecasting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "forecast_id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    @Column(name = "forecasted_qty")
    private Integer forecastedQty;

    @Column(name = "target_date")
    private LocalDate targetDate;

    @Column(name = "conf_score", precision = 15, scale = 2)
    private BigDecimal confScore;

}