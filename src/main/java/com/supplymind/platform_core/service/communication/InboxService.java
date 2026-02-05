package com.supplymind.platform_core.service.communication;

import com.supplymind.platform_core.common.enums.PurchaseOrderStatus;
import com.supplymind.platform_core.model.core.PurchaseOrder;
import com.supplymind.platform_core.repository.core.PurchaseOrderRepository;
import com.supplymind.platform_core.service.intel.AiStatusScanner;
import com.supplymind.platform_core.dto.core.purchaseorder.InboxMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InboxService {

    private final InboxProvider inboxProvider;
    private final PurchaseOrderRepository poRepo;
    private final AiStatusScanner aiScanner;

    public void createInboxForPo(Long poId) {
        inboxProvider.getOrCreateLabel("SupplyMind/PO-" + poId);
    }

    public List<InboxMessage> getPoChat(Long poId) {
        PurchaseOrder po = poRepo.findById(poId).orElse(null);
        if (po == null) return new ArrayList<>();

        String labelId = inboxProvider.getOrCreateLabel("SupplyMind/PO-" + poId);
        List<InboxMessage> messages = inboxProvider.fetchMessages(labelId);

        boolean dbNeedsUpdate = false;

        for (InboxMessage msg : messages) {

            // --- AI DELTA LOGIC ---
            boolean isNew = (po.getLastActivityAt() == null) || (msg.getTimestamp() > po.getLastActivityAt().toEpochMilli());
            boolean isFromSupplier = !msg.getFrom().contains("Me") && !msg.getFrom().contains("SupplyMind");

            if (isNew && isFromSupplier) {
                System.out.println("🤖 New Email Detected! Scanning...");
                var result = aiScanner.scanEmailForStatus(msg.getSnippet());

                // Date Logic
                if (result.deliveryDate() != null) {
                    Instant newDate = result.deliveryDate().atStartOfDay(ZoneId.systemDefault()).toInstant();
                    if (po.getExpectedDeliveryDate() == null || !po.getExpectedDeliveryDate().equals(newDate)) {
                        po.setStatus(PurchaseOrderStatus.DELAY_EXPECTED);
                        po.setExpectedDeliveryDate(newDate);
                        dbNeedsUpdate = true;
                    }
                }

                // Status Logic
                if (!result.status().equals("SUPPLIER_REPLIED") && po.getStatus() != PurchaseOrderStatus.DELAY_EXPECTED) {
                    try {
                        po.setStatus(PurchaseOrderStatus.valueOf(result.status()));
                        dbNeedsUpdate = true;
                    } catch (Exception ignored) {}
                }
            }

            if (isNew) {
                po.setLastActivityAt(Instant.ofEpochMilli(msg.getTimestamp()));
                dbNeedsUpdate = true;
            }
        }

        if (dbNeedsUpdate) poRepo.save(po);

        messages.sort(Comparator.comparingLong(InboxMessage::getTimestamp));
        return messages;
    }
}