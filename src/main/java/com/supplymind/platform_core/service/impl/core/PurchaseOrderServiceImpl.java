package com.supplymind.platform_core.service.impl.core;

import com.supplymind.platform_core.common.enums.InventoryTransactionType;
import com.supplymind.platform_core.common.enums.PurchaseOrderStatus;
import com.supplymind.platform_core.dto.core.purchaseorder.*;
import com.supplymind.platform_core.exception.BadRequestException;
import com.supplymind.platform_core.exception.NotFoundException;
import com.supplymind.platform_core.model.auth.User;
import com.supplymind.platform_core.model.core.*;
import com.supplymind.platform_core.repository.auth.UserRepository;
import com.supplymind.platform_core.repository.core.*;
import com.supplymind.platform_core.service.auth.AuthService;
import com.supplymind.platform_core.service.core.PurchaseOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PurchaseOrderServiceImpl implements PurchaseOrderService {

    private final PurchaseOrderRepository poRepo;
    private final PurchaseOrderItemRepository itemRepo;

    private final SupplierRepository supplierRepo;
    private final WarehouseRepository warehouseRepo;
    private final ProductRepository productRepo;

    private final InventoryRepository inventoryRepo;
    private final InventoryTransactionRepository txRepo;

    private final UserRepository userRepo;
    private final AuthService authService;


    // ----------------------------
    // CREATE DRAFT
    // ----------------------------
    @Override
    @Transactional
    public PurchaseOrderResponse createDraft(PurchaseOrderCreateRequest req) {

        Supplier supplier = supplierRepo.findById(req.supplierId())
                .orElseThrow(() -> new NotFoundException("Supplier not found: " + req.supplierId()));

        Warehouse warehouse = warehouseRepo.findById(req.warehouseId())
                .orElseThrow(() -> new NotFoundException("Warehouse not found: " + req.warehouseId()));

        // Minimal buyer assignment: pick first admin/manager or first user.
        // Later we can tie to JWT current user.
        User buyer = userRepo.findAll().stream().findFirst()
                .orElseThrow(() -> new BadRequestException("No users exist to assign as buyer. Create a user first."));

        PurchaseOrder po = PurchaseOrder.builder()
                .supplier(supplier)
                .warehouse(warehouse)
                .buyer(buyer)
                .status(PurchaseOrderStatus.DRAFT)
                .totalAmount(BigDecimal.ZERO)
                .build();

        poRepo.save(po);
        return toResponse(po, List.of());
    }

    // ----------------------------
    // LIST WITH FILTERS
    // ----------------------------
    @Override
    public Page<PurchaseOrderResponse> list(PurchaseOrderStatus status, Long supplierId, Long warehouseId, Pageable pageable) {
        Page<PurchaseOrder> page;

        if (status != null && supplierId != null && warehouseId != null) {
            page = poRepo.findAllByStatusAndSupplier_SupplierIdAndWarehouse_WarehouseId(status, supplierId, warehouseId, pageable);

        } else if (status != null && supplierId != null) {
            page = poRepo.findAllByStatusAndSupplier_SupplierId(status, supplierId, pageable);

        } else if (status != null && warehouseId != null) {
            page = poRepo.findAllByStatusAndWarehouse_WarehouseId(status, warehouseId, pageable);

        } else if (supplierId != null && warehouseId != null) {
            page = poRepo.findAllBySupplier_SupplierIdAndWarehouse_WarehouseId(supplierId, warehouseId, pageable);

        } else if (status != null) {
            page = poRepo.findAllByStatus(status, pageable);

        } else if (supplierId != null) {
            page = poRepo.findAllBySupplier_SupplierId(supplierId, pageable);

        } else if (warehouseId != null) {
            page = poRepo.findAllByWarehouse_WarehouseId(warehouseId, pageable);

        } else {
            page = poRepo.findAll(pageable);
        }

        return page.map(po -> toResponse(po, itemRepo.findAllByPo_PoId(po.getPoId())));
    }


    // ----------------------------
    // GET DETAILS
    // ----------------------------
    @Override
    public PurchaseOrderResponse get(Long poId) {
        PurchaseOrder po = poRepo.findById(poId)
                .orElseThrow(() -> new NotFoundException("Purchase Order not found: " + poId));

        List<PurchaseOrderItem> items = itemRepo.findAllByPo_PoId(poId);
        return toResponse(po, items);
    }

    // ----------------------------
    // UPDATE HEADER (DRAFT ONLY)
    // ----------------------------
    @Override
    @Transactional
    public PurchaseOrderResponse updateHeader(Long poId, PurchaseOrderUpdateRequest req) {
        PurchaseOrder po = requirePo(poId);

        ensureDraft(po);

        if (req.supplierId() != null) {
            Supplier s = supplierRepo.findById(req.supplierId())
                    .orElseThrow(() -> new NotFoundException("Supplier not found: " + req.supplierId()));
            po.setSupplier(s);
        }

        if (req.warehouseId() != null) {
            Warehouse w = warehouseRepo.findById(req.warehouseId())
                    .orElseThrow(() -> new NotFoundException("Warehouse not found: " + req.warehouseId()));
            po.setWarehouse(w);
        }

        List<PurchaseOrderItem> items = itemRepo.findAllByPo_PoId(poId);
        recalcTotal(po, items);

        return toResponse(po, items);
    }

    // ----------------------------
    // ADD ITEM (DRAFT ONLY)
    // ----------------------------
    @Override
    @Transactional
    public PurchaseOrderItemResponse addItem(Long poId, PurchaseOrderItemCreateRequest req) {
        PurchaseOrder po = requirePo(poId);
        ensureDraft(po);

        Product product = productRepo.findById(req.productId())
                .orElseThrow(() -> new NotFoundException("Product not found: " + req.productId()));

        PurchaseOrderItem item = PurchaseOrderItem.builder()
                .po(po)
                .product(product)
                .orderedQty(req.orderedQty())
                .receivedQty(0)
                .unitCost(req.unitCost())
                .build();

        PurchaseOrderItem saved = itemRepo.save(item);

        List<PurchaseOrderItem> items = itemRepo.findAllByPo_PoId(poId);
        recalcTotal(po, items);

        return toItemResponse(saved);
    }

    // ----------------------------
    // UPDATE ITEM (DRAFT ONLY)
    // ----------------------------
    @Override
    @Transactional
    public PurchaseOrderItemResponse updateItem(Long poId, Long itemId, PurchaseOrderItemUpdateRequest req) {
        PurchaseOrder po = requirePo(poId);
        ensureDraft(po);

        PurchaseOrderItem item = requireItem(poId, itemId);

        if (req.orderedQty() != null) item.setOrderedQty(req.orderedQty());
        if (req.unitCost() != null) item.setUnitCost(req.unitCost());

        PurchaseOrderItem saved = itemRepo.save(item);

        List<PurchaseOrderItem> items = itemRepo.findAllByPo_PoId(poId);
        recalcTotal(po, items);

        return toItemResponse(saved);
    }

    // ----------------------------
    // REMOVE ITEM (DRAFT ONLY)
    // ----------------------------
    @Override
    @Transactional
    public void removeItem(Long poId, Long itemId) {
        PurchaseOrder po = requirePo(poId);
        ensureDraft(po);

        PurchaseOrderItem item = requireItem(poId, itemId);
        itemRepo.delete(item);

        List<PurchaseOrderItem> items = itemRepo.findAllByPo_PoId(poId);
        recalcTotal(po, items);
    }

    // ----------------------------
    // SUBMIT (DRAFT -> SUBMITTED)
    // ----------------------------
    @Override
    @Transactional
    public PurchaseOrderResponse submit(Long poId) {
        PurchaseOrder po = requirePo(poId);
        ensureDraft(po);

        List<PurchaseOrderItem> items = itemRepo.findAllByPo_PoId(poId);
        if (items.isEmpty()) throw new BadRequestException("Cannot submit PO with no items.");

        po.setStatus(PurchaseOrderStatus.PENDING_APPROVAL);
        recalcTotal(po, items);

        return toResponse(po, items);
    }

    // ----------------------------
    // APPROVE (SUBMITTED -> APPROVED)
    // ----------------------------
    @Override
    @Transactional
    public PurchaseOrderResponse approve(Long poId) {
        PurchaseOrder po = requirePo(poId);

        if (po.getStatus() != PurchaseOrderStatus.PENDING_APPROVAL) {
            throw new BadRequestException("Only PENDING_APPROVAL POs can be approved.");
        }

        po.setStatus(PurchaseOrderStatus.APPROVED);

        List<PurchaseOrderItem> items = itemRepo.findAllByPo_PoId(poId);
        return toResponse(po, items);
    }

    // ----------------------------
    // CANCEL (not RECEIVED)
    // ----------------------------
    @Override
    @Transactional
    public PurchaseOrderResponse cancel(Long poId) {
        PurchaseOrder po = requirePo(poId);

        if (po.getStatus() == PurchaseOrderStatus.CONFIRMED) {
            throw new BadRequestException("Cannot cancel a RECEIVED PO.");
        }

        po.setStatus(PurchaseOrderStatus.CANCELLED);

        List<PurchaseOrderItem> items = itemRepo.findAllByPo_PoId(poId);
        return toResponse(po, items);
    }

    // ----------------------------
    // RECEIVE (APPROVED -> RECEIVED, or SUBMITTED -> RECEIVED if you want)
    // Updates inventory + writes InventoryTransaction (IN)
    // ----------------------------
    @Override
    @Transactional
    public PurchaseOrderResponse receive(Long poId, ReceivePurchaseOrderRequest req) {
        PurchaseOrder po = requirePo(poId);

        if (!(po.getStatus() == PurchaseOrderStatus.APPROVED || po.getStatus() == PurchaseOrderStatus.PENDING_APPROVAL)) {
            throw new BadRequestException("PO must be APPROVED (or PENDING_APPROVAL) to receive.");
        }

        Map<Long, Integer> receiveMap = req.lines().stream()
                .collect(Collectors.toMap(ReceivePurchaseOrderRequest.ReceiveLine::poItemId,
                        ReceivePurchaseOrderRequest.ReceiveLine::receiveQty,
                        Integer::sum));

        List<PurchaseOrderItem> items = itemRepo.findAllByPo_PoId(poId);

        // Validate line ids belong to this PO + validate remaining qty
        for (PurchaseOrderItem item : items) {
            Integer receiveQty = receiveMap.get(item.getPoItemId());
            if (receiveQty == null) continue;

            int already = item.getReceivedQty() == null ? 0 : item.getReceivedQty();
            int ordered = item.getOrderedQty();
            int remaining = ordered - already;

            if (receiveQty <= 0) throw new BadRequestException("Receive qty must be > 0");
            if (receiveQty > remaining) {
                throw new BadRequestException("Receive qty exceeds remaining for item " + item.getPoItemId());
            }
        }

        // Apply receiving: update item received_qty, update inventory, write tx
        for (PurchaseOrderItem item : items) {
            Integer receiveQty = receiveMap.get(item.getPoItemId());
            if (receiveQty == null) continue;

            int newReceived = (item.getReceivedQty() == null ? 0 : item.getReceivedQty()) + receiveQty;
            item.setReceivedQty(newReceived);
            itemRepo.save(item);

            // Update inventory row for (warehouse, product)
            Long warehouseId = po.getWarehouse().getWarehouseId();
            Long productId = item.getProduct().getProductId();

            Inventory inv = inventoryRepo.findByWarehouse_WarehouseIdAndProduct_ProductId(warehouseId, productId)
                    .orElseGet(() -> Inventory.builder()
                            .warehouse(po.getWarehouse())
                            .product(item.getProduct())
                            .qtyOnHand(0)
                            .build());

            int current = inv.getQtyOnHand() == null ? 0 : inv.getQtyOnHand();
            inv.setQtyOnHand(current + receiveQty);
            inventoryRepo.save(inv);

            // Write transaction
            InventoryTransaction tx = InventoryTransaction.builder()
                    .warehouse(po.getWarehouse())
                    .product(item.getProduct())
                    .type(InventoryTransactionType.IN)
                    .quantity(receiveQty)
                    .build();
            txRepo.save(tx);
        }

        // If fully received, mark PO RECEIVED
        boolean fullyReceived = items.stream().allMatch(i -> {
            int ordered = i.getOrderedQty() == null ? 0 : i.getOrderedQty();
            int rec = i.getReceivedQty() == null ? 0 : i.getReceivedQty();
            return rec >= ordered;
        });

        if (fullyReceived) {
            po.setStatus(PurchaseOrderStatus.CONFIRMED);
        }

        recalcTotal(po, items);
        return toResponse(po, items);
    }

    // ----------------------------
    // RECEIVING STATUS
    // ----------------------------
    @Override
    public ReceivingStatusResponse receivingStatus(Long poId) {
        PurchaseOrder po = requirePo(poId);
        List<PurchaseOrderItem> items = itemRepo.findAllByPo_PoId(poId);

        List<ReceivingStatusResponse.ReceivingLineStatus> lines = items.stream().map(i -> {
            int ordered = i.getOrderedQty() == null ? 0 : i.getOrderedQty();
            int received = i.getReceivedQty() == null ? 0 : i.getReceivedQty();
            int remaining = Math.max(0, ordered - received);

            return new ReceivingStatusResponse.ReceivingLineStatus(
                    i.getPoItemId(),
                    i.getProduct().getProductId(),
                    i.getProduct().getSku(),
                    i.getProduct().getName(),
                    ordered,
                    received,
                    remaining
            );
        }).toList();

        return new ReceivingStatusResponse(po.getPoId(), lines);
    }

    // ----------------------------
    // Helpers
    // ----------------------------
    private PurchaseOrder requirePo(Long poId) {
        return poRepo.findById(poId)
                .orElseThrow(() -> new NotFoundException("Purchase Order not found: " + poId));
    }

    private PurchaseOrderItem requireItem(Long poId, Long itemId) {
        PurchaseOrderItem item = itemRepo.findById(itemId)
                .orElseThrow(() -> new NotFoundException("PO item not found: " + itemId));

        if (!Objects.equals(item.getPo().getPoId(), poId)) {
            throw new BadRequestException("Item " + itemId + " does not belong to PO " + poId);
        }
        return item;
    }

    private void ensureDraft(PurchaseOrder po) {
        if (po.getStatus() != PurchaseOrderStatus.DRAFT) {
            throw new BadRequestException("Only DRAFT purchase orders can be modified.");
        }
    }

    private void recalcTotal(PurchaseOrder po, List<PurchaseOrderItem> items) {
        BigDecimal total = BigDecimal.ZERO;

        for (PurchaseOrderItem i : items) {
            if (i.getUnitCost() == null || i.getOrderedQty() == null) continue;
            total = total.add(i.getUnitCost().multiply(BigDecimal.valueOf(i.getOrderedQty())));
        }

        po.setTotalAmount(total);
        poRepo.save(po);
    }

    private PurchaseOrderResponse toResponse(PurchaseOrder po, List<PurchaseOrderItem> items) {
        List<PurchaseOrderItemResponse> itemResponses = items.stream()
                .map(this::toItemResponse)
                .toList();

        return new PurchaseOrderResponse(
                po.getPoId(),
                po.getSupplier().getSupplierId(),
                po.getSupplier().getName(),
                po.getWarehouse().getWarehouseId(),
                po.getWarehouse().getLocationName(),
                po.getBuyer().getId(),
                po.getBuyer().getEmail(),
                po.getStatus(),
                po.getTotalAmount(),
                po.getCreatedOn(),
                itemResponses
        );
    }

    private PurchaseOrderItemResponse toItemResponse(PurchaseOrderItem item) {
        return new PurchaseOrderItemResponse(
                item.getPoItemId(),
                item.getProduct().getProductId(),
                item.getProduct().getSku(),
                item.getProduct().getName(),
                item.getOrderedQty(),
                item.getReceivedQty(),
                item.getUnitCost()
        );
    }
}

