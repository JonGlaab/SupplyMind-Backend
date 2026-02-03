package com.supplymind.platform_core.controller.core;

import com.supplymind.platform_core.common.util.PaginationDefaults;
import com.supplymind.platform_core.dto.core.product.ProductCreateRequest;
import com.supplymind.platform_core.dto.core.product.ProductResponse;
import com.supplymind.platform_core.dto.core.product.ProductUpdateRequest;
import com.supplymind.platform_core.service.core.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/core/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService service;

    // Create product: Procurement + Manager
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    //TODO: ADMIN preauthorized to delete for dev testing. Remove ADMIN for production.
    @PreAuthorize("hasAnyRole('PROCUREMENT_OFFICER','MANAGER', 'ADMIN')")
    public ProductResponse create(@Valid @RequestBody ProductCreateRequest req) {
        return service.create(req);
    }

    // List products: readable by internal roles
    @GetMapping
    @PreAuthorize("hasAnyRole('PROCUREMENT_OFFICER','MANAGER','STAFF','ADMIN')")
    public Page<ProductResponse> list(
            @PageableDefault(size = PaginationDefaults.DEFAULT_PAGE_SIZE, sort = "productId") Pageable pageable
    ) {
        return service.list(pageable);
    }

    // Get product: readable by internal roles
    @GetMapping("/{productId}")
    @PreAuthorize("hasAnyRole('PROCUREMENT_OFFICER','MANAGER','STAFF','ADMIN')")
    public ProductResponse get(@PathVariable Long productId) {
        return service.get(productId);
    }

    // Update product: Procurement + Manager
    @PatchMapping("/{productId}")
    //TODO: ADMIN preauthorized to delete for dev testing. Remove ADMIN for production.
    @PreAuthorize("hasAnyRole('PROCUREMENT_OFFICER','MANAGER', 'ADMIN')")
    public ProductResponse update(
            @PathVariable Long productId,
            @Valid @RequestBody ProductUpdateRequest req
    ) {
        return service.update(productId, req);
    }

    // Delete product: Procurement only (more sensitive)
    @DeleteMapping("/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    //TODO: ADMIN preauthorized to delete for dev testing. Remove ADMIN for production.
    @PreAuthorize("hasAnyRole('PROCUREMENT_OFFICER', 'ADMIN')")
    public void delete(@PathVariable Long productId) {
        service.delete(productId);
    }
}
