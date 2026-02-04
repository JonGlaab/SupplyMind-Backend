package com.supplymind.platform_core.service.common;

import com.supplymind.platform_core.model.core.PurchaseOrder;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

@Service
public class PdfGenerationService {

    public File generatePurchaseOrderPdf(PurchaseOrder po) throws IOException {

        File tempFile = File.createTempFile("PO-" + po.getPoId() + "-", ".pdf");

        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("%PDF-1.4 (Simulated)\n");
            writer.write("OFFICIAL PURCHASE ORDER\n");
            writer.write("============================================================\n");
            writer.write("PO NUMBER: " + po.getPoId() + "\n");
            writer.write("STATUS:    " + po.getStatus() + "\n");
            writer.write("SUPPLIER:  " + po.getSupplier().getName() + "\n");
            writer.write("DATE:      " + DateTimeFormatter.ISO_INSTANT.format(po.getCreatedOn()) + "\n");
            writer.write("============================================================\n\n");
            writer.write("ITEMS:\n");

            writer.write(String.format("%-25s | %-5s | %-10s | %-10s\n", "PRODUCT", "QTY", "UNIT COST", "TOTAL"));
            writer.write("------------------------------------------------------------\n");

            po.getPurchaseOrderItems().forEach(item -> {
                try {
                    BigDecimal lineTotal = item.getUnitCost().multiply(BigDecimal.valueOf(item.getOrderedQty()));
                    writer.write(String.format("%-25s | %-5d | $%-9s | $%s\n",
                            item.getProduct().getName(),
                            item.getOrderedQty(),
                            item.getUnitCost(),
                            lineTotal));

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            writer.write("------------------------------------------------------------\n");
            writer.write("GRAND TOTAL: $" + po.getTotalAmount() + "\n\n");

            String buyerName = (po.getBuyer() != null) ? po.getBuyer().getFirstName() : "Unknown";
            writer.write("[DIGITALLY SIGNED BY: " + buyerName + "]\n");
            writer.write("[VERIFIED BY SUPPLYMIND PLATFORM]");
        }

        return tempFile;
    }
}