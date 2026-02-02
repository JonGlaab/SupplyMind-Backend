package com.supplymind.platform_core.dto.intel.forecast;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ForecastResponse {
    private Long productId;
    private int analysisPeriodDays;
    private int totalSalesInPeriod;
    private double averageDailySales;
    private int predictedDemandNext30Days;
    private String trend;
}