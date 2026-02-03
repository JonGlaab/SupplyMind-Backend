package com.supplymind.platform_core.service.impl.core;

import com.supplymind.platform_core.common.enums.InventoryTransactionType;
import com.supplymind.platform_core.dto.core.inventory.InventoryResponse;
import com.supplymind.platform_core.dto.core.inventory.InventoryTransactionRequest;
import com.supplymind.platform_core.dto.core.inventory.InventoryTransactionResponse;
import com.supplymind.platform_core.exception.BadRequestException;
import com.supplymind.platform_core.exception.NotFoundException;
import com.supplymind.platform_core.model.core.Inventory;
import com.supplymind.platform_core.model.core.InventoryTransaction;
import com.supplymind.platform_core.model.core.Product;
import com.supplymind.platform_core.model.core.Warehouse;
import com.supplymind.platform_core.repository.core.InventoryRepository;
import com.supplymind.platform_core.repository.core.InventoryTransactionRepository;
import com.supplymind.platform_core.repository.core.ProductRepository;
import com.supplymind.platform_core.repository.core.WarehouseRepository;
import com.supplymind.platform_core.service.core.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepo;
    private final InventoryTransactionRepository txRepo;
    private final WarehouseRepository warehouseRepo;
    private final ProductRepository productRepo;

    @Override
    @Transactional(readOnly = true)
    public Page<InventoryResponse> listByWarehouse(Long warehouseId, Pageable pageable) {
        // Validate warehouse exists
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
            case ADJUST -> qty; // absolute set, not delta
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
        return new InventoryResponse(
                inv.getInventoryId(),
                inv.getWarehouse().getWarehouseId(),
                inv.getProduct().getProductId(),
                inv.getProduct().getSku(),
                inv.getProduct().getName(),
                inv.getQtyOnHand(),
                inv.getCreatedAt(),
                inv.getUpdatedAt()
        );
    }
}