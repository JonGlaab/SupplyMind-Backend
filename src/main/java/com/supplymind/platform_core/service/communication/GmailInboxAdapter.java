package com.supplymind.platform_core.service.communication;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.*;
import com.supplymind.platform_core.dto.core.purchaseorder.InboxMessage;
import com.supplymind.platform_core.service.communication.InboxProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

@Component
@RequiredArgsConstructor
public class GmailInboxAdapter implements InboxProvider {

    private final Gmail gmail;

    @Override
    public String getOrCreateLabel(String labelName) {
        try {
            ListLabelsResponse response = gmail.users().labels().list("me").execute();
            Optional<Label> existing = response.getLabels().stream()
                    .filter(l -> l.getName().equalsIgnoreCase(labelName))
                    .findFirst();

            if (existing.isPresent()) return existing.get().getId();

            Label label = new Label().setName(labelName)
                    .setLabelListVisibility("labelShow")
                    .setMessageListVisibility("show");

            return gmail.users().labels().create("me", label).execute().getId();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public List<InboxMessage> fetchMessages(String labelId) {
        List<InboxMessage> messages = new ArrayList<>();
        if (labelId == null) return messages;

        try {
            // 1. List Message IDs
            ListMessagesResponse listResponse = gmail.users().messages().list("me")
                    .setLabelIds(Collections.singletonList(labelId))
                    .execute();

            if (listResponse.getMessages() == null) return messages;

            // 2. Fetch Full Details for each
            for (Message msgHeader : listResponse.getMessages()) {
                Message fullMsg = gmail.users().messages().get("me", msgHeader.getId()).execute();

                messages.add(mapToDto(fullMsg));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return messages;
    }

    // Helper: Converts Raw Gmail Message -> Clean DTO
    private InboxMessage mapToDto(Message msg) {
        String snippet = msg.getSnippet();
        long timestamp = msg.getInternalDate();
        String sender = "Unknown";
        List<String> attachments = new ArrayList<>();

        // Parse Headers (From)
        if (msg.getPayload().getHeaders() != null) {
            sender = msg.getPayload().getHeaders().stream()
                    .filter(h -> h.getName().equalsIgnoreCase("From"))
                    .findFirst()
                    .map(MessagePartHeader::getValue)
                    .orElse("Unknown");
        }

        // Parse Attachments (Deep scan of payload parts)
        if (msg.getPayload().getParts() != null) {
            for (MessagePart part : msg.getPayload().getParts()) {
                if (part.getFilename() != null && !part.getFilename().isEmpty()) {
                    attachments.add(part.getFilename());
                }
            }
        }

        return new InboxMessage(msg.getId(), snippet, timestamp, sender, attachments);
    }
}