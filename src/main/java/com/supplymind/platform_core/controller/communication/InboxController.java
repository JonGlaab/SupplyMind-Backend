package com.supplymind.platform_core.controller.communication;

import com.supplymind.platform_core.dto.communication.InboxConversation;
import com.supplymind.platform_core.dto.core.purchaseorder.InboxMessage;
import com.supplymind.platform_core.service.communication.InboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/core/inbox")
@RequiredArgsConstructor
public class InboxController {

    private final InboxService inboxService;

    /**
     * Get List of Conversations (Sidebar)
     * Used by: InboxPage.jsx (loadConversations)
     */
    @GetMapping("/conversations")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','PROCUREMENT_OFFICER')")
    public ResponseEntity<List<InboxConversation>> getConversations() {
        return ResponseEntity.ok(inboxService.getConversations());
    }

    /**
     * Get Chat History (Triggers AI Scan)
     * Used by: InboxPage.jsx (loadChat)
     */
    @GetMapping("/po/{poId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','PROCUREMENT_OFFICER')")
    public ResponseEntity<List<InboxMessage>> getPoChat(@PathVariable Long poId) {
        return ResponseEntity.ok(inboxService.getPoChat(poId));
    }

    /**
     * Download Attachment
     * Used by: InboxPage.jsx (handleDownload)
     */
    @GetMapping("/attachments/{poId}/{fileName}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','PROCUREMENT_OFFICER')")
    public ResponseEntity<Resource> downloadAttachment(
            @PathVariable Long poId,
            @PathVariable String fileName,
            @RequestParam String messageId) {

        try {
            byte[] fileData = inboxService.getAttachment(poId, messageId, fileName);
            ByteArrayResource resource = new ByteArrayResource(fileData);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .contentLength(fileData.length)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.notFound().build();
        }
    }
}