package com.supplymind.platform_core.service.common;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.supplymind.platform_core.model.auth.User;
import com.supplymind.platform_core.model.core.PurchaseOrder;
import com.supplymind.platform_core.model.core.PurchaseOrderItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
@Slf4j
@RequiredArgsConstructor
public class PdfGenerationService {

    private final StorageService storageService;

    private static final Font FONT_TITLE = new Font(Font.HELVETICA, 18, Font.BOLD);
    private static final Font FONT_HEADER = new Font(Font.HELVETICA, 12, Font.BOLD);
    private static final Font FONT_NORMAL = new Font(Font.HELVETICA, 10, Font.NORMAL);
    private static final Font FONT_SMALL = new Font(Font.HELVETICA, 8, Font.ITALIC, Color.GRAY);

    public File generatePurchaseOrderPdf(PurchaseOrder po, User approver, boolean signed) throws IOException {
        File tempFile = File.createTempFile("PO-" + po.getPoId() + "-", ".pdf");

        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, fos);
            document.open();

            addHeader(document, po);
            addMetaInfo(document, po);
            addItemsTable(document, po);
            addTotals(document, po);
            addSignature(document, po, approver, signed);
            addFooter(document);

            document.close();
        } catch (DocumentException e) {
            log.error("Error generating PDF", e);
            throw new IOException("PDF Generation failed", e);
        }

        return tempFile;
    }

    private void addHeader(Document doc, PurchaseOrder po) throws DocumentException {
        Paragraph title = new Paragraph("PURCHASE ORDER", FONT_TITLE);
        title.setAlignment(Element.ALIGN_RIGHT);
        doc.add(title);

        String dateStr = DateTimeFormatter.ISO_LOCAL_DATE.format(
                po.getCreatedOn().atZone(ZoneId.systemDefault())
        );

        Paragraph sub = new Paragraph("PO #: " + po.getPoId() + "\nDate: " + dateStr, FONT_NORMAL);
        sub.setAlignment(Element.ALIGN_RIGHT);
        doc.add(sub);
        doc.add(new Paragraph("\n"));
    }

    private void addMetaInfo(Document doc, PurchaseOrder po) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);

        // FROM
        PdfPCell buyerCell = new PdfPCell();
        buyerCell.setBorder(Rectangle.NO_BORDER);
        buyerCell.addElement(new Paragraph("FROM:", FONT_HEADER));
        buyerCell.addElement(new Paragraph("SupplyMind Inc.", FONT_NORMAL));
        buyerCell.addElement(new Paragraph("123 Tech Blvd, Innovation City", FONT_NORMAL));
        buyerCell.addElement(new Paragraph("procurement@supplymind.com", FONT_NORMAL));

        // TO
        PdfPCell supplierCell = new PdfPCell();
        supplierCell.setBorder(Rectangle.NO_BORDER);
        supplierCell.addElement(new Paragraph("TO:", FONT_HEADER));

        String supplierName = (po.getSupplier() != null) ? po.getSupplier().getName() : "Unknown Supplier";

        // ✅ Corrected: uses getContactEmail() based on your Supplier.java
        String supplierEmail = (po.getSupplier() != null && po.getSupplier().getContactEmail() != null)
                ? po.getSupplier().getContactEmail()
                : "";

        supplierCell.addElement(new Paragraph(supplierName, FONT_NORMAL));
        supplierCell.addElement(new Paragraph(supplierEmail, FONT_NORMAL));

        table.addCell(buyerCell);
        table.addCell(supplierCell);
        doc.add(table);
        doc.add(new Paragraph("\n"));
    }

    private void addItemsTable(Document doc, PurchaseOrder po) throws DocumentException {
        PdfPTable table = new PdfPTable(new float[]{1, 4, 1, 2, 2});
        table.setWidthPercentage(100);
        table.setSpacingBefore(10f);

        String[] headers = {"#", "Description", "Qty", "Unit Price", "Total"};
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Paragraph(h, FONT_HEADER));
            cell.setBackgroundColor(Color.LIGHT_GRAY);
            cell.setPadding(5);
            table.addCell(cell);
        }

        int index = 1;
        NumberFormat currency = NumberFormat.getCurrencyInstance(Locale.US);

        if (po.getPurchaseOrderItems() != null) {
            for (PurchaseOrderItem item : po.getPurchaseOrderItems()) {
                table.addCell(new Paragraph(String.valueOf(index++), FONT_NORMAL));

                String productName = (item.getProduct() != null) ? item.getProduct().getName() : "Item";
                table.addCell(new Paragraph(productName, FONT_NORMAL));

                int qty = item.getOrderedQty() != null ? item.getOrderedQty() : 0;
                table.addCell(new Paragraph(String.valueOf(qty), FONT_NORMAL));

                BigDecimal cost = item.getUnitCost() != null ? item.getUnitCost() : BigDecimal.ZERO;
                table.addCell(new Paragraph(currency.format(cost), FONT_NORMAL));

                BigDecimal lineTotal = cost.multiply(BigDecimal.valueOf(qty));
                table.addCell(new Paragraph(currency.format(lineTotal), FONT_NORMAL));
            }
        }

        doc.add(table);
    }

    private void addTotals(Document doc, PurchaseOrder po) throws DocumentException {
        BigDecimal total = po.getTotalAmount() != null ? po.getTotalAmount() : BigDecimal.ZERO;

        Paragraph p = new Paragraph("\nTotal Amount: " +
                NumberFormat.getCurrencyInstance(Locale.US).format(total), FONT_TITLE);
        p.setAlignment(Element.ALIGN_RIGHT);
        doc.add(p);
    }

    private void addSignature(Document doc, PurchaseOrder po, User approver, boolean signed) throws DocumentException {
        doc.add(new Paragraph("\n\n\n"));

        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(40);
        table.setHorizontalAlignment(Element.ALIGN_LEFT);

        // Cell for the signature image
        PdfPCell imageCell = new PdfPCell();
        imageCell.setBorder(Rectangle.NO_BORDER);
        imageCell.setFixedHeight(40f); // Space for the signature

        if (signed) {
            User user = (approver != null) ? approver : po.getBuyer();
            if (user != null && user.getSignatureUrl() != null && !user.getSignatureUrl().isEmpty()) {
                try {
                    String presignedUrl = storageService.presignGetUrl(user.getSignatureUrl());
                    Image signatureImg = Image.getInstance(URI.create(presignedUrl).toURL());
                    signatureImg.scaleToFit(120, 50);
                    imageCell.addElement(signatureImg);
                } catch (Exception e) {
                    log.warn("Failed to load signature image: {}", user.getSignatureUrl());
                    imageCell.addElement(new Paragraph("(Signature not available)", FONT_SMALL));
                }
            } else {
                imageCell.addElement(new Paragraph("(No signature on file)", FONT_SMALL));
            }
        }
        table.addCell(imageCell);

        // Cell for the signature line and text
        PdfPCell lineCell = new PdfPCell();
        lineCell.setBorder(Rectangle.TOP);
        lineCell.setPaddingTop(5);

        User user = (approver != null) ? approver : po.getBuyer();
        String name = (user != null) ? user.getFirstName() + " " + user.getLastName() : "Authorized Manager";

        lineCell.addElement(new Paragraph("Authorized Signature", FONT_HEADER));
        lineCell.addElement(new Paragraph(name, FONT_NORMAL));
        lineCell.addElement(new Paragraph("Date: " + java.time.LocalDate.now(), FONT_NORMAL));

        if (signed) {
            lineCell.addElement(new Paragraph("(Digitally Signed via SupplyMind)", FONT_SMALL));
        } else {
            lineCell.addElement(new Paragraph("(Draft - Not valid for payment)", FONT_SMALL));
        }

        table.addCell(lineCell);
        doc.add(table);
    }

    private void addFooter(Document doc) throws DocumentException {
        Paragraph footer = new Paragraph("\n\nGenerated by SupplyMind Platform", FONT_SMALL);
        footer.setAlignment(Element.ALIGN_CENTER);
        doc.add(footer);
    }
}
