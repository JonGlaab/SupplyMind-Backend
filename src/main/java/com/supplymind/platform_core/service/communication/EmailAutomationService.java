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

    private final InboxProvider inboxProvider;
    private final PurchaseOrderRepository poRepo;
    private final AiStatusScanner aiScanner;


    private final Pattern PO_PATTERN = Pattern.compile("(?i)PO[-\\s]*#?(\\d+)");

    // Run every 60 seconds
    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void scanAndRouteEmails() {
        log.debug("🕵️ Starting AI-Assisted Inbox Scan...");

        try {
            // 1. Fetch unread messages from main INBOX
            List<InboxMessage> inboxMessages = inboxProvider.fetchMessages("INBOX");

            for (InboxMessage msg : inboxMessages) {
                if (msg.getSubject() == null) continue;

                // 2. Exact Regex Match (Safe Routing)
                Matcher matcher = PO_PATTERN.matcher(msg.getSubject());
                if (matcher.find()) {
                    String poIdStr = matcher.group(1);
                    Long poId = Long.parseLong(poIdStr);

                    poRepo.findById(poId).ifPresent(po -> {
                        // Security: Ignore emails sent BY us (loopback check)
                        boolean isFromSupplier = !msg.getFrom().toLowerCase().contains("supplymind");

                        if (isFromSupplier) {
                            log.info("📧 Analyzing Supplier Reply for PO #{}", poId);

                            AiStatusScanner.StatusScanResult analysis = aiScanner.scanEmailForStatus(msg.getSnippet());

                            // A. Update Delivery Date (If found)
                            if (analysis.deliveryDate() != null) {
                                Instant newDate = analysis.deliveryDate().atStartOfDay(ZoneId.systemDefault()).toInstant();
                                po.setExpectedDeliveryDate(newDate);
                                log.info("📅 AI caught delivery date: {}", newDate);
                            }

                            // B. Update Status (Conservative Mode)
                            // We only switch if it's currently "Sent" or "Replied".
                            // We do NOT overwrite final states like PAID or RECEIVED.
                            if (po.getStatus() == PurchaseOrderStatus.EMAIL_SENT || po.getStatus() == PurchaseOrderStatus.SUPPLIER_REPLIED) {
                                try {
                                    String detectedStr = analysis.status();
                                    // Map AI string to Enum safely
                                    PurchaseOrderStatus detectedStatus = PurchaseOrderStatus.valueOf(detectedStr);

                                    // Only allow specific "In-Flight" status updates
                                    if (detectedStatus == PurchaseOrderStatus.DELAY_EXPECTED ||
                                            detectedStatus == PurchaseOrderStatus.CONFIRMED ||
                                            detectedStatus == PurchaseOrderStatus.SUPPLIER_REPLIED) {

                                        po.setStatus(detectedStatus);
                                        log.info("🤖 AI updated status to: {}", detectedStatus);
                                    }
                                } catch (Exception ignored) {
                                    // If AI returns weird text, just mark as Replied
                                    po.setStatus(PurchaseOrderStatus.SUPPLIER_REPLIED);
                                }
                            }

                            // Always bubble up the activity
                            po.setLastActivityAt(Instant.now());
                            poRepo.save(po);
                        }

                        // --- ROUTING STEP ---
                        // Move email from "INBOX" to "SupplyMind/PO-{id}"
                        // This ensures it appears in the specific PO Chat on the frontend
                        String targetLabel = "SupplyMind/PO-" + poId;
                        inboxProvider.moveMessage(msg.getMessageId(), "INBOX", targetLabel);
                    });
                }
            }
        } catch (Exception e) {
            log.error("Error during automation scan", e);
        }
    }
}