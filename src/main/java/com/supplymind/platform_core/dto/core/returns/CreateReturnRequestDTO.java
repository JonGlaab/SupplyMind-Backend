package com.supplymind.platform_core.dto.core.returns;

import lombok.Data;
import java.util.List;

@Data
public class CreateReturnRequestDTO {
    private Long poId;
    private String reason;
    private String requestedBy;
    private List<CreateReturnLineDTO> items;
}
