package com.supplymind.platform_core.service.communication;

import com.supplymind.platform_core.dto.core.purchaseorder.InboxMessage;
import com.supplymind.platform_core.model.core.PurchaseOrder;
import com.supplymind.platform_core.repository.core.PurchaseOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InboxService {

    private final InboxProvider inboxProvider;
    private final PurchaseOrderRepository poRepo;

    public void createInboxForPo(Long poId) {
        inboxProvider.getOrCreateLabel("SupplyMind/PO-" + poId);
    }

    // UPDATED: Purely fetches messages. No AI scanning here (Background service handles it).
    public List<InboxMessage> getPoChat(Long poId) {
        PurchaseOrder po = poRepo.findById(poId).orElse(null);
        if (po == null) return new ArrayList<>();

        // 1. Fetch messages from the routed folder
        String labelId = inboxProvider.getOrCreateLabel("SupplyMind/PO-" + poId);
        List<InboxMessage> messages = inboxProvider.fetchMessages(labelId);

        // 2. Sort by time (Oldest -> Newest)
        messages.sort(Comparator.comparingLong(InboxMessage::getTimestamp));

        return messages;
    }

    public byte[] getAttachment(Long poId, String messageId, String fileName) {
        String labelId = "SupplyMind/PO-" + poId;

        // 2. Fetch
        byte[] data = inboxProvider.fetchAttachment(labelId, messageId, fileName);

        if (data == null) {
            throw new RuntimeException("Attachment not found in email");
        }
        return data;
    }
}