package com.supplymind.platform_core.dto.core.finance;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ConnectOnboardingLinkResponseDTO {
    private Long supplierId;
    private String connectStatus;
    private String url;
}
