package com.supplymind.platform_core.dto.intel.reorder;

import lombok.Data;

@Data
public class ReorderRecommendationRequest {

    private Long productId;

    private Integer targetDaysSupply = 45;
}
