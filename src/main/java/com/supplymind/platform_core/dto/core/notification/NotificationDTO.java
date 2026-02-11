package com.supplymind.platform_core.dto.core.notification;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;

@Data
@Builder
public class NotificationDTO {
    private Long id;
    private String title;
    private String message;
    private String type;        // INFO, WARNING, etc.
    private Long referenceId;   // PO ID
    private String referenceType;
    private boolean isRead;
    private Instant createdAt;
    private String timeAgo;
}