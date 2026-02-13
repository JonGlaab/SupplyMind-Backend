package com.supplymind.platform_core.service.mobile;

import com.supplymind.platform_core.common.enums.ScanType;
import com.supplymind.platform_core.dto.mobile.ScanAnalysisResponse;
import com.supplymind.platform_core.model.core.Product;
import com.supplymind.platform_core.model.core.PurchaseOrder;
import com.supplymind.platform_core.repository.core.ProductRepository;
import com.supplymind.platform_core.repository.core.PurchaseOrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class MobileScanService {

    @Autowired
    private PurchaseOrderRepository poRepo;

    @Autowired
    private ProductRepository productRepo;

    public ScanAnalysisResponse analyzeCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return ScanAnalysisResponse.builder()
                    .scanType(ScanType.UNKNOWN)
                    .message("Empty scan result")
                    .build();
        }

        String cleanCode = code.trim();

        // 1. Check for Purchase Order Match
        PurchaseOrder foundPo = findPurchaseOrder(cleanCode);

        // 2. Check for Product Match (SKU)
        Optional<Product> productOpt = productRepo.findBySku(cleanCode);

        // 3. Apply Decision Matrix
        if (foundPo != null && productOpt.isPresent()) {
            return buildResponse(ScanType.AMBIGUOUS, foundPo, productOpt.get());
        } else if (foundPo != null) {
            return buildResponse(ScanType.PO, foundPo, null);
        } else if (productOpt.isPresent()) {
            return buildResponse(ScanType.PRODUCT, null, productOpt.get());
        }

        return ScanAnalysisResponse.builder()
                .scanType(ScanType.UNKNOWN)
                .message("No matching PO or Product found.")
                .build();
    }

    private PurchaseOrder findPurchaseOrder(String code) {
        try {
            // Support both "PO-100" and "100" formats
            String numericId = code.toUpperCase().startsWith("PO-") ? code.substring(3) : code;
            if (numericId.matches("\\d+")) {
                // Using findById from JpaRepository
                return poRepo.findById(Long.parseLong(numericId)).orElse(null);
            }
        } catch (Exception ignored) {}
        return null;
    }

    private ScanAnalysisResponse buildResponse(ScanType type, PurchaseOrder po, Product prod) {
        var response = ScanAnalysisResponse.builder().scanType(type);

        if (po != null) {
            response.poId(po.getPoId());
            response.supplierName(po.getSupplier() != null ? po.getSupplier().getName() : "Unknown Supplier");
            response.totalItems(po.getPurchaseOrderItems() != null ? po.getPurchaseOrderItems().size() : 0);
        }

        if (prod != null) {
            response.productId(prod.getProductId());
            response.productSku(prod.getSku());
            response.productName(prod.getName());
            response.category(prod.getCategory());
        }

        return response.build();
    }
}