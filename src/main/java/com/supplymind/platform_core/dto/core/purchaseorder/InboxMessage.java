package com.supplymind.platform_core.dto.core.purchaseorder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class InboxMessage {
    private String messageId;
    private String subject;
    private String from;
    private String body;
    private String snippet;
    private long timestamp;
    private List<String> attachments = new ArrayList<>();
}
