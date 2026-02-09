package com.supplymind.platform_core.dto.communication;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;

@Data
@Builder
public class InboxConversation {
    private Long id;
    private String poCode;
    private String supplierName;
    private String lastMessage;
    private Instant timestamp;
    private int unreadCount;
    private String status;
}
