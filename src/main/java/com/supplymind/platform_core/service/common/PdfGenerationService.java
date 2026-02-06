package com.supplymind.platform_core.service.common;

import com.supplymind.platform_core.model.core.PurchaseOrder;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class PdfGenerationService {

    // 👇 UPDATED: Now accepts 'boolean signed'
    public File generatePurchaseOrderPdf(PurchaseOrder po, boolean signed) throws IOException {

        File tempFile = File.createTempFile("PO-" + po.getPoId() + "-", ".pdf");

        try (FileWriter writer = new FileWriter(tempFile)) {
            // --- HEADER ---
            writer.write("%PDF-1.4 (Simulated)\n");
            writer.write("OFFICIAL PURCHASE ORDER\n");
            writer.write("============================================================\n");
            writer.write("PO NUMBER: " + po.getPoId() + "\n");
            writer.write("STATUS:    " + po.getStatus() + "\n");
            writer.write("SUPPLIER:  " + (po.getSupplier() != null ? po.getSupplier().getName() : "Unknown") + "\n");
            writer.write("DATE:      " + DateTimeFormatter.ISO_INSTANT.format(po.getCreatedOn()) + "\n");
            writer.write("============================================================\n\n");

            // --- ITEMS TABLE ---
            writer.write("ITEMS:\n");
            writer.write(String.format("%-25s | %-5s | %-10s | %-10s\n", "PRODUCT", "QTY", "UNIT COST", "TOTAL"));
            writer.write("------------------------------------------------------------\n");

            if (po.getPurchaseOrderItems() != null) {
                for (var item : po.getPurchaseOrderItems()) {
                    BigDecimal lineTotal = item.getUnitCost().multiply(BigDecimal.valueOf(item.getOrderedQty()));
                    writer.write(String.format("%-25s | %-5d | $%-9s | $%s\n",
                            item.getProduct().getName(),
                            item.getOrderedQty(),
                            item.getUnitCost(),
                            lineTotal));
                }
            }
            writer.write("------------------------------------------------------------\n");
            writer.write("GRAND TOTAL: $" + po.getTotalAmount() + "\n\n");

            // --- SIGNATURE SECTION (The Upgrade) ---
            writer.write("\n\n");
            writer.write("APPROVAL & CERTIFICATION\n");
            writer.write("------------------------------------------------------------\n");

            String managerName = (po.getBuyer() != null)
                    ? po.getBuyer().getFirstName() + " " + po.getBuyer().getLastName()
                    : "Authorized Manager";

            if (signed) {
                writer.write("\n");
                writer.write("      /Signed/ " + managerName + "\n");
                writer.write("      __________________________________________\n");
                writer.write("      AUTHORIZED SIGNATURE\n\n");
                writer.write("      Name:  " + managerName + "\n");
                writer.write("      Date:  " + LocalDate.now() + "\n");
                writer.write("      [Digitally Certified by SupplyMind Platform]\n");
            } else {
                // ⬜ Blank Line (For preview or manual signing)
                writer.write("\n\n");
                writer.write("      __________________________________________\n");
                writer.write("      AUTHORIZED SIGNATURE\n\n");
                writer.write("      Name:  ___________________________________\n");
                writer.write("      Date:  ___________________________________\n");
            }

            writer.write("\n============================================================\n");
            writer.write("SupplyMind Inc. | 123 Tech Blvd, Innovation City\n");
        }

        return tempFile;
    }
}