package com.supplymind.platform_core.controller.core;

import com.supplymind.platform_core.common.enums.PurchaseOrderStatus;
import com.supplymind.platform_core.common.util.PaginationDefaults;
import com.supplymind.platform_core.dto.core.purchaseorder.*;
import com.supplymind.platform_core.service.core.PurchaseOrderService;


import com.supplymind.platform_core.model.auth.User;
import com.supplymind.platform_core.model.core.PurchaseOrder;
import com.supplymind.platform_core.repository.auth.UserRepository;
import com.supplymind.platform_core.repository.core.PurchaseOrderRepository;
import com.supplymind.platform_core.service.common.PdfGenerationService;
import com.supplymind.platform_core.service.communication.EmailProvider;
import com.supplymind.platform_core.service.intel.AiContentService;
import org.springframework.core.io.InputStreamResource;
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
import java.io.FileInputStream;
import java.security.Principal;


@RestController
@RequestMapping("/api/core/purchase-orders")
@RequiredArgsConstructor
public class PurchaseOrderController {

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

    // POST /api/core/purchase-orders/{poId}/status
    @PostMapping("/{poId}/status")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','PROCUREMENT_OFFICER')")
    public PurchaseOrderResponse updateStatus(
            @PathVariable Long poId,
            @Valid @RequestBody PurchaseOrderStatusUpdateRequest req
    ) {
        return service.updateStatus(poId, req);
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
    //Email
    /**
     * Frontend calls this to show the PDF Preview on the Right Side.
     */
    @GetMapping(value = "/{poId}/preview-pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<Resource> previewPurchaseOrderPdf(
            @PathVariable Long poId,
            @RequestParam(defaultValue = "false") boolean signed) { 
        try {
            PurchaseOrder po = purchaseOrderRepository.findById(poId)
                    .orElseThrow(() -> new RuntimeException("Purchase Order not found"));

            // Pass 'signed' flag to service
            File pdfFile = pdfGenerationService.generatePurchaseOrderPdf(po, signed);

            InputStreamResource resource = new InputStreamResource(new FileInputStream(pdfFile));

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=PO-" + po.getPoId() + ".pdf")
                    .contentLength(pdfFile.length())
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(resource);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Frontend calls this to populate the "Left Side" Email Editor (AI Draft).
     */
    @GetMapping("/{poId}/email-draft")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','PROCUREMENT_OFFICER')")
    @Transactional(readOnly = true)
    public ResponseEntity<EmailDraftResponse> getEmailDraft(@PathVariable Long poId, Principal principal) {
        try {
            PurchaseOrder po = purchaseOrderRepository.findById(poId)
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
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Manager clicks "Confirm & Send".
     * Accepts the FINAL body (edited by manager) and sends it.
     */
    @PostMapping("/{poId}/send-email")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','PROCUREMENT_OFFICER')")
    @Transactional
    public ResponseEntity<String> sendPurchaseOrder(
            @PathVariable Long poId,
            @RequestBody SendPurchaseOrderEmailRequest request) {
        try {
            PurchaseOrder po = purchaseOrderRepository.findById(poId)
                    .orElseThrow(() -> new RuntimeException("Purchase Order not found"));

            if (po.getSupplier() == null || po.getSupplier().getContactEmail() == null) {
                return ResponseEntity.badRequest().body("Supplier email is missing.");
            }

            // Generate FINAL PDF (Signed if request.isAddSignature() is true)
            File pdfAttachment = pdfGenerationService.generatePurchaseOrderPdf(po, request.isAddSignature());

            emailProvider.sendEmail(
                    po.getSupplier().getContactEmail(),
                    request.getSubject(),
                    request.getBody(),
                    pdfAttachment
            );

            // Create Inbox Label
            inboxService.createInboxForPo(poId);

            po.setStatus(PurchaseOrderStatus.EMAIL_SENT);
            po.setLastActivityAt(java.time.Instant.now());
            purchaseOrderRepository.save(po);

            return ResponseEntity.ok("Sent PO-" + po.getPoId());

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Failed: " + e.getMessage());
        }
    }
    @GetMapping("/{poId}/preview-pdf")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','PROCUREMENT_OFFICER')")
    public ResponseEntity<Resource> previewPdf(
            @PathVariable Long poId,
            @RequestParam(defaultValue = "false") boolean signed) {
        try {
            PurchaseOrder po = purchaseOrderRepository.findById(poId)
                    .orElseThrow(() -> new RuntimeException("PO not found"));

            File pdfFile = pdfGenerationService.generatePurchaseOrderPdf(po, signed);
            InputStreamResource resource = new InputStreamResource(new java.io.FileInputStream(pdfFile));

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=PO-" + poId + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}

