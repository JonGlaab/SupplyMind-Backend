package com.supplymind.platform_core.dto.intel.forecast;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ForecastResponse {
    private Long productId;
    private int predictedDemandNext30Days;
    private String trend;
    private List<DataPoint> history;

    @Data
    @AllArgsConstructor
    public static class DataPoint {
        private String date;
        private Integer quantity;
    }
}