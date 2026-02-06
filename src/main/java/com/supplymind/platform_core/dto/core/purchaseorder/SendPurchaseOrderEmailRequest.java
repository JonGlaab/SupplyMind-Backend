package com.supplymind.platform_core.dto.core.purchaseorder;

import lombok.Data;

@Data
public class SendPurchaseOrderEmailRequest {
    private String subject;
    private String body;
    private boolean addSignature;
}