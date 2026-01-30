package com.supplymind.platform_core.service.impl.core;

import com.supplymind.platform_core.dto.core.supplierproduct.*;
import com.supplymind.platform_core.exception.ConflictException;
import com.supplymind.platform_core.exception.NotFoundException;
import com.supplymind.platform_core.model.core.Product;
import com.supplymind.platform_core.model.core.Supplier;
import com.supplymind.platform_core.model.core.SupplierProduct;
import com.supplymind.platform_core.repository.core.ProductRepository;
import com.supplymind.platform_core.repository.core.SupplierProductRepository;
import com.supplymind.platform_core.repository.core.SupplierRepository;
import com.supplymind.platform_core.service.core.SupplierProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SupplierProductServiceImpl implements SupplierProductService {

    private final SupplierProductRepository repo;
    private final SupplierRepository supplierRepo;
    private final ProductRepository productRepo;

    @Override
    public SupplierProductResponse create(SupplierProductCreateRequest req) {

        if (repo.existsBySupplier_SupplierIdAndProduct_ProductId(req.supplierId(), req.productId())) {
            throw new ConflictException("Supplier already has this product assigned.");
        }

        Supplier supplier = supplierRepo.findById(req.supplierId())
                .orElseThrow(() -> new NotFoundException("Supplier not found: " + req.supplierId()));

        Product product = productRepo.findById(req.productId())
                .orElseThrow(() -> new NotFoundException("Product not found: " + req.productId()));

        SupplierProduct sp = SupplierProduct.builder()
                .supplier(supplier)
                .product(product)
                .leadTimeDays(req.leadTimeDays())
                .costPrice(req.costPrice())
                .build();

        return toResponse(repo.save(sp));
    }

    @Override
    public List<SupplierProductResponse> listBySupplier(Long supplierId) {
        return repo.findAllBySupplier_SupplierId(supplierId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public SupplierProductResponse update(Long id, SupplierProductUpdateRequest req) {
        SupplierProduct sp = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("SupplierProduct not found: " + id));

        if (req.leadTimeDays() != null) sp.setLeadTimeDays(req.leadTimeDays());
        if (req.costPrice() != null) sp.setCostPrice(req.costPrice());

        return toResponse(repo.save(sp));
    }

    @Override
    public void delete(Long id) {
        SupplierProduct sp = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("SupplierProduct not found: " + id));
        repo.delete(sp);
    }

    private SupplierProductResponse toResponse(SupplierProduct sp) {
        return new SupplierProductResponse(
                sp.getId(),
                sp.getSupplier().getSupplierId(),
                sp.getSupplier().getName(),
                sp.getProduct().getProductId(),
                sp.getProduct().getSku(),
                sp.getProduct().getName(),
                sp.getLeadTimeDays(),
                sp.getCostPrice()
        );
    }
}

