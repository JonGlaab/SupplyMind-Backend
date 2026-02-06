package com.supplymind.platform_core.dto.core.returns;

import lombok.Data;
import java.util.List;

@Data
public class ApproveReturnDTO {
    private String approvedBy;
    private List<ApproveReturnLineDTO> items;
}
