package com.supplymind.platform_core.service.communication.adapter;

import com.supplymind.platform_core.dto.core.purchaseorder.InboxMessage;
import com.supplymind.platform_core.service.communication.InboxProvider;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMultipart;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
public class ImapInboxAdapter implements InboxProvider {

    @Value("${spring.mail.username}")
    private String username;

    @Value("${spring.mail.password}")
    private String password;

    private static final String IMAP_HOST = "imap.gmail.com";

    private Store connect() throws MessagingException {
        Properties props = new Properties();
        props.put("mail.store.protocol", "imaps");
        props.put("mail.imaps.host", IMAP_HOST);
        props.put("mail.imaps.port", "993");
        props.put("mail.imaps.ssl.enable", "true");

        Session session = Session.getInstance(props);
        Store store = session.getStore("imaps");
        store.connect(IMAP_HOST, username, password);
        return store;
    }

    @Override
    public String getOrCreateLabel(String labelName) {
        // In IMAP, Labels are just Folders.
        try (Store store = connect()) {
            Folder folder = store.getFolder(labelName);
            if (!folder.exists()) {
                boolean created = folder.create(Folder.HOLDS_MESSAGES);
                if (created) return labelName;
            }
            return labelName;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public List<InboxMessage> fetchMessages(String labelId) {
        List<InboxMessage> inboxMessages = new ArrayList<>();
        if (labelId == null) return inboxMessages;

        try (Store store = connect()) {
            Folder folder = store.getFolder(labelId); // labelId is just the folder name in IMAP
            if (!folder.exists()) return inboxMessages;

            folder.open(Folder.READ_ONLY);

            // Fetch last 20 messages to keep it fast
            int count = folder.getMessageCount();
            int start = Math.max(1, count - 20);
            Message[] messages = folder.getMessages(start, count);

            for (Message msg : messages) {
                try {
                    inboxMessages.add(mapToDto(msg));
                } catch (Exception e) {
                    System.err.println("Error parsing email: " + e.getMessage());
                }
            }

            folder.close(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return inboxMessages;
    }

    // --- Helpers to parse ugly Email formats ---

    private InboxMessage mapToDto(Message msg) throws MessagingException, java.io.IOException {
        String messageId = String.valueOf(msg.getMessageNumber()); // Simple ID for now
        long timestamp = msg.getSentDate() != null ? msg.getSentDate().getTime() : System.currentTimeMillis();

        // Get Sender
        String from = "Unknown";
        Address[] senders = msg.getFrom();
        if (senders != null && senders.length > 0) {
            from = ((InternetAddress) senders[0]).getAddress();
        }

        // Parse Body & Attachments
        StringBuilder textBody = new StringBuilder();
        List<String> attachments = new ArrayList<>();
        parseContent(msg.getContent(), textBody, attachments);

        // Snippet is just the first 100 chars of body
        String snippet = textBody.length() > 100 ? textBody.substring(0, 100) + "..." : textBody.toString();

        return new InboxMessage(messageId, snippet, timestamp, from, attachments);
    }

    private void parseContent(Object content, StringBuilder textBody, List<String> attachments) throws MessagingException, java.io.IOException {
        if (content instanceof String) {
            textBody.append(content);
        } else if (content instanceof MimeMultipart) {
            MimeMultipart multipart = (MimeMultipart) content;
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart part = multipart.getBodyPart(i);
                if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())) {
                    attachments.add(part.getFileName());
                } else if (part.isMimeType("text/plain")) {
                    textBody.append(part.getContent());
                } else if (part.isMimeType("multipart/*")) {
                    parseContent(part.getContent(), textBody, attachments);
                }
            }
        }
    }
}