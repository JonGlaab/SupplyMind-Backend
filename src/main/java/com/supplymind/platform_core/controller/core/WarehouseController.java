package com.supplymind.platform_core.controller.core;

import com.supplymind.platform_core.common.util.PaginationDefaults;
import com.supplymind.platform_core.dto.core.warehouse.*;
import com.supplymind.platform_core.service.core.WarehouseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/core/warehouses")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseService service;

    // POST /api/core/warehouses
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public WarehouseResponse create(@Valid @RequestBody WarehouseCreateRequest req) {
        return service.create(req);
    }

    // GET /api/core/warehouses
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','PROCUREMENT_OFFICER','STAFF')")
    public Page<WarehouseResponse> list(
            @PageableDefault(size = PaginationDefaults.DEFAULT_PAGE_SIZE, sort = "warehouseId") Pageable pageable
    ) {
        return service.list(pageable);
    }

    // GET /api/core/warehouses/{warehouseId}
    @GetMapping("/{warehouseId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','PROCUREMENT_OFFICER','STAFF')")
    public WarehouseResponse get(@PathVariable Long warehouseId) {
        return service.get(warehouseId);
    }

    // PATCH /api/core/warehouses/{warehouseId}
    @PatchMapping("/{warehouseId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public WarehouseResponse update(@PathVariable Long warehouseId,
                                    @Valid @RequestBody WarehouseUpdateRequest req) {
        return service.update(warehouseId, req);
    }
}
