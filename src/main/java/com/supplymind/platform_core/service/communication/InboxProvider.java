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
    /**
     * Fetches all attachments from a specific label.
     */
    byte[] fetchAttachment(String labelId, String messageId, String fileName);
    /**
     * Moves a message from one label to another.
     */
    void moveMessage(String messageId, String sourceLabel, String targetLabel);
    /**
     * Copies a message from one label to another.
     */
    void copyMessage(String messageId, String sourceLabel, String targetLabel);

}