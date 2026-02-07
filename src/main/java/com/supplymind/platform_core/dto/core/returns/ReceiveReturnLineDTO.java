package com.supplymind.platform_core.dto.core.returns;

import lombok.Data;

@Data
public class ReceiveReturnLineDTO {
    private Long returnLineId;
    private Integer qtyReceivedNow; // partial allowed
}
