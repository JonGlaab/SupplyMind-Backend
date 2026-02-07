package com.supplymind.platform_core.dto.core.returns;


import lombok.Data;
import java.util.List;

@Data
public class ReceiveReturnDTO {
    private String receivedBy;
    private List<ReceiveReturnLineDTO> items;
}

