package com.supplymind.platform_core.controller.core;

import com.supplymind.platform_core.common.util.PaginationDefaults;
import com.supplymind.platform_core.dto.core.supplier.*;
import com.supplymind.platform_core.service.core.SupplierService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/core/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('PROCUREMENT_OFFICER','MANAGER')")
    public SupplierResponse create(@Valid @RequestBody SupplierCreateRequest req) {
        return service.create(req);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PROCUREMENT_OFFICER','MANAGER','STAFF','ADMIN')")
    public Page<SupplierResponse> list(
            @PageableDefault(size = PaginationDefaults.DEFAULT_PAGE_SIZE, sort = "supplierId") Pageable pageable
    ) {
        return service.list(pageable);
    }

    @GetMapping("/{supplierId}")
    @PreAuthorize("hasAnyRole('PROCUREMENT_OFFICER','MANAGER','STAFF','ADMIN')")
    public SupplierResponse get(@PathVariable Long supplierId) {
        return service.get(supplierId);
    }

    @PatchMapping("/{supplierId}")
    @PreAuthorize("hasAnyRole('PROCUREMENT_OFFICER','MANAGER')")
    public SupplierResponse update(@PathVariable Long supplierId,
                                   @Valid @RequestBody SupplierUpdateRequest req) {
        return service.update(supplierId, req);
    }

    @DeleteMapping("/{supplierId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('PROCUREMENT_OFFICER','MANAGER')")
    public void delete(@PathVariable Long supplierId) {
        service.delete(supplierId);
    }
}

