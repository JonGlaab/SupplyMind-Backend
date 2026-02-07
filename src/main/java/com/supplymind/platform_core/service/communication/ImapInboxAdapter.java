package com.supplymind.platform_core.service.communication;

import com.supplymind.platform_core.dto.core.purchaseorder.InboxMessage;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.search.HeaderTerm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Slf4j
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
        // Optimization: Don't fetch full content when listing headers
        props.put("mail.imaps.partialfetch", "false");

        Session session = Session.getInstance(props);
        Store store = session.getStore("imaps");
        store.connect(IMAP_HOST, username, password);
        return store;
    }

    @Override
    public String getOrCreateLabel(String labelName) {
        // In Gmail IMAP, we can't easily "create" labels via simple IMAP folders
        // without specific X-EXTENSIONS, but we can return the path.
        // If the folder doesn't exist, we will handle it gracefully in fetch.
        return labelName;
    }

    @Override
    public List<InboxMessage> fetchMessages(String labelId) {
        Store store = null;
        Folder folder = null;
        List<InboxMessage> result = new ArrayList<>();

        try {
            store = connect();
            folder = store.getFolder(labelId);

            if (!folder.exists()) {
                // If folder/label doesn't exist yet (no emails for this PO), return empty list
                return result;
            }

            folder.open(Folder.READ_ONLY);

            // Fetch messages (Limit to last 50 for performance if needed)
            Message[] messages = folder.getMessages();
            FetchProfile fp = new FetchProfile();
            fp.add(FetchProfile.Item.ENVELOPE); // Fetch headers only first
            folder.fetch(messages, fp);

            for (Message msg : messages) {
                try {
                    InboxMessage dto = new InboxMessage();

                    // 1. Basic Headers
                    dto.setSubject(msg.getSubject());
                    dto.setTimestamp(msg.getSentDate() != null ? msg.getSentDate().getTime() : System.currentTimeMillis());

                    // 2. Message ID (Crucial for fetching attachments later)
                    String[] messageIds = msg.getHeader("Message-ID");
                    dto.setMessageId(messageIds != null && messageIds.length > 0 ? messageIds[0] : null);

                    // 3. Sender
                    Address[] froms = msg.getFrom();
                    String from = (froms != null && froms.length > 0) ? ((InternetAddress) froms[0]).getAddress() : "Unknown";
                    dto.setFrom(from);

                    // 4. Content Parsing (Body & Attachment Names)
                    StringBuilder textBody = new StringBuilder();
                    List<String> attachmentNames = new ArrayList<>();
                    parseContent(msg.getContent(), textBody, attachmentNames);

                    dto.setBody(textBody.toString());
                    // Create a snippet (first 100 chars)
                    dto.setSnippet(textBody.length() > 100 ? textBody.substring(0, 100) + "..." : textBody.toString());
                    dto.setAttachments(attachmentNames);

                    result.add(dto);
                } catch (Exception e) {
                    log.error("Error parsing message", e);
                }
            }

        } catch (Exception e) {
            log.error("Failed to fetch messages from " + labelId, e);
        } finally {
            closeQuietly(folder, store);
        }

        return result;
    }
    @Override
    public void moveMessage(String messageId, String sourceLabel, String targetLabel) {
        Store store = null;
        Folder sourceFolder = null;
        Folder targetFolder = null;
        try {
            store = connect();

            // 1. Prepare Source
            sourceFolder = store.getFolder(sourceLabel);
            if (!sourceFolder.exists()) return;
            sourceFolder.open(Folder.READ_WRITE);

            // 2. Prepare Target
            targetFolder = store.getFolder(targetLabel);
            if (!targetFolder.exists()) {
                targetFolder.create(Folder.HOLDS_MESSAGES);
            }
            targetFolder.open(Folder.READ_WRITE);

            // 3. Find the specific message
            Message[] messages = sourceFolder.search(new HeaderTerm("Message-ID", messageId));

            if (messages.length > 0) {
                // 4. Copy to Target
                sourceFolder.copyMessages(messages, targetFolder);

                // 5. Delete from Source (Mark as DELETED)
                // This flags it for deletion. The 'close(true)' below performs the EXPUNGE.
                messages[0].setFlag(Flags.Flag.DELETED, true);

                log.info("Moved email {} from {} to {}", messageId, sourceLabel, targetLabel);
            }

        } catch (Exception e) {
            log.error("Failed to move message", e);
            throw new RuntimeException("Move failed");
        } finally {
            closeQuietly(targetFolder, null);
            // Closing source with 'true' expunges (permanently removes) deleted messages
            try {
                if (sourceFolder != null && sourceFolder.isOpen()) sourceFolder.close(true);
                if (store != null) store.close();
            } catch (Exception e) { /* ignore */ }
        }
    }


    @Override
    public byte[] fetchAttachment(String labelId, String messageId, String fileName) {
        Store store = null;
        Folder folder = null;
        try {
            store = connect();
            folder = store.getFolder(labelId);

            if (!folder.exists()) {
                throw new RuntimeException("Folder not found: " + labelId);
            }

            folder.open(Folder.READ_ONLY);

            // Search for the specific message by ID
            // Message-ID headers usually look like <UniqueString@mail.gmail.com>
            Message[] messages = folder.search(new HeaderTerm("Message-ID", messageId));

            if (messages.length == 0) {
                log.warn("Message with ID {} not found in folder {}", messageId, labelId);
                // Fallback: Try searching by Subject or Date if Message-ID fails (optional)
                throw new RuntimeException("Message not found");
            }

            Message message = messages[0]; // Take the first match

            // Scan the message content recursively
            byte[] fileData = findAttachmentInContent(message.getContent(), fileName);

            if (fileData == null) {
                throw new RuntimeException("Attachment '" + fileName + "' not found in message");
            }

            return fileData;

        } catch (Exception e) {
            log.error("Failed to fetch attachment", e);
            throw new RuntimeException("Attachment fetch failed: " + e.getMessage());
        } finally {
            closeQuietly(folder, store);
        }
    }

    // --- Helpers ---

    private void parseContent(Object content, StringBuilder textBody, List<String> attachments) throws Exception {
        if (content instanceof String) {
            textBody.append((String) content);
        } else if (content instanceof Multipart) {
            Multipart multi = (Multipart) content;
            for (int i = 0; i < multi.getCount(); i++) {
                BodyPart part = multi.getBodyPart(i);

                if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition()) ||
                        (part.getFileName() != null && !part.getFileName().isEmpty())) {
                    // It's an attachment
                    attachments.add(part.getFileName());
                } else if (part.isMimeType("text/plain")) {
                    textBody.append(part.getContent());
                } else if (part.isMimeType("multipart/*")) {
                    parseContent(part.getContent(), textBody, attachments);
                }
            }
        }
    }

    private byte[] findAttachmentInContent(Object content, String fileName) throws Exception {
        if (content instanceof Multipart) {
            Multipart multi = (Multipart) content;
            for (int i = 0; i < multi.getCount(); i++) {
                BodyPart part = multi.getBodyPart(i);

                // Check if this part is the file we want
                // We check both Disposition (Standard) and FileName (Some clients don't set disposition correctly)
                boolean isAttachment = Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition()) ||
                        Part.INLINE.equalsIgnoreCase(part.getDisposition()) ||
                        (part.getFileName() != null);

                if (isAttachment && part.getFileName() != null && part.getFileName().equalsIgnoreCase(fileName)) {
                    // Found it! Read bytes.
                    return readInputStream(part.getInputStream());
                }

                // Recurse if the part itself contains other parts (nested multipart)
                if (part.getContent() instanceof Multipart) {
                    byte[] found = findAttachmentInContent(part.getContent(), fileName);
                    if (found != null) return found;
                }
            }
        }
        return null;
    }

    private byte[] readInputStream(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[1024];
        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        buffer.flush();
        return buffer.toByteArray();
    }

    private void closeQuietly(Folder folder, Store store) {
        try {
            if (folder != null && folder.isOpen()) folder.close(false);
            if (store != null) store.close();
        } catch (Exception e) {
            // Ignore
        }
    }
}