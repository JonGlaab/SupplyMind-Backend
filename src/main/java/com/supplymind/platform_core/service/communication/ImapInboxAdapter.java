package com.supplymind.platform_core.service.communication;

import com.supplymind.platform_core.dto.core.purchaseorder.InboxMessage;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.search.HeaderTerm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class ImapInboxAdapter implements InboxProvider {

    @Value("${spring.mail.username}")
    private String username;

    @Value("${spring.mail.password}")
    private String password;

    private static final String IMAP_HOST = "imap.gmail.com";

    private static final int SCAN_DEPTH = 500;

    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]+>");
    private static final Pattern REPLY_SPLIT_PATTERN = Pattern.compile(
            "(\\nOn\\s.+,.+wrote:|\\nFrom:\\s.+|\\nSent:\\s.+|\\n-----Original Message-----)",
            Pattern.CASE_INSENSITIVE
    );

    private Store connect() throws MessagingException {
        Properties props = new Properties();
        props.put("mail.store.protocol", "imaps");
        props.put("mail.imaps.host", IMAP_HOST);
        props.put("mail.imaps.port", "993");
        props.put("mail.imaps.ssl.enable", "true");
        props.put("mail.imaps.partialfetch", "false");

        Session session = Session.getInstance(props);
        Store store = session.getStore("imaps");
        store.connect(IMAP_HOST, username, password);
        return store;
    }

    @Override
    public String getOrCreateLabel(String labelName) {
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

            if (!folder.exists()) return result;

            folder.open(Folder.READ_ONLY);

            int count = folder.getMessageCount();
            if (count == 0) return result;

            int start = Math.max(1, count - SCAN_DEPTH);
            Message[] messages = folder.getMessages(start, count);

            FetchProfile fp = new FetchProfile();
            fp.add(FetchProfile.Item.ENVELOPE);
            folder.fetch(messages, fp);

            for (int i = messages.length - 1; i >= 0; i--) {
                Message msg = messages[i];
                try {
                    InboxMessage dto = new InboxMessage();
                    dto.setSubject(msg.getSubject());
                    if (dto.getSubject() == null) continue;

                    dto.setTimestamp(msg.getSentDate() != null ? msg.getSentDate().getTime() : System.currentTimeMillis());

                    String[] messageIds = msg.getHeader("Message-ID");
                    dto.setMessageId(messageIds != null && messageIds.length > 0 ? messageIds[0] : null);

                    Address[] froms = msg.getFrom();
                    String from = (froms != null && froms.length > 0) ? ((InternetAddress) froms[0]).getAddress() : "Unknown";
                    dto.setFrom(from);

                    StringBuilder textBody = new StringBuilder();
                    List<String> attachmentNames = new ArrayList<>();

                    try {
                        extractContent(msg, textBody, attachmentNames);
                    } catch (Exception e) {
                        log.warn("Could not extract content for msg: {}", dto.getSubject());
                    }

                    String fullBody = textBody.toString().trim();
                    if (fullBody.isEmpty()) fullBody = "(No text content found)";

                    String cleanBody = cleanReplyBody(fullBody);
                    dto.setBody(cleanBody);
                    dto.setSnippet(cleanBody.length() > 150 ? cleanBody.substring(0, 150) + "..." : cleanBody);
                    dto.setAttachments(attachmentNames);

                    result.add(dto);
                } catch (Exception e) {
                    log.error("Skipping message due to parse error", e);
                }
            }
        } catch (Exception e) {
            log.error("Failed to fetch messages from " + labelId, e);
        } finally {
            closeQuietly(folder, store);
        }

        return result;
    }

    private void extractContent(Part part, StringBuilder textBody, List<String> attachments) throws Exception {
        if (part.isMimeType("text/plain")) {
            textBody.append((String) part.getContent());
        }
        else if (part.isMimeType("text/html")) {
            String html = (String) part.getContent();
            html = html.replaceAll("(?i)<br\\s*/?>", "\n")
                    .replaceAll("(?i)</p>", "\n\n")
                    .replaceAll("(?i)</div>", "\n");
            String cleanText = HTML_TAG_PATTERN.matcher(html).replaceAll("").trim();
            cleanText = cleanText.replace("&nbsp;", " ")
                    .replace("&amp;", "&").replace("&quot;", "\"")
                    .replace("&gt;", ">").replace("&lt;", "<");
            textBody.append(cleanText);
        }
        else if (part.isMimeType("multipart/*")) {
            Multipart multi = (Multipart) part.getContent();
            for (int i = 0; i < multi.getCount(); i++) {
                extractContent(multi.getBodyPart(i), textBody, attachments);
            }
        }
        else if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition()) ||
                (part.getFileName() != null && !part.getFileName().isEmpty())) {
            attachments.add(part.getFileName());
        }
    }

    private String cleanReplyBody(String body) {
        Matcher matcher = REPLY_SPLIT_PATTERN.matcher(body);
        if (matcher.find()) {
            return body.substring(0, matcher.start()).trim();
        }
        return body;
    }

    @Override
    public void moveMessage(String messageId, String sourceLabel, String targetLabel) {
        operateOnMessage(messageId, sourceLabel, targetLabel, true);
    }

    @Override
    public void copyMessage(String messageId, String sourceLabel, String targetLabel) {
        operateOnMessage(messageId, sourceLabel, targetLabel, false);
    }

    private void operateOnMessage(String messageId, String sourceLabel, String targetLabel, boolean isMove) {
        Store store = null;
        Folder sourceFolder = null;
        Folder targetFolder = null;
        try {
            store = connect();
            sourceFolder = store.getFolder(sourceLabel);
            if (!sourceFolder.exists()) return;
            // READ_WRITE needed for moving (flagging as deleted)
            sourceFolder.open(Folder.READ_WRITE);

            targetFolder = store.getFolder(targetLabel);
            if (!targetFolder.exists()) {
                boolean created = targetFolder.create(Folder.HOLDS_MESSAGES);
                if (!created) log.warn("Failed to create folder: {}", targetLabel);
            }
            targetFolder.open(Folder.READ_WRITE);

            // Avoid duplicating if already there
            if (targetFolder.search(new HeaderTerm("Message-ID", messageId)).length > 0) {
                log.debug("Message {} already exists in {}", messageId, targetLabel);
                return;
            }

            Message[] messages = sourceFolder.search(new HeaderTerm("Message-ID", messageId));
            if (messages.length > 0) {
                sourceFolder.copyMessages(messages, targetFolder);
                if (isMove) {
                    messages[0].setFlag(Flags.Flag.DELETED, true);
                    log.info("Moved message {} from {} to {}", messageId, sourceLabel, targetLabel);
                } else {
                    log.info("Copied message {} from {} to {}", messageId, sourceLabel, targetLabel);
                }
            }
        } catch (Exception e) {
            log.error("Failed to move/copy message", e);
        } finally {
            closeQuietly(targetFolder, null);
            closeQuietly(sourceFolder, store);
        }
    }

    @Override
    public byte[] fetchAttachment(String labelId, String messageId, String fileName) {
        Store store = null;
        Folder folder = null;
        try {
            store = connect();
            folder = store.getFolder(labelId);
            if (!folder.exists()) throw new RuntimeException("Folder not found");
            folder.open(Folder.READ_ONLY);

            Message[] messages = folder.search(new HeaderTerm("Message-ID", messageId));
            if (messages.length == 0) throw new RuntimeException("Message not found");

            return findAttachmentInContent(messages[0], fileName);
        } catch (Exception e) {
            log.error("Failed to fetch attachment", e);
            throw new RuntimeException(e);
        } finally {
            closeQuietly(folder, store);
        }
    }

    private byte[] findAttachmentInContent(Part part, String fileName) throws Exception {
        if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition()) ||
                (part.getFileName() != null && part.getFileName().equalsIgnoreCase(fileName))) {
            return readInputStream(part.getInputStream());
        }

        if (part.isMimeType("multipart/*")) {
            Multipart multi = (Multipart) part.getContent();
            for (int i = 0; i < multi.getCount(); i++) {
                byte[] found = findAttachmentInContent(multi.getBodyPart(i), fileName);
                if (found != null) return found;
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
        return buffer.toByteArray();
    }

    private void closeQuietly(Folder folder, Store store) {
        try {
            if (folder != null && folder.isOpen()) folder.close(false);
            if (store != null) store.close();
        } catch (Exception e) { /* ignore */ }
    }
}