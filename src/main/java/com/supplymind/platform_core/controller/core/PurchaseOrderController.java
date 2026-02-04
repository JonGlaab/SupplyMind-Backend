package com.supplymind.platform_core.controller.core;

import com.supplymind.platform_core.common.enums.PurchaseOrderStatus;
import com.supplymind.platform_core.common.util.PaginationDefaults;
import com.supplymind.platform_core.dto.core.purchaseorder.*;
import com.supplymind.platform_core.service.core.PurchaseOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/core/purchase-orders")
@RequiredArgsConstructor
public class PurchaseOrderController {

    private final PurchaseOrderService service;

    // POST /api/core/purchase-orders - create draft
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT_OFFICER')")
    public PurchaseOrderResponse createDraft(@Valid @RequestBody PurchaseOrderCreateRequest req) {
        return service.createDraft(req);
    }

    // GET /api/core/purchase-orders?status=&supplierId=&warehouseId=
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','PROCUREMENT_OFFICER')")
    public Page<PurchaseOrderResponse> list(
            @RequestParam(required = false) PurchaseOrderStatus status,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) Long warehouseId,
            @PageableDefault(size = PaginationDefaults.DEFAULT_PAGE_SIZE, sort = "poId", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return service.list(status, supplierId, warehouseId, capPageSize(pageable));
    }

    // GET /api/core/purchase-orders/{poId}
    @GetMapping("/{poId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','PROCUREMENT_OFFICER')")
    public PurchaseOrderResponse get(@PathVariable Long poId) {
        return service.get(poId);
    }

    // PATCH /api/core/purchase-orders/{poId} - edit header
    @PatchMapping("/{poId}")
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT_OFFICER')")
    public PurchaseOrderResponse updateHeader(@PathVariable Long poId,
                                              @Valid @RequestBody PurchaseOrderUpdateRequest req) {
        return service.updateHeader(poId, req);
    }

    // POST /api/core/purchase-orders/{poId}/items
    @PostMapping("/{poId}/items")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT_OFFICER')")
    public PurchaseOrderItemResponse addItem(@PathVariable Long poId,
                                             @Valid @RequestBody PurchaseOrderItemCreateRequest req) {
        return service.addItem(poId, req);
    }

    // PATCH /api/core/purchase-orders/{poId}/items/{itemId}
    @PatchMapping("/{poId}/items/{itemId}")
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT_OFFICER')")
    public PurchaseOrderItemResponse updateItem(@PathVariable Long poId,
                                                @PathVariable Long itemId,
                                                @Valid @RequestBody PurchaseOrderItemUpdateRequest req) {
        return service.updateItem(poId, itemId, req);
    }

    // DELETE /api/core/purchase-orders/{poId}/items/{itemId}
    @DeleteMapping("/{poId}/items/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT_OFFICER')")
    public void removeItem(@PathVariable Long poId, @PathVariable Long itemId) {
        service.removeItem(poId, itemId);
    }

    // POST /api/core/purchase-orders/{poId}/submit
    @PostMapping("/{poId}/submit")
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT_OFFICER')")
    public PurchaseOrderResponse submit(@PathVariable Long poId) {
        return service.submit(poId);
    }

    // POST /api/core/purchase-orders/{poId}/approve (optional)
    @PostMapping("/{poId}/approve")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public PurchaseOrderResponse approve(@PathVariable Long poId) {
        return service.approve(poId);
    }

    // POST /api/core/purchase-orders/{poId}/cancel
    @PostMapping("/{poId}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT_OFFICER')")
    public PurchaseOrderResponse cancel(@PathVariable Long poId) {
        return service.cancel(poId);
    }

    // POST /api/core/purchase-orders/{poId}/receive
    @PostMapping("/{poId}/receive")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
    public PurchaseOrderResponse receive(@PathVariable Long poId,
                                         @Valid @RequestBody ReceivePurchaseOrderRequest req) {
        return service.receive(poId, req);
    }

    // GET /api/core/purchase-orders/{poId}/receiving-status
    @GetMapping("/{poId}/receiving-status")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','PROCUREMENT_OFFICER')")
    public ReceivingStatusResponse receivingStatus(@PathVariable Long poId) {
        return service.receivingStatus(poId);
    }

    private Pageable capPageSize(Pageable pageable) {
        int size = Math.min(pageable.getPageSize(), PaginationDefaults.MAX_PAGE_SIZE);
        return PageRequest.of(pageable.getPageNumber(), size, pageable.getSort());
    }
}

