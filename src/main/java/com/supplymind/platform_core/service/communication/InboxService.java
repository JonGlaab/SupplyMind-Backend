package com.supplymind.platform_core.service.communication;

import com.supplymind.platform_core.dto.communication.InboxConversation;
import com.supplymind.platform_core.dto.core.purchaseorder.InboxMessage;
import com.supplymind.platform_core.model.core.PurchaseOrder;
import com.supplymind.platform_core.repository.core.PurchaseOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InboxService {

    private final ImapInboxAdapter inboxProvider;
    private final PurchaseOrderRepository poRepo;

    /**
     * Ensures the folder exists (e.g., "SupplyMind/PO-102")
     */
    public void createInboxForPo(Long poId) {
        inboxProvider.getOrCreateLabel("SupplyMind/PO-" + poId);
    }

    /**
     * Fetches the list of active POs to display in the Inbox Sidebar.
     * Note: This comes from the Database, not Gmail.
     * If a PO is missing here, it means it doesn't exist in the Prod Database.
     */
    @Transactional(readOnly = true)
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
     * FIX: Looks specifically in 'SupplyMind/PO-{id}' instead of 'INBOX'.
     * This solves the issue where Localhost moves the email and Prod can't find it.
     */
    @Transactional(readOnly = true)
    public List<InboxMessage> getPoChat(Long poId) {
        PurchaseOrder po = poRepo.findById(poId).orElse(null);
        if (po == null) return new ArrayList<>();


        String targetLabel = "SupplyMind/PO-" + poId;


        String labelId = inboxProvider.getOrCreateLabel(targetLabel);


        List<InboxMessage> messages = inboxProvider.fetchMessages(labelId);

        
        messages.sort(Comparator.comparingLong(InboxMessage::getTimestamp));

        return messages;
    }

    /**
     * Fetches attachment from the specific PO folder.
     */
    public byte[] getAttachment(Long poId, String messageId, String fileName) {
        String labelId = "SupplyMind/PO-" + poId;
        return inboxProvider.fetchAttachment(labelId, messageId, fileName);
    }
}