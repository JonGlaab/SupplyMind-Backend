package com.supplymind.platform_core.service.impl.core;

import com.supplymind.platform_core.common.enums.InventoryTransactionType;
import com.supplymind.platform_core.dto.core.inventory.*;
import com.supplymind.platform_core.exception.BadRequestException;
import com.supplymind.platform_core.exception.NotFoundException;
import com.supplymind.platform_core.model.core.*;
import com.supplymind.platform_core.repository.core.*;
import com.supplymind.platform_core.service.core.InventoryService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepo;
    private final InventoryTransactionRepository txRepo;
    private final WarehouseRepository warehouseRepo;
    private final ProductRepository productRepo;
    private final SupplierProductRepository supplierProductRepo;

    @Override
    @Transactional(readOnly = true)
    public Page<InventoryResponse> listByWarehouse(Long warehouseId, Pageable pageable) {
        warehouseRepo.findById(warehouseId)
                .orElseThrow(() -> new NotFoundException("Warehouse not found: " + warehouseId));

        return inventoryRepo.findAllByWarehouse_WarehouseId(warehouseId, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InventoryTransactionResponse> listTransactions(Long warehouseId, Long productId, Pageable pageable) {

        if (warehouseId != null && productId != null) {
            return txRepo.findAllByWarehouse_WarehouseIdAndProduct_ProductId(warehouseId, productId, pageable)
                    .map(this::toTxResponse);
        }

        if (warehouseId != null) {
            return txRepo.findAllByWarehouse_WarehouseId(warehouseId, pageable)
                    .map(this::toTxResponse);
        }

        if (productId != null) {
            return txRepo.findAllByProduct_ProductId(productId, pageable)
                    .map(this::toTxResponse);
        }

        return txRepo.findAll(pageable).map(this::toTxResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InventoryResponse> findLowStock(Long warehouseId, Long supplierId, Pageable pageable) {
        if (supplierId != null) {
            List<SupplierProduct> supplierProducts = supplierProductRepo.findAllBySupplier_SupplierId(supplierId);
            List<Long> productIds = supplierProducts.stream()
                    .map(sp -> sp.getProduct().getProductId())
                    .collect(Collectors.toList());

            if (productIds.isEmpty()) {
                return new PageImpl<>(Collections.emptyList(), pageable, 0);
            }

            return inventoryRepo.findLowStockForProducts(warehouseId, productIds, pageable)
                    .map(this::toResponse);
        } else {
            return inventoryRepo.findLowStock(warehouseId, pageable)
                    .map(this::toResponse);
        }
    }

    private InventoryTransactionResponse toTxResponse(com.supplymind.platform_core.model.core.InventoryTransaction tx) {
        return new InventoryTransactionResponse(
                tx.getTransactionId(),
                tx.getWarehouse().getWarehouseId(),
                tx.getProduct().getProductId(),
                tx.getProduct().getSku(),
                tx.getProduct().getName(),
                tx.getType(),
                tx.getQuantity(),
                tx.getTimestamp()
        );
    }

    @Override
    @Transactional
    public void recordTransaction(InventoryTransactionRequest req) {

        Warehouse warehouse = warehouseRepo.findById(req.warehouseId())
                .orElseThrow(() -> new NotFoundException("Warehouse not found: " + req.warehouseId()));

        Product product = productRepo.findById(req.productId())
                .orElseThrow(() -> new NotFoundException("Product not found: " + req.productId()));

        Inventory inv = inventoryRepo
                .findByWarehouse_WarehouseIdAndProduct_ProductId(req.warehouseId(), req.productId())
                .orElseGet(() -> Inventory.builder()
                        .warehouse(warehouse)
                        .product(product)
                        .qtyOnHand(0)
                        .build()
                );

        int current = inv.getQtyOnHand() == null ? 0 : inv.getQtyOnHand();
        int qty = req.quantity();

        int updatedQty = switch (req.type()) {
            case IN, RETURN -> current + qty;
            case OUT -> current - qty;
            case ADJUST -> qty;
        };

        if (updatedQty < 0) {
            throw new BadRequestException(
                    "Inventory cannot go below 0. Current=" + current +
                            ", requested=" + req.type() + " " + qty
            );
        }

        inv.setQtyOnHand(updatedQty);
        inventoryRepo.save(inv);

        InventoryTransaction tx = InventoryTransaction.builder()
                .warehouse(warehouse)
                .product(product)
                .type(req.type())
                .quantity(qty)
                .build();

        txRepo.save(tx);
    }

    private InventoryResponse toResponse(Inventory inv) {
        Product p = inv.getProduct();

        Supplier supplier = null;
        if (p.getSupplierProducts() != null && !p.getSupplierProducts().isEmpty()) {
            supplier = p.getSupplierProducts().iterator().next().getSupplier();
        }

        return new InventoryResponse(
                inv.getInventoryId(),
                inv.getWarehouse().getWarehouseId(),
                inv.getWarehouse().getLocationName(),
                p.getProductId(),
                p.getSku(),
                p.getName(),
                inv.getQtyOnHand(),
                p.getReorderPoint(),
                inv.getMaxStockLevel(),
                supplier != null ? supplier.getSupplierId() : null,
                supplier != null ? supplier.getName() : null,
                p.getUnitPrice(),
                inv.getCreatedAt(),
                inv.getUpdatedAt()
        );
    }

    @Override
    @Transactional
    public void executeImmediateTransfer(InventoryTransferRequest req) {
        LocalDateTime now = LocalDateTime.now();

        Inventory sourceInv = inventoryRepo.findByWarehouse_WarehouseIdAndProduct_ProductId(
                        req.fromWarehouseId(), req.productId())
                .orElseThrow(() -> new EntityNotFoundException("Product not found in source warehouse"));

        if (sourceInv.getQtyOnHand() < req.quantity()) {
            throw new BadRequestException("Insufficient stock in source warehouse. Available: "
                    + sourceInv.getQtyOnHand() + ", Requested: " + req.quantity());
        }

        Inventory destInv = inventoryRepo.findByWarehouse_WarehouseIdAndProduct_ProductId(
                        req.toWarehouseId(), req.productId())
                .orElseGet(() -> {
                    Inventory newInv = new Inventory();
                    newInv.setWarehouse(warehouseRepo.getReferenceById(req.toWarehouseId()));
                    newInv.setProduct(productRepo.getReferenceById(req.productId()));
                    newInv.setQtyOnHand(0);
                    return newInv;
                });

        sourceInv.setQtyOnHand(sourceInv.getQtyOnHand() - req.quantity());
        destInv.setQtyOnHand(destInv.getQtyOnHand() + req.quantity());

        inventoryRepo.save(sourceInv);
        inventoryRepo.save(destInv);

        saveTransaction(req.fromWarehouseId(), req.productId(), req.quantity(), InventoryTransactionType.OUT);
        saveTransaction(req.toWarehouseId(), req.productId(), req.quantity(), InventoryTransactionType.IN);

        txRepo.flush();
    }

    private void saveTransaction(Long whId, Long prodId, Integer qty, InventoryTransactionType type) {
        txRepo.save(InventoryTransaction.builder()
                .warehouse(warehouseRepo.getReferenceById(whId))
                .product(productRepo.getReferenceById(prodId))
                .quantity(qty)
                .type(type)
                .build());
    }

    @Override
    public Page<InventorySlimResponse> listByWarehouse(Long warehouseId, String sku, Pageable pageable) {
        Page<Inventory> inventoryPage;

        if (sku != null && !sku.trim().isEmpty()) {
            inventoryPage = inventoryRepo.findByWarehouse_WarehouseIdAndProduct_Sku(warehouseId, sku, pageable);
        } else {
            inventoryPage = inventoryRepo.findAllByWarehouse_WarehouseId(warehouseId, pageable);
        }

        return inventoryPage.map(inv -> new InventorySlimResponse(
                inv.getInventoryId(),
                inv.getWarehouse().getWarehouseId(),
                inv.getWarehouse().getLocationName(),
                inv.getProduct().getProductId(),
                inv.getProduct().getSku(),
                inv.getProduct().getName(),
                inv.getQtyOnHand(),
                inv.getProduct().getReorderPoint(),
                inv.getProduct().getUnitPrice(),
                inv.getUpdatedAt()
        ));
    }
}