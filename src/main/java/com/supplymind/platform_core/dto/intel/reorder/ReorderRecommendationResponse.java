package com.supplymind.platform_core.dto.intel.reorder;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReorderRecommendationResponse {
    private Long productId;
    private String sku;
    private int currentStock;
    private int predictedDemandNext30Days;

    private boolean reorderNeeded;
    private int suggestedReorderQuantity;
    private String reasoning;
}