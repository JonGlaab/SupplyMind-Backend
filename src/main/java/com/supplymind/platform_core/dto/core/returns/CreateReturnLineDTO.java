package com.supplymind.platform_core.dto.core.returns;

import lombok.Data;

@Data
public class CreateReturnLineDTO {
    private Long poItemId;
    private Integer qtyReturnRequested;
    private String conditionNotes;
}
