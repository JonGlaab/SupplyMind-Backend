package com.supplymind.platform_core.controller.core;

import com.supplymind.platform_core.common.util.PaginationDefaults;
import com.supplymind.platform_core.dto.core.inventory.InventoryResponse;
import com.supplymind.platform_core.dto.core.inventory.InventoryTransactionRequest;
import com.supplymind.platform_core.dto.core.inventory.InventoryTransactionResponse;
import com.supplymind.platform_core.service.core.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/core/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService service;

    private Pageable capPageSize(Pageable pageable) {
        int size = Math.min(pageable.getPageSize(), PaginationDefaults.MAX_PAGE_SIZE);
        return PageRequest.of(pageable.getPageNumber(), size, pageable.getSort());
    }


    // GET /api/core/inventory?warehouseId=1
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','PROCUREMENT_OFFICER','STAFF')")
    public Page<InventoryResponse> listByWarehouse(
            @RequestParam Long warehouseId,
            @PageableDefault(size = PaginationDefaults.DEFAULT_PAGE_SIZE, sort = PaginationDefaults.DEFAULT_SORT) Pageable pageable
    ) {
        return service.listByWarehouse(warehouseId, capPageSize(pageable));
    }

    // GET /api/core/inventory/low-stock?warehouseId=1&supplierId=2
    @GetMapping("/low-stock")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','PROCUREMENT_OFFICER')")
    public Page<InventoryResponse> findLowStock(
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long supplierId,
            @PageableDefault(size = PaginationDefaults.DEFAULT_PAGE_SIZE, sort = PaginationDefaults.DEFAULT_SORT) Pageable pageable
    ) {
        return service.findLowStock(warehouseId, supplierId, capPageSize(pageable));
    }

    // POST /api/core/inventory/transactions
    @PostMapping("/transactions")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
    public void recordTransaction(@Valid @RequestBody InventoryTransactionRequest req) {
        service.recordTransaction(req);
    }

    @GetMapping("/transactions")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','PROCUREMENT_OFFICER','STAFF')")
    public Page<InventoryTransactionResponse> listTransactions(
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long productId,
            @PageableDefault(size = PaginationDefaults.DEFAULT_PAGE_SIZE, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return service.listTransactions(warehouseId, productId, capPageSize(pageable));
    }

}
