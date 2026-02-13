package com.supplymind.platform_core.dto.mobile;

import com.supplymind.platform_core.common.enums.ScanType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScanAnalysisResponse {
    private ScanType scanType;

    private String message;

    // IF PO MATCHED:
    private Long poId;
    private String supplierName;
    private int totalItems;

    // IF PRODUCT MATCHED:
    private Long productId;
    private String productSku;
    private String productName;
    private String category;
}