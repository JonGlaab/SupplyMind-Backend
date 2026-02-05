package com.supplymind.platform_core.controller.core;

import com.supplymind.platform_core.dto.core.purchaseorder.InboxMessage;
import com.supplymind.platform_core.service.communication.InboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/core/inbox")
@RequiredArgsConstructor
public class InboxController {

    private final InboxService inboxService;

    // Get Chat History (Triggers AI Scan)
    @GetMapping("/po/{poId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','PROCUREMENT_OFFICER')")
    public ResponseEntity<List<InboxMessage>> getPoChat(@PathVariable Long poId) {
        return ResponseEntity.ok(inboxService.getPoChat(poId));
    }
}