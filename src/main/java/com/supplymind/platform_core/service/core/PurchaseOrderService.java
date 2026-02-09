package com.supplymind.platform_core.service.core;

import com.supplymind.platform_core.common.enums.PurchaseOrderStatus;
import com.supplymind.platform_core.dto.core.purchaseorder.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PurchaseOrderService {

    PurchaseOrderResponse createDraft(PurchaseOrderCreateRequest req);

    Page<PurchaseOrderResponse> list(PurchaseOrderStatus status, Long supplierId, Long warehouseId, Pageable pageable);

    PurchaseOrderResponse get(Long poId);

    void delete(Long poId);

    PurchaseOrderResponse updateHeader(Long poId, PurchaseOrderUpdateRequest req);

    PurchaseOrderItemResponse addItem(Long poId, PurchaseOrderItemCreateRequest req);

    PurchaseOrderItemResponse updateItem(Long poId, Long itemId, PurchaseOrderItemUpdateRequest req);

    void removeItem(Long poId, Long itemId);

    PurchaseOrderResponse submit(Long poId);

    ApprovalResponse approve(Long poId);

    PurchaseOrderResponse cancel(Long poId);

    PurchaseOrderResponse receive(Long poId, ReceivePurchaseOrderRequest req);

    ReceivingStatusResponse receivingStatus(Long poId);

    PurchaseOrderResponse updateStatus(Long poId, PurchaseOrderStatusUpdateRequest req);

}
