package com.supplymind.platform_core.service.impl.core;

import com.supplymind.platform_core.dto.core.supplier.*;
import com.supplymind.platform_core.exception.ConflictException;
import com.supplymind.platform_core.exception.NotFoundException;
import com.supplymind.platform_core.model.core.Supplier;
import com.supplymind.platform_core.repository.core.SupplierRepository;
import com.supplymind.platform_core.service.core.SupplierService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SupplierServiceImpl implements SupplierService {

    private final SupplierRepository repo;

    @Override
    public SupplierResponse create(SupplierCreateRequest req) {
        String name = req.name().trim();

        // Optional uniqueness rule
        if (repo.existsByNameIgnoreCase(name)) {
            throw new ConflictException("Supplier already exists: " + name);
        }

        Supplier s = Supplier.builder()
                .name(name)
                .contactEmail(req.contactEmail() == null ? null : req.contactEmail().trim())
                .phone(req.phone() == null ? null : req.phone().trim())
                .address(req.address())
                .build();

        return toResponse(repo.save(s));
    }

    @Override
    public Page<SupplierResponse> list(Pageable pageable) {
        return repo.findAll(pageable).map(this::toResponse);
    }

    @Override
    public SupplierResponse get(Long supplierId) {
        Supplier s = repo.findById(supplierId)
                .orElseThrow(() -> new NotFoundException("Supplier not found: " + supplierId));
        return toResponse(s);
    }

    @Override
    public SupplierResponse update(Long supplierId, SupplierUpdateRequest req) {
        Supplier s = repo.findById(supplierId)
                .orElseThrow(() -> new NotFoundException("Supplier not found: " + supplierId));

        if (req.name() != null) s.setName(req.name().trim());
        if (req.contactEmail() != null) s.setContactEmail(req.contactEmail().trim());
        if (req.phone() != null) s.setPhone(req.phone().trim());
        if (req.address() != null) s.setAddress(req.address());

        return toResponse(repo.save(s));
    }

    @Override
    public void delete(Long supplierId) {
        Supplier s = repo.findById(supplierId)
                .orElseThrow(() -> new NotFoundException("Supplier not found: " + supplierId));

        repo.delete(s); // triggers soft delete via @SQLDelete
    }

    private SupplierResponse toResponse(Supplier s) {
        return new SupplierResponse(
                s.getSupplierId(),
                s.getName(),
                s.getContactEmail(),
                s.getPhone(),
                s.getAddress(),
                s.getCreatedAt(),
                s.getUpdatedAt()
        );
    }
}

