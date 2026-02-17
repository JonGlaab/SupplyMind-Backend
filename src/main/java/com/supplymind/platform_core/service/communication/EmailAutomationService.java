package com.supplymind.platform_core.service.communication;

import com.supplymind.platform_core.common.enums.PurchaseOrderStatus;
import com.supplymind.platform_core.dto.core.purchaseorder.InboxMessage;
import com.supplymind.platform_core.repository.core.PurchaseOrderRepository;
import com.supplymind.platform_core.service.intel.AiStatusScanner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailAutomationService {

    private final ImapInboxAdapter inboxProvider;
    private final PurchaseOrderRepository poRepo;
    private final AiStatusScanner aiScanner;
    private final SimpMessagingTemplate messagingTemplate;

    private final Pattern PO_PATTERN = Pattern.compile("(?i)(?:Purchase\\s+Order|PO)[-\\s#]*(\\d+)");

    // Run every 60 seconds
    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void scanAndRouteEmails() {
        log.debug("🕵️ Starting Inbox & Sent Scan...");

        // 1. Scan Supplier Replies (INBOX) -> MOVE to Folder
        scanFolderAndRoute("INBOX", true);

        // 2. Scan My Sent Emails -> COPY to Folder
        // Try modern/standard name
        scanFolderAndRoute("[Gmail]/Sent Mail", false);

        // Try legacy/migrated name
        scanFolderAndRoute("[Gmail]/Sent", false);
    }

    private void scanFolderAndRoute(String sourceFolder, boolean isMoveOperation) {
        try {
            List<InboxMessage> messages = inboxProvider.fetchMessages(sourceFolder);

            // If folder doesn't exist or is empty, just return
            if (messages.isEmpty()) return;

            for (InboxMessage msg : messages) {
                if (msg.getSubject() == null) continue;

                Matcher matcher = PO_PATTERN.matcher(msg.getSubject());
                if (matcher.find()) {
                    Long poId = Long.parseLong(matcher.group(1));

                    poRepo.findById(poId).ifPresent(po -> {
                        String targetLabel = "SupplyMind/PO-" + poId;

                        boolean isSupplierReply = !msg.getFrom().toLowerCase().contains("supplymind");

                        // --- LOGIC A: AI ANALYSIS (Only for Incoming Supplier Replies) ---
                        if (isMoveOperation && isSupplierReply) {
                            try {
                                handleSupplierReply(po, msg);
                            } catch (Exception e) {
                                log.error("⚠️ AI Analysis failed for PO #{} but proceeding with move.", poId, e);
                            }
                        }

                        // --- LOGIC B: ROUTING & BROADCAST ---
                        try {
                            if (isMoveOperation) {
                                inboxProvider.moveMessage(msg.getMessageId(), sourceFolder, targetLabel);
                                log.info("✅ Moved message for PO #{} to {}", poId, targetLabel);
                            } else {
                                inboxProvider.copyMessage(msg.getMessageId(), sourceFolder, targetLabel);
                                log.info("✅ Copied sent message for PO #{} to {}", poId, targetLabel);
                            }

                            messagingTemplate.convertAndSend("/topic/po/" + poId, msg);
                            log.info("📡 Broadcasted WebSocket event for PO #{}", poId);

                        } catch (Exception e) {
                            log.error("❌ Failed to move/copy/broadcast email for PO #" + poId, e);
                        }
                    });
                }
            }
        } catch (Exception e) {
            log.debug("Skipping scan for folder {}: {}", sourceFolder, e.getMessage());
        }
    }

    private void handleSupplierReply(com.supplymind.platform_core.model.core.PurchaseOrder po, InboxMessage msg) {
        log.info("📧 Analyzing Supplier Reply for PO #{}", po.getPoId());

        AiStatusScanner.StatusScanResult analysis = aiScanner.scanEmailForStatus(msg.getSnippet());

        if (analysis.deliveryDate() != null) {
            po.setExpectedDeliveryDate(analysis.deliveryDate().atStartOfDay(ZoneId.systemDefault()).toInstant());
        }

        if (po.getStatus() == PurchaseOrderStatus.EMAIL_SENT || po.getStatus() == PurchaseOrderStatus.SUPPLIER_REPLIED) {
            try {
                PurchaseOrderStatus detected = PurchaseOrderStatus.valueOf(analysis.status());
                // Update status if relevant...
                if (detected == PurchaseOrderStatus.DELAY_EXPECTED ||
                        detected == PurchaseOrderStatus.CONFIRMED ||
                        detected == PurchaseOrderStatus.SUPPLIER_REPLIED ||
                        detected == PurchaseOrderStatus.SHIPPED) {
                    po.setStatus(detected);
                }
            } catch (Exception ignored) {}
        }

        po.setLastActivityAt(Instant.now());
        poRepo.save(po);
    }
}