package com.supplymind.platform_core.dto.core.purchaseorder;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EmailDraftResponse {
    private String subject;
    private String body;
    private String toEmail;
}