package com.supplymind.platform_core.controller.core;

import com.supplymind.platform_core.common.enums.PurchaseOrderStatus;
import com.supplymind.platform_core.dto.core.purchaseorder.*;
import com.supplymind.platform_core.service.core.PurchaseOrderService;


import com.supplymind.platform_core.model.auth.User;
import com.supplymind.platform_core.model.core.PurchaseOrder;
import com.supplymind.platform_core.repository.auth.UserRepository;
import com.supplymind.platform_core.repository.core.PurchaseOrderRepository;
import com.supplymind.platform_core.service.common.PdfGenerationService;
import com.supplymind.platform_core.service.communication.EmailProvider;
import com.supplymind.platform_core.service.intel.AiContentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import com.supplymind.platform_core.service.communication.InboxService;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.security.Principal;

/**
 * REST controller for managing Purchase Orders.
 * Provides endpoints for creating, retrieving, updating, and managing the lifecycle of POs.
 * Access is restricted based on user roles.
 */
@RestController
@RequestMapping("/api/core/purchase-orders")
@RequiredArgsConstructor
public class PurchaseOrderController {

    private static final Logger logger = LoggerFactory.getLogger(PurchaseOrderController.class);

    private final PurchaseOrderService service;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final UserRepository userRepository;
    private final EmailProvider emailProvider;
    private final AiContentService aiContentService;
    private final PdfGenerationService pdfGenerationService;
    private final InboxService inboxService;

    // POST /api/core/purchase-orders - create draft
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT_OFFICER')")
    public PurchaseOrderResponse createDraft(@Valid @RequestBody PurchaseOrderCreateRequest req) {
        return service.createDraft(req);
    }

    /**
     * Lists all Purchase Orders with optional filters and pagination.
     * @param status Optional filter by PO status.
     * @param supplierId Optional filter by supplier ID.
     * @param warehouseId Optional filter by warehouse ID.
     * @param pageable Pagination information.
     * @return A paginated list of Purchase Orders.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','PROCUREMENT_OFFICER')")
    public Page<PurchaseOrderResponse> list(
            @RequestParam(required = false) PurchaseOrderStatus status,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) Long warehouseId,
            @PageableDefault(size = 30, sort = "poId", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return service.list(status, supplierId, warehouseId, pageable);
    }

    /**
     * Retrieves a single Purchase Order by its ID.
     * @param poId The ID of the Purchase Order.
     * @return The requested Purchase Order.
     */
    @GetMapping("/{poId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','PROCUREMENT_OFFICER')")
    public PurchaseOrderResponse get(@PathVariable Long poId) {
        return service.get(poId);
    }

    /**
     * Deletes a draft Purchase Order.
     * @param poId The ID of the Purchase Order to delete.
     */
    @DeleteMapping("/{poId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT_OFFICER')")
    public void delete(@PathVariable Long poId) {
        service.delete(poId);
    }

    /**
     * Updates the header information of a draft Purchase Order.
     * @param poId The ID of the Purchase Order.
     * @param req The request body with updated data.
     * @return The updated Purchase Order.
     */
    @PatchMapping("/{poId}")
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT_OFFICER')")
    public PurchaseOrderResponse updateHeader(@PathVariable Long poId,
                                              @Valid @RequestBody PurchaseOrderUpdateRequest req) {
        return service.updateHeader(poId, req);
    }

    /**
     * Adds an item to a draft Purchase Order.
     * @param poId The ID of the Purchase Order.
     * @param req The request body containing the new item's details.
     * @return The newly added Purchase Order item.
     */
    @PostMapping("/{poId}/items")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT_OFFICER')")
    public PurchaseOrderItemResponse addItem(@PathVariable Long poId,
                                             @Valid @RequestBody PurchaseOrderItemCreateRequest req) {
        return service.addItem(poId, req);
    }

    /**
     * Updates an item on a draft Purchase Order.
     * @param poId The ID of the Purchase Order.
     * @param itemId The ID of the item to update.
     * @param req The request body with updated item data.
     * @return The updated Purchase Order item.
     */
    @PatchMapping("/{poId}/items/{itemId}")
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT_OFFICER')")
    public PurchaseOrderItemResponse updateItem(@PathVariable Long poId,
                                                @PathVariable Long itemId,
                                                @Valid @RequestBody PurchaseOrderItemUpdateRequest req) {
        return service.updateItem(poId, itemId, req);
    }

    /**
     * Removes an item from a draft Purchase Order.
     * @param poId The ID of the Purchase Order.
     * @param itemId The ID of the item to remove.
     */
    @DeleteMapping("/{poId}/items/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT_OFFICER')")
    public void removeItem(@PathVariable Long poId, @PathVariable Long itemId) {
        service.removeItem(poId, itemId);
    }

    /**
     * Submits a draft Purchase Order for approval.
     * @param poId The ID of the Purchase Order to submit.
     * @return The submitted Purchase Order with status PENDING_APPROVAL.
     */
    @PostMapping("/{poId}/submit")
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT_OFFICER')")
    public PurchaseOrderResponse submit(@PathVariable Long poId) {
        return service.submit(poId);
    }

    /**
     * Approves a Purchase Order that is pending approval.
     * @param poId The ID of the Purchase Order to approve.
     * @return The approved Purchase Order with status APPROVED.
     */
    @PostMapping("/{poId}/approve")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public PurchaseOrderResponse approve(@PathVariable Long poId) { return service.approve(poId); }

    /**
     * Cancels a Purchase Order.
     * @param poId The ID of the Purchase Order to cancel.
     * @return The cancelled Purchase Order with status CANCELLED.
     */
    @PostMapping("/{poId}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT_OFFICER')")
    public PurchaseOrderResponse cancel(@PathVariable Long poId) {
        return service.cancel(poId);
    }

    /**
     * Manually updates the status of a Purchase Order.
     * @param poId The ID of the Purchase Order.
     * @param req The request body containing the new status.
     * @return The updated Purchase Order.
     */
    @PostMapping("/{poId}/status")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','PROCUREMENT_OFFICER')")
    public PurchaseOrderResponse updateStatus(
            @PathVariable Long poId,
            @Valid @RequestBody PurchaseOrderStatusUpdateRequest req
    ) {
        return service.updateStatus(poId, req);
    }

    /**
     * Records the receipt of goods for a Purchase Order.
     * @param poId The ID of the Purchase Order.
     * @param req The request body containing details of the received items.
     * @return The updated Purchase Order, likely with status COMPLETED.
     */
    @PostMapping("/{poId}/receive")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
    public PurchaseOrderResponse receive(@PathVariable Long poId,
                                         @Valid @RequestBody ReceivePurchaseOrderRequest req) {
        return service.receive(poId, req);
    }

    /**
     * Retrieves the receiving status of a Purchase Order, detailing ordered vs. received quantities.
     * @param poId The ID of the Purchase Order.
     * @return The receiving status details.
     */
    @GetMapping("/{poId}/receiving-status")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','PROCUREMENT_OFFICER')")
    public ReceivingStatusResponse receivingStatus(@PathVariable Long poId) {
        return service.receivingStatus(poId);
    }

    /**
     * Generates and returns a PDF preview of the Purchase Order.
     * @param poId The ID of the Purchase Order.
     * @param includeSignature Whether to include the approver's signature in the PDF.
     * @return A response entity containing the PDF file.
     */
    @GetMapping("/{poId}/preview-pdf")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','PROCUREMENT_OFFICER')")
    @Transactional(readOnly = true)
    public ResponseEntity<Resource> previewPurchaseOrderPdf(
            @PathVariable Long poId,
            @RequestParam(defaultValue = "false") boolean includeSignature
    ) {
        try {
            PurchaseOrder po = purchaseOrderRepository.findById(poId)
                    .orElseThrow(() -> new RuntimeException("Purchase Order not found: " + poId));

            boolean isSigned = includeSignature && po.getApprover() != null;
            File pdfFile = pdfGenerationService.generatePurchaseOrderPdf(po, po.getApprover(), isSigned);
            FileSystemResource resource = new FileSystemResource(pdfFile);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=" + pdfFile.getName())
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(pdfFile.length())
                    .body(resource);
        } catch (Exception e) {
            logger.error("Error generating PDF preview for PO: {}", poId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Generates an AI-drafted email body for sending a Purchase Order to a supplier.
     * @param poId The ID of the Purchase Order.
     * @param principal The current authenticated user.
     * @return A response entity containing the email subject, body, and recipient.
     */
    @GetMapping("/{poId}/email-draft")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','PROCUREMENT_OFFICER')")
    @Transactional(readOnly = true)
    public ResponseEntity<EmailDraftResponse> getEmailDraft(@PathVariable Long poId, Principal principal) {
        try {
            PurchaseOrder po = purchaseOrderRepository.findByIdWithItems(poId)
                    .orElseThrow(() -> new RuntimeException("Purchase Order not found: " + poId));

            // Identify Manager for the AI Signature
            String managerName = "SupplyMind Manager";
            if (principal != null) {
                User user = userRepository.findByEmail(principal.getName()).orElse(null);
                if (user != null) managerName = user.getFirstName() + " " + user.getLastName();
            }

            if (po.getSupplier() == null) return ResponseEntity.badRequest().build();

            // Ask AI for the draft
            String body = aiContentService.generatePurchaseOrderEmail(po, managerName, po.getSupplier().getName());
            String subject = "Official Purchase Order PO-" + po.getPoId();

            return ResponseEntity.ok(new EmailDraftResponse(subject, body, po.getSupplier().getContactEmail()));

        } catch (Exception e) {
            logger.error("Error generating email draft for PO: {}", poId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Sends the Purchase Order email to the supplier.
     * @param poId The ID of the Purchase Order.
     * @param request The request body containing the final email content and settings.
     * @return A confirmation message.
     */
    @PostMapping("/{poId}/send")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','PROCUREMENT_OFFICER')")
    @Transactional
    public ResponseEntity<String> sendPurchaseOrder(
            @PathVariable Long poId,
            @RequestBody SendPurchaseOrderEmailRequest request) {
        try {
            PurchaseOrder po = purchaseOrderRepository.findById(poId)
                    .orElseThrow(() -> new RuntimeException("Purchase Order not found"));

            // Eagerly fetch required data
            po.getSupplier().getName();
            if (po.getApprover() != null) {
                po.getApprover().getFirstName();
            }

            String recipient = (request.getToEmail() != null && !request.getToEmail().isBlank())
                    ? request.getToEmail()
                    : po.getSupplier().getContactEmail();

            if (recipient == null) {
                return ResponseEntity.badRequest().body("Supplier email is missing.");
            }

            File pdfAttachment = pdfGenerationService.generatePurchaseOrderPdf(po, po.getApprover(), request.isAddSignature());

            emailProvider.sendEmail(
                    recipient,
                    request.getSubject(),
                    request.getBody(),
                    pdfAttachment
            );

            inboxService.createInboxForPo(poId);

            po.setStatus(PurchaseOrderStatus.EMAIL_SENT);
            po.setLastActivityAt(java.time.Instant.now());
            purchaseOrderRepository.save(po);

            return ResponseEntity.ok("Sent PO-" + po.getPoId());

        } catch (Exception e) {
            logger.error("Error sending email for PO: {}", poId, e);
            return ResponseEntity.internalServerError().body("Failed: " + e.getMessage());
        }
    }
}
