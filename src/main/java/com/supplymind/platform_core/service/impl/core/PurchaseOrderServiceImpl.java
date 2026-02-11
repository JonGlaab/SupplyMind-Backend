package com.supplymind.platform_core.service.impl.core;

import com.supplymind.platform_core.common.enums.InventoryTransactionType;
import com.supplymind.platform_core.common.enums.PurchaseOrderStatus;
import com.supplymind.platform_core.dto.core.purchaseorder.*;
import com.supplymind.platform_core.exception.BadRequestException;
import com.supplymind.platform_core.exception.NotFoundException;
import com.supplymind.platform_core.model.auth.User;
import com.supplymind.platform_core.model.core.*;
import com.supplymind.platform_core.repository.core.*;
import com.supplymind.platform_core.service.auth.AuthService;
import com.supplymind.platform_core.service.common.PdfGenerationService;
import com.supplymind.platform_core.service.common.StorageService;
import com.supplymind.platform_core.service.core.PurchaseOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PurchaseOrderServiceImpl implements PurchaseOrderService {

    private final PurchaseOrderRepository poRepo;
    private final PurchaseOrderItemRepository itemRepo;

    private final SupplierRepository supplierRepo;
    private final WarehouseRepository warehouseRepo;
    private final ProductRepository productRepo;

    private final InventoryRepository inventoryRepo;
    private final InventoryTransactionRepository txRepo;

    private final AuthService authService;

    private final PdfGenerationService pdfGenerationService;
    private final StorageService storageService;

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

        // Use the currently authenticated user as the buyer
        User buyer = authService.getCurrentUser()
                .orElseThrow(() -> new BadRequestException("Could not identify current user to assign as buyer."));

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
    @Transactional(readOnly = true)
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
            page = poRepo.findAllWithDetails(pageable);
        }

        List<PurchaseOrderResponse> dtoList = page.getContent().stream()
                .map(po -> toResponse(po, itemRepo.findAllByPo_PoId(po.getPoId())))
                .collect(Collectors.toList());

        return new PageImpl<>(dtoList, pageable, page.getTotalElements());
    }

    // ----------------------------
    // GET DETAILS
    // ----------------------------
    @Override
    @Transactional(readOnly = true)
    public PurchaseOrderResponse get(Long poId) {
        PurchaseOrder po = poRepo.findById(poId)
                .orElseThrow(() -> new NotFoundException("Purchase Order not found: " + poId));

        List<PurchaseOrderItem> items = itemRepo.findAllByPo_PoId(poId);
        return toResponse(po, items);
    }

    @Override
    @Transactional
    public void delete(Long poId) {
        PurchaseOrder po = requirePo(poId);
        ensureDraft(po);
        itemRepo.deleteAllByPoPoId(poId);
        poRepo.delete(po);
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

    @Override
    @Transactional
    public PurchaseOrderResponse approve(Long poId) {
        PurchaseOrder po = requirePo(poId);

        if (po.getStatus() != PurchaseOrderStatus.PENDING_APPROVAL) {
            throw new BadRequestException("Only PENDING_APPROVAL POs can be approved.");
        }

        User approver = authService.getCurrentUser()
                .orElseThrow(() -> new BadRequestException("Could not identify current user to set as approver."));
        po.setApprover(approver);

        generateAndStorePdf(po, approver);

        po.setStatus(PurchaseOrderStatus.APPROVED);
        poRepo.save(po);

        List<PurchaseOrderItem> items = itemRepo.findAllByPo_PoId(poId);
        return toResponse(po, items);
    }

    @Override
    @Transactional
    public PurchaseOrderResponse cancel(Long poId) {
        PurchaseOrder po = requirePo(poId);

        if (po.getStatus() == PurchaseOrderStatus.COMPLETED) {
            throw new BadRequestException("Cannot cancel a COMPLETED PO.");
        }

        po.setStatus(PurchaseOrderStatus.CANCELLED);

        List<PurchaseOrderItem> items = itemRepo.findAllByPo_PoId(poId);
        return toResponse(po, items);
    }

    @Override
    @Transactional
    public PurchaseOrderResponse receive(Long poId, ReceivePurchaseOrderRequest req) {

        try {
        PurchaseOrder po = requirePo(poId);

        if (po.getStatus() != PurchaseOrderStatus.DELIVERED) {
            throw new BadRequestException("PO must be in DELIVERED status to process incoming stock.");
        }

        Map<Long, Integer> receiveMap = req.lines().stream()
                .collect(Collectors.toMap(
                        ReceivePurchaseOrderRequest.ReceiveLine::poItemId,
                        ReceivePurchaseOrderRequest.ReceiveLine::receiveQty,
                        Integer::sum));

        List<PurchaseOrderItem> items = itemRepo.findAllByPo_PoId(poId);

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

        for (PurchaseOrderItem item : items) {
            Integer receiveQty = receiveMap.get(item.getPoItemId());
            if (receiveQty == null) continue;

            int newReceived = (item.getReceivedQty() == null ? 0 : item.getReceivedQty()) + receiveQty;
            item.setReceivedQty(newReceived);
            itemRepo.save(item);

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

            InventoryTransaction tx = InventoryTransaction.builder()
                    .warehouse(po.getWarehouse())
                    .product(item.getProduct())
                    .type(InventoryTransactionType.IN)
                    .quantity(receiveQty)
                    .build();
            txRepo.save(tx);
        }

        boolean fullyReceived = items.stream().allMatch(i -> {
            int ordered = i.getOrderedQty() == null ? 0 : i.getOrderedQty();
            int rec = i.getReceivedQty() == null ? 0 : i.getReceivedQty();
            return rec >= ordered;
        });

        if (fullyReceived) {
            po.setStatus(PurchaseOrderStatus.COMPLETED);
        }

        recalcTotal(po, items);
        return toResponse(po, items);
        }
        catch (Exception e) {
            System.err.println("!!! RECEIVE ERROR !!!: " + e.getMessage());
            throw e;
        }
    }

    @Override
    @Transactional
    public PurchaseOrderResponse updateStatus(Long poId, PurchaseOrderStatusUpdateRequest req) {
        PurchaseOrder po = requirePo(poId);

        PurchaseOrderStatus current = po.getStatus();
        PurchaseOrderStatus next = req.status();

        if (current == PurchaseOrderStatus.COMPLETED || current == PurchaseOrderStatus.CANCELLED) {
            throw new BadRequestException("Cannot change status for a " + current + " PO.");
        }

        if (next == PurchaseOrderStatus.DELAY_EXPECTED) {
            if (!(current == PurchaseOrderStatus.CONFIRMED || current == PurchaseOrderStatus.SHIPPED)) {
                throw new BadRequestException("DELAY_EXPECTED can only be set after CONFIRMED or SHIPPED.");
            }
            po.setStatus(PurchaseOrderStatus.DELAY_EXPECTED);
            return toResponse(po, itemRepo.findAllByPo_PoId(poId));
        }

        boolean ok = switch (current) {
            case DRAFT -> next == PurchaseOrderStatus.PENDING_APPROVAL; // use /submit normally
            case PENDING_APPROVAL -> next == PurchaseOrderStatus.APPROVED; // use /approve normally
            case APPROVED -> next == PurchaseOrderStatus.EMAIL_SENT;
            case EMAIL_SENT -> next == PurchaseOrderStatus.SUPPLIER_REPLIED;
            case SUPPLIER_REPLIED -> next == PurchaseOrderStatus.CONFIRMED;
            case CONFIRMED -> next == PurchaseOrderStatus.SHIPPED;
            case SHIPPED -> next == PurchaseOrderStatus.DELIVERED;
            case DELIVERED -> next == PurchaseOrderStatus.COMPLETED;
            case DELAY_EXPECTED -> next == PurchaseOrderStatus.SHIPPED || next == PurchaseOrderStatus.DELIVERED;
            default -> false;
        };

        if (!ok) {
            throw new BadRequestException("Invalid status transition: " + current + " -> " + next);
        }

        // If moving to APPROVED, generate PDF
        if (next == PurchaseOrderStatus.APPROVED) {
            User approver = authService.getCurrentUser()
                    .orElseThrow(() -> new BadRequestException("Could not identify current user to set as approver."));
            po.setApprover(approver);
            generateAndStorePdf(po, approver);
        }

        po.setStatus(next);
        return toResponse(po, itemRepo.findAllByPo_PoId(poId));
    }

    @Override
    @Transactional(readOnly = true)
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
                .filter(item -> item.getProduct() != null)
                .map(this::toItemResponse)
                .toList();

        Supplier supplier = po.getSupplier();
        Warehouse warehouse = po.getWarehouse();
        User buyer = po.getBuyer();
        User approver = po.getApprover();

        String pdfUrl = po.getPdfUrl();
        if (pdfUrl != null && !pdfUrl.startsWith("http")) {
            try {
                pdfUrl = storageService.presignGetUrl(pdfUrl);
                log.info("Generated presigned URL for key {}: {}", po.getPdfUrl(), pdfUrl);
            } catch (Exception e) {
                log.error("Failed to generate presigned URL for key: {}", po.getPdfUrl(), e);
            }
        }

        return new PurchaseOrderResponse(
                po.getPoId(),
                supplier != null ? supplier.getSupplierId() : null,
                supplier != null ? supplier.getName() : null,
                supplier != null ? supplier.getContactEmail() : null,
                warehouse != null ? warehouse.getWarehouseId() : null,
                warehouse != null ? warehouse.getLocationName() : null,
                buyer != null ? buyer.getId() : null,
                buyer != null ? buyer.getEmail() : null,
                approver != null ? approver.getEmail() : null,
                po.getStatus(),
                po.getTotalAmount(),
                po.getCreatedOn(),
                pdfUrl,
                itemResponses
        );
    }

    private PurchaseOrderItemResponse toItemResponse(PurchaseOrderItem item) {
        Product product = item.getProduct();
        return new PurchaseOrderItemResponse(
                item.getPoItemId(),
                product != null ? product.getProductId() : null,
                product != null ? product.getSku() : null,
                product != null ? product.getName() : null,
                item.getOrderedQty(),
                item.getReceivedQty(),
                item.getUnitCost()
        );
    }

    private void generateAndStorePdf(PurchaseOrder po, User approver) {
        try {
            // 1. Generate PDF
            File pdfFile = pdfGenerationService.generatePurchaseOrderPdf(po, approver, true);

            // 2. Upload to Storage
            String objectKey = storageService.buildObjectKey(
                    "invoice",
                    po.getSupplier().getSupplierId(),
                    pdfFile.getName()
            );
            storageService.uploadFile(objectKey, pdfFile, MediaType.APPLICATION_PDF_VALUE);

            // 3. Update PO with URL (Storing Object Key now)
            po.setPdfUrl(objectKey);

            // 4. Clean up temp file
            if (pdfFile.exists()) {
                if (!pdfFile.delete()) {
                    log.warn("Could not delete temporary PDF file: {}", pdfFile.getAbsolutePath());
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to approve and store PO: " + e.getMessage(), e);
        }
    }
}
