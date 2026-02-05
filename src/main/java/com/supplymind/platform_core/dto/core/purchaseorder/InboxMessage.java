package com.supplymind.platform_core.dto.core.purchaseorder;
import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;
@Data
@AllArgsConstructor
public class InboxMessage {
    private String messageId;
    private String snippet;
    private long timestamp;
    private String from;
    private List<String> attachments;
}
