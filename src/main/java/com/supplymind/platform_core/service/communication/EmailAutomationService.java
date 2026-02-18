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

    // Added initialDelay to give the login screen 30 seconds of peace on startup
    // @Scheduled(initialDelay = 30000, fixedDelay = 60000) //TODO remove note or try something else if this fails
    public void scanAndRouteEmails() {
        log.debug("🕵️ Starting Inbox & Sent Scan...");
        scanFolderAndRoute("INBOX", true);
        scanFolderAndRoute("[Gmail]/Sent Mail", false);
        scanFolderAndRoute("[Gmail]/Sent", false);
    }

    private void scanFolderAndRoute(String sourceFolder, boolean isMoveOperation) {
        try {
            List<InboxMessage> messages = inboxProvider.fetchMessages(sourceFolder);
            if (messages.isEmpty()) return;

            for (InboxMessage msg : messages) {
                if (msg.getSubject() == null) continue;

                Matcher matcher = PO_PATTERN.matcher(msg.getSubject());
                if (matcher.find()) {
                    Long poId = Long.parseLong(matcher.group(1));

                    // ✅ STEP 1: Use the Slim check (No Joins, connection released immediately)
                    if (poRepo.existsByPoId(poId).isEmpty()) continue;

                    String targetLabel = "SupplyMind/PO-" + poId;
                    boolean isSupplierReply = !msg.getFrom().toLowerCase().contains("supplymind");

                    // ✅ STEP 2: Slow Network I/O (Happens while DB pool is free)
                    try {
                        if (isMoveOperation) {
                            inboxProvider.moveMessage(msg.getMessageId(), sourceFolder, targetLabel);
                            log.info("✅ Moved message for PO #{}", poId);
                        } else {
                            inboxProvider.copyMessage(msg.getMessageId(), sourceFolder, targetLabel);
                            log.info("✅ Copied sent message for PO #{}", poId);
                        }

                        messagingTemplate.convertAndSend("/topic/po/" + poId, msg);
                    } catch (Exception e) {
                        log.error("❌ Email I/O failed for PO #{}", poId, e);
                        continue;
                    }

                    // ✅ STEP 3: Transactional DB Update (Only if needed)
                    if (isMoveOperation && isSupplierReply) {
                        updateStatusWithAi(poId, msg);
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Skipping scan for folder {}: {}", sourceFolder, e.getMessage());
        }
    }

    // This method is now the only place that holds a short-lived transaction
    @Transactional
    public void updateStatusWithAi(Long poId, InboxMessage msg) {
        // We use findById here because we actually need to update the object
        poRepo.findById(poId).ifPresent(po -> {
            log.info("📧 Analyzing Supplier Reply for PO #{}", poId);
            AiStatusScanner.StatusScanResult analysis = aiScanner.scanEmailForStatus(msg.getSnippet());

            if (analysis.deliveryDate() != null) {
                po.setExpectedDeliveryDate(analysis.deliveryDate().atStartOfDay(ZoneId.systemDefault()).toInstant());
            }

            // Logic to update status based on analysis...
            try {
                PurchaseOrderStatus detected = PurchaseOrderStatus.valueOf(analysis.status());
                List<PurchaseOrderStatus> validTransitions = List.of(
                        PurchaseOrderStatus.DELAY_EXPECTED,
                        PurchaseOrderStatus.CONFIRMED,
                        PurchaseOrderStatus.SUPPLIER_REPLIED,
                        PurchaseOrderStatus.SHIPPED
                );

                if (validTransitions.contains(detected)) {
                    po.setStatus(detected);
                }
            } catch (Exception ignored) {}

            po.setLastActivityAt(Instant.now());
            poRepo.save(po);
        });
    }
}