package com.supplymind.platform_core.service.communication;

import com.supplymind.platform_core.common.enums.PurchaseOrderStatus;
import com.supplymind.platform_core.dto.core.purchaseorder.InboxMessage;
import com.supplymind.platform_core.repository.core.PurchaseOrderRepository;
import com.supplymind.platform_core.service.intel.AiStatusScanner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    // ⚠️ Change type to ImapInboxAdapter to access 'copyMessage'
    private final ImapInboxAdapter inboxProvider;
    private final PurchaseOrderRepository poRepo;
    private final AiStatusScanner aiScanner;

    private final Pattern PO_PATTERN = Pattern.compile("(?i)PO[-\\s]*#?(\\d+)");

    // Run every 60 seconds
    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void scanAndRouteEmails() {
        log.debug("🕵️ Starting Inbox & Sent Scan...");

        // 1. Scan Supplier Replies (INBOX) -> MOVE to Folder
        // "true" means this is a MOVE operation (cleans up Inbox)
        scanFolderAndRoute("INBOX", true);

        // 2. Scan My Sent Emails ([Gmail]/Sent Mail) -> COPY to Folder
        // "false" means this is a COPY operation (keeps record in Sent)
        scanFolderAndRoute("[Gmail]/Sent Mail", false);
    }

    private void scanFolderAndRoute(String sourceFolder, boolean isMoveOperation) {
        try {
            List<InboxMessage> messages = inboxProvider.fetchMessages(sourceFolder);

            for (InboxMessage msg : messages) {
                if (msg.getSubject() == null) continue;

                Matcher matcher = PO_PATTERN.matcher(msg.getSubject());
                if (matcher.find()) {
                    Long poId = Long.parseLong(matcher.group(1));

                    poRepo.findById(poId).ifPresent(po -> {
                        String targetLabel = "SupplyMind/PO-" + poId;

                        // We only run AI analysis on incoming mail (Supplier Replies)
                        boolean isSupplierReply = !msg.getFrom().toLowerCase().contains("supplymind");

                        // --- LOGIC A: AI ANALYSIS ---
                        if (isMoveOperation && isSupplierReply) {
                            handleSupplierReply(po, msg);
                        }

                        // --- LOGIC B: ROUTING ---
                        if (isMoveOperation) {
                            // Move from Inbox
                            inboxProvider.moveMessage(msg.getMessageId(), sourceFolder, targetLabel);
                        } else {
                            // Copy from Sent
                            inboxProvider.copyMessage(msg.getMessageId(), sourceFolder, targetLabel);
                        }
                    });
                }
            }
        } catch (Exception e) {
            log.error("Error scanning folder: " + sourceFolder, e);
        }
    }

    private void handleSupplierReply(com.supplymind.platform_core.model.core.PurchaseOrder po, InboxMessage msg) {
        log.info("📧 Analyzing Supplier Reply for PO #{}", po.getPoId());

        // Ask AI for Status
        AiStatusScanner.StatusScanResult analysis = aiScanner.scanEmailForStatus(msg.getSnippet());

        // Update Delivery Date
        if (analysis.deliveryDate() != null) {
            po.setExpectedDeliveryDate(analysis.deliveryDate().atStartOfDay(ZoneId.systemDefault()).toInstant());
        }

        // Update Status (Safe Mode)
        // Only update if current status is "In Flight"
        if (po.getStatus() == PurchaseOrderStatus.EMAIL_SENT || po.getStatus() == PurchaseOrderStatus.SUPPLIER_REPLIED) {
            try {
                PurchaseOrderStatus detected = PurchaseOrderStatus.valueOf(analysis.status());
                if (detected == PurchaseOrderStatus.DELAY_EXPECTED ||
                        detected == PurchaseOrderStatus.CONFIRMED ||
                        detected == PurchaseOrderStatus.SUPPLIER_REPLIED) {
                    po.setStatus(detected);
                }
            } catch (Exception ignored) {}
        }

        po.setLastActivityAt(Instant.now());
        poRepo.save(po);
    }
}