package com.supplymind.platform_core.dto.intel.email;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;

@Data
@Builder
public class EmailMessage {
    private String id;
    private String threadId;
    private String sender;
    private String subject;
    private String bodyPreview;
    private String fullBody;
    private Instant date;
    private boolean isMe;
}