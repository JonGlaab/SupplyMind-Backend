package com.supplymind.platform_core.service.impl.core;

import com.supplymind.platform_core.dto.core.product.*;
import com.supplymind.platform_core.exception.ConflictException;
import com.supplymind.platform_core.exception.NotFoundException;
import com.supplymind.platform_core.model.core.Product;
import com.supplymind.platform_core.repository.core.InventoryRepository;
import com.supplymind.platform_core.repository.core.ProductRepository;
import com.supplymind.platform_core.service.core.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository repo;
    private final InventoryRepository inventoryRepository;

    @Override
    public ProductResponse create(ProductCreateRequest req) {
        String sku = req.sku().trim();

        if (repo.existsBySku(sku)) {
            throw new ConflictException("SKU already exists: " + sku);
        }

        Product p = Product.builder()
                .sku(sku)
                .name(req.name().trim())
                .category(req.category() == null ? null : req.category().trim())
                .unitPrice(req.unitPrice())
                .reorderPoint(req.reorderPoint())
                .description(req.description() != null ? req.description().trim() : null)
                .build();

        return toResponse(repo.save(p));
    }

    @Override
    public Page<ProductResponse> list(Pageable pageable) {
        // 1. Fetch the combined data in ONE trip to the DB
        Page<Object[]> results = repo.findAllWithTotalStock(pageable);

        // 2. Map the Object array to your ProductResponse
        return results.map(row -> {
            Product p = (Product) row[0];
            // row[1] is the result of SUM(), which comes back as a Long or 0
            Long totalQty = (row[1] != null) ? (Long) row[1] : 0L;

            // Use a dedicated helper to avoid the "toResponse" loop
            return toResponseWithQty(p, totalQty.intValue());
        });
    }

    private ProductResponse toResponseWithQty(Product p, Integer qtyOnHand) {
        return new ProductResponse(
                p.getProductId(),
                p.getSku(),
                p.getName(),
                p.getCategory(),
                p.getUnitPrice(),
                p.getReorderPoint(),
                p.getDescription(),
                p.getCreatedAt(),
                p.getUpdatedAt(),
                qtyOnHand,
                p.getReorderPoint()
        );
    }

    @Override
    public ProductResponse get(Long productId) {
        Product p = repo.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found: " + productId));
        return toResponse(p);
    }

    @Override
    public ProductResponse update(Long productId, ProductUpdateRequest req) {
        Product p = repo.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found: " + productId));

        if (req.name() != null) p.setName(req.name().trim());
        if (req.category() != null) p.setCategory(req.category().trim());
        if (req.unitPrice() != null) p.setUnitPrice(req.unitPrice());
        if (req.reorderPoint() != null) p.setReorderPoint(req.reorderPoint());
        if (req.description() != null) p.setDescription(req.description().trim());

        return toResponse(repo.save(p));
    }

    @Override
    public void delete(Long productId) {
        if (!repo.existsById(productId)) {
            throw new NotFoundException("Product not found: " + productId);
        }
        repo.deleteById(productId);
    }

    private ProductResponse toResponse(Product p) {
        Integer qtyOnHand = inventoryRepository.findTotalQuantityByProductId(p.getProductId());
        return toResponseWithQty(p, qtyOnHand);
    }
}