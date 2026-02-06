package com.supplymind.platform_core.service.impl.core;

import com.supplymind.platform_core.dto.core.returns.*;
import com.supplymind.platform_core.model.core.*;
import com.supplymind.platform_core.common.enums.ReturnStatus; // ✅ USE THIS ONE (adjust if yours lives elsewhere)
import com.supplymind.platform_core.repository.core.PurchaseOrderItemRepository;
import com.supplymind.platform_core.repository.core.PurchaseOrderRepository;
import com.supplymind.platform_core.repository.core.ReturnLineItemRepository;
import com.supplymind.platform_core.repository.core.ReturnReceiptRepository;
import com.supplymind.platform_core.repository.core.ReturnRequestRepository;
import com.supplymind.platform_core.service.core.ReturnService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReturnServiceImpl implements ReturnService {

    private final ReturnRequestRepository returnRepo;
    private final ReturnReceiptRepository receiptRepo;
    private final ReturnLineItemRepository returnLineRepo;

    private final PurchaseOrderRepository poRepo;
    private final PurchaseOrderItemRepository poItemRepo;

    // statuses that "consume" returnable qty (prevents overlapping returns)
    private static final Set<ReturnStatus> STATUSES_THAT_CONSUME_QTY = EnumSet.of(
            ReturnStatus.REQUESTED,
            ReturnStatus.APPROVED,
            ReturnStatus.PARTIALLY_RECEIVED,
            ReturnStatus.RECEIVED,
            ReturnStatus.REFUNDED
    );

    @Override
    @Transactional
    public ReturnRequest createReturn(CreateReturnRequestDTO dto) {
        PurchaseOrder po = poRepo.findById(dto.getPoId())
                .orElseThrow(() -> new IllegalArgumentException("PO not found: " + dto.getPoId()));

        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new IllegalArgumentException("Return must contain at least one item.");
        }

        ReturnRequest r = new ReturnRequest();
        r.setPo(po);
        r.setReason(dto.getReason());
        r.setRequestedBy(dto.getRequestedBy());
        r.setStatus(ReturnStatus.REQUESTED);

        // Avoid duplicate lines for same poItem in one request
        Set<Long> seenPoItems = new HashSet<>();

        for (CreateReturnLineDTO lineDTO : dto.getItems()) {
            // ✅ FIXED BUG
            if (lineDTO.getPoItemId() == null) throw new IllegalArgumentException("poItemId is required");
            if (!seenPoItems.add(lineDTO.getPoItemId())) {
                throw new IllegalArgumentException("Duplicate poItemId in request: " + lineDTO.getPoItemId());
            }

            PurchaseOrderItem poItem = poItemRepo.findById(lineDTO.getPoItemId())
                    .orElseThrow(() -> new IllegalArgumentException("PO item not found: " + lineDTO.getPoItemId()));

            // Ensure item belongs to the PO
            if (!poItem.getPo().getPoId().equals(po.getPoId())) {
                throw new IllegalArgumentException("PO item " + poItem.getPoItemId() + " does not belong to PO " + po.getPoId());
            }

            int receivedQty = safeInt(poItem.getReceivedQty());
            int orderedQty = safeInt(poItem.getOrderedQty());

            // You can only return what was RECEIVED, minus already "consumed" by other returns
            int alreadyConsumed = returnLineRepo.sumApprovedQtyByPoItemAndStatuses(
                    poItem.getPoItemId(),
                    STATUSES_THAT_CONSUME_QTY
            );

            int maxReturnable = Math.max(0, receivedQty - alreadyConsumed);

            Integer reqQty = lineDTO.getQtyReturnRequested();
            if (reqQty == null || reqQty <= 0) throw new IllegalArgumentException("qtyReturnRequested must be > 0");
            if (reqQty > maxReturnable) {
                throw new IllegalArgumentException(
                        "Requested qty (" + reqQty + ") exceeds returnable qty (" + maxReturnable + ") for PO item " + poItem.getPoItemId()
                );
            }

            ReturnLineItem li = new ReturnLineItem();
            li.setPoItem(poItem);
            li.setOrderedQty(orderedQty);
            li.setReceivedQtyOnPo(receivedQty);
            li.setQtyReturnRequested(reqQty);
            li.setUnitCost(poItem.getUnitCost() == null ? BigDecimal.ZERO : poItem.getUnitCost());
            li.setConditionNotes(lineDTO.getConditionNotes());

            r.addItem(li);
        }

        return returnRepo.save(r);
    }

    @Override
    @Transactional
    public ReturnRequest approveReturn(Long returnId, ApproveReturnDTO dto) {
        ReturnRequest r = get(returnId);

        if (r.getStatus() != ReturnStatus.REQUESTED) {
            throw new IllegalStateException("Only REQUESTED returns can be approved.");
        }

        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new IllegalArgumentException("Approval must include line approvals.");
        }

        Map<Long, ReturnLineItem> lineMap = new HashMap<>();
        r.getItems().forEach(li -> lineMap.put(li.getId(), li));

        // ✅ Re-check returnable qty at approval time (realistic)
        // We exclude current return by subtracting its own approved (currently 0) so safe.
        int totalApproved = 0;

        for (ApproveReturnLineDTO a : dto.getItems()) {
            ReturnLineItem li = lineMap.get(a.getReturnLineId());
            if (li == null) throw new IllegalArgumentException("Return line not found: " + a.getReturnLineId());

            if (a.getQtyApproved() == null || a.getQtyApproved() < 0) {
                throw new IllegalArgumentException("qtyApproved must be >= 0");
            }
            if (a.getQtyApproved() > li.getQtyReturnRequested()) {
                throw new IllegalArgumentException("qtyApproved cannot exceed qtyReturnRequested for line " + li.getId());
            }

            PurchaseOrderItem poItem = li.getPoItem();
            int receivedQty = safeInt(poItem.getReceivedQty());

            // consumed by OTHER returns:
            int consumedAll = returnLineRepo.sumApprovedQtyByPoItemAndStatuses(
                    poItem.getPoItemId(),
                    STATUSES_THAT_CONSUME_QTY
            );

            // subtract this return's current approval for this line (may be 0)
            int consumedOtherReturns = Math.max(0, consumedAll - safeInt(li.getQtyApproved()));

            int maxReturnableNow = Math.max(0, receivedQty - consumedOtherReturns);

            if (a.getQtyApproved() > maxReturnableNow) {
                throw new IllegalArgumentException(
                        "Approved qty (" + a.getQtyApproved() + ") exceeds current returnable qty (" + maxReturnableNow + ") " +
                                "for PO item " + poItem.getPoItemId() + ". Another return may have consumed qty."
                );
            }

            li.setQtyApproved(a.getQtyApproved());
            li.setRestockFee(a.getRestockFee() == null ? BigDecimal.ZERO : a.getRestockFee());
            totalApproved += a.getQtyApproved();
        }

        r.setApprovedBy(dto.getApprovedBy());
        r.setApprovedAt(Instant.now());

        if (totalApproved <= 0) {
            r.setStatus(ReturnStatus.REJECTED);
        } else {
            r.setStatus(ReturnStatus.APPROVED);
        }

        return r;
    }

    @Override
    @Transactional
    public ReturnReceipt receiveReturn(Long returnId, ReceiveReturnDTO dto) {
        ReturnRequest r = get(returnId);

        if (!(r.getStatus() == ReturnStatus.APPROVED || r.getStatus() == ReturnStatus.PARTIALLY_RECEIVED)) {
            throw new IllegalStateException("Return must be APPROVED or PARTIALLY_RECEIVED to receive.");
        }

        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new IllegalArgumentException("Receive must include received lines.");
        }

        ReturnReceipt receipt = new ReturnReceipt();
        receipt.setReturnRequest(r);
        receipt.setReceivedBy(dto.getReceivedBy());

        Map<Long, ReturnLineItem> lineMap = new HashMap<>();
        r.getItems().forEach(li -> lineMap.put(li.getId(), li));

        for (ReceiveReturnLineDTO rec : dto.getItems()) {
            ReturnLineItem li = lineMap.get(rec.getReturnLineId());
            if (li == null) throw new IllegalArgumentException("Return line not found: " + rec.getReturnLineId());

            int qtyNow = rec.getQtyReceivedNow() == null ? 0 : rec.getQtyReceivedNow();
            if (qtyNow <= 0) throw new IllegalArgumentException("qtyReceivedNow must be > 0");
            if (qtyNow > li.remainingToReceive()) {
                throw new IllegalArgumentException("Receiving exceeds remaining approved qty for line " + li.getId());
            }

            li.setQtyReceived(li.getQtyReceived() + qtyNow);

            ReturnReceiptItem rri = new ReturnReceiptItem();
            rri.setReturnLine(li);
            rri.setQtyReceivedNow(qtyNow);
            receipt.addItem(rri);
        }

        boolean fullyReceived = r.getItems().stream().allMatch(li -> li.getQtyReceived() >= li.getQtyApproved());

        if (fullyReceived) {
            r.setStatus(ReturnStatus.RECEIVED);
            r.setReceivedAt(Instant.now());
        } else {
            r.setStatus(ReturnStatus.PARTIALLY_RECEIVED);
        }

        return receiptRepo.save(receipt);
    }

    @Override
    @Transactional
    public ReturnRequest refundReturn(Long returnId, RefundReturnDTO dto) {
        ReturnRequest r = get(returnId);

        if (r.getStatus() != ReturnStatus.RECEIVED) {
            throw new IllegalStateException("Return must be RECEIVED before refund.");
        }

        // Optional: compute refund if not provided
        if (dto.getRefundAmount() == null) {
            dto.setRefundAmount(computeRefundAmount(r));
        }

        r.setStatus(ReturnStatus.REFUNDED);
        r.setRefundedAt(Instant.now());
        // If you want: store refundedBy/ref/reference in ReturnRequest or a separate ReturnRefund entity

        return r;
    }

    @Override
    @Transactional
    public ReturnRequest cancelReturn(Long returnId, String cancelledBy) {
        ReturnRequest r = get(returnId);

        if (r.getStatus() != ReturnStatus.REQUESTED) {
            throw new IllegalStateException("Only REQUESTED returns can be cancelled.");
        }

        r.setStatus(ReturnStatus.CANCELLED);
        // If you want: store cancelledBy/cancelledAt on ReturnRequest

        return r;
    }

    @Override
    public ReturnRequest getReturn(Long returnId) {
        return get(returnId);
    }

    private ReturnRequest get(Long returnId) {
        return returnRepo.findById(returnId)
                .orElseThrow(() -> new IllegalArgumentException("Return not found: " + returnId));
    }

    private int safeInt(Integer v) {
        return v == null ? 0 : v;
    }

    private BigDecimal computeRefundAmount(ReturnRequest r) {
        // Typical refund = sum(received_qty * unit_cost) - restock_fee
        BigDecimal total = BigDecimal.ZERO;

        for (ReturnLineItem li : r.getItems()) {
            BigDecimal line = li.getUnitCost()
                    .multiply(BigDecimal.valueOf(li.getQtyReceived()));

            if (li.getRestockFee() != null) {
                line = line.subtract(li.getRestockFee());
            }

            total = total.add(line);
        }

        // never refund negative
        return total.max(BigDecimal.ZERO);
    }
}
