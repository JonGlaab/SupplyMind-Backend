package com.supplymind.platform_core.controller.core;

import com.supplymind.platform_core.dto.core.supplierproduct.*;
import com.supplymind.platform_core.service.core.SupplierProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/core/supplier-products")
@RequiredArgsConstructor
public class SupplierProductController {

    private final SupplierProductService service;

    // Assign product to supplier with lead time + cost price
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('PROCUREMENT_OFFICER','MANAGER')")
    public SupplierProductResponse create(@Valid @RequestBody SupplierProductCreateRequest req) {
        return service.create(req);
    }

    // View supplier product catalogue
    @GetMapping
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('PROCUREMENT_OFFICER','MANAGER','STAFF','ADMIN')")
    public List<SupplierProductResponse> listBySupplier(@RequestParam Long supplierId) {
        return service.listBySupplier(supplierId);
    }

    // Update lead time / cost price only
    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('PROCUREMENT_OFFICER','MANAGER')")
    public SupplierProductResponse update(@PathVariable Long id,
                                          @Valid @RequestBody SupplierProductUpdateRequest req) {
        return service.update(id, req);
    }

    // Remove assignment
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('PROCUREMENT_OFFICER','MANAGER')")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}

