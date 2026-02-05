package com.supplymind.platform_core.service.communication;
import com.supplymind.platform_core.dto.core.purchaseorder.InboxMessage;
import java.util.List;

public interface InboxProvider {
    /**
     * Ensures a label/folder exists for the given name.
     * @return The ID of the label.
     */
    String getOrCreateLabel(String labelName);

    /**
     * Fetches all messages from a specific label.
     * Returns clean DTOs, hiding the email provider's complexity.
     */
    List<InboxMessage> fetchMessages(String labelId);
}