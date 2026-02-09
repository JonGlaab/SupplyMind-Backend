package com.supplymind.platform_core.service.communication;

import com.supplymind.platform_core.common.enums.PurchaseOrderStatus;
import com.supplymind.platform_core.dto.communication.InboxConversation; // Corrected Package
import com.supplymind.platform_core.dto.core.purchaseorder.InboxMessage;
import com.supplymind.platform_core.model.core.PurchaseOrder;
import com.supplymind.platform_core.repository.core.PurchaseOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InboxService {

    private final InboxProvider inboxProvider;
    private final PurchaseOrderRepository poRepo;

    public void createInboxForPo(Long poId) {
        inboxProvider.getOrCreateLabel("SupplyMind/PO-" + poId);
    }

    /**
     * Fetches the list of active POs to display in the Inbox Sidebar.
     * Uses the new InboxConversation DTO from the 'communication' package.
     */
    public List<InboxConversation> getConversations() {
        List<PurchaseOrder> allPos = poRepo.findAll();

        return allPos.stream()
                .sorted(Comparator.comparing(this::getLastActivity, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(po -> InboxConversation.builder()
                        .id(po.getPoId())
                        .poCode("PO-" + po.getPoId())
                        .supplierName(po.getSupplier() != null ? po.getSupplier().getName() : "Unknown Supplier")
                        .lastMessage("Status: " + po.getStatus())
                        .timestamp(getLastActivity(po))
                        .status(po.getStatus().name())
                        .unreadCount(0)
                        .build()
                ).collect(Collectors.toList());
    }

    private Instant getLastActivity(PurchaseOrder po) {
        if (po.getLastActivityAt() != null) {
            return po.getLastActivityAt();
        }
        return po.getCreatedOn();
    }

    /**
     * Fetches the actual chat messages (Emails) for a specific PO.
     */
    public List<InboxMessage> getPoChat(Long poId) {
        PurchaseOrder po = poRepo.findById(poId).orElse(null);
        if (po == null) return new ArrayList<>();

        String labelId = inboxProvider.getOrCreateLabel("SupplyMind/PO-" + poId);
        List<InboxMessage> messages = inboxProvider.fetchMessages(labelId);

        messages.sort(Comparator.comparingLong(InboxMessage::getTimestamp));

        return messages;
    }

    public byte[] getAttachment(Long poId, String messageId, String fileName) {
        String labelId = "SupplyMind/PO-" + poId;
        return inboxProvider.fetchAttachment(labelId, messageId, fileName);
    }
}