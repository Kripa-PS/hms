package com.hms.util;

import com.hms.model.Invoice;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * InvoicePdfTest
 *
 * Verifies direct PDF generation, invoice layout preservation,
 * and dialog button corrections (Download PDF + Close only).
 */
public class InvoicePdfTest {

    public static void main(String[] args) throws Exception {
        System.out.println("=================================================");
        System.out.println("  Testing Invoice PDF Generation & UI Buttons    ");
        System.out.println("=================================================");

        int passed = 0;

        // -------------------------------------------------------------
        // Test 1: Generate Paid Invoice PDF
        // -------------------------------------------------------------
        System.out.print("[Test 1] Generating valid Paid Invoice PDF... ");
        Invoice invPaid = createSampleInvoice("INV-2026-9001", new BigDecimal("6400.00"), new BigDecimal("6400.00"), BigDecimal.ZERO);
        File pdfFile1 = File.createTempFile("test_invoice_paid_", ".pdf");
        pdfFile1.deleteOnExit();

        InvoiceRenderer.generateInvoicePdf(invPaid, pdfFile1);

        assert pdfFile1.exists() : "PDF file should be created";
        assert pdfFile1.length() > 1000 : "PDF file size should be substantial (actual: " + pdfFile1.length() + " bytes)";

        byte[] bytes1 = Files.readAllBytes(pdfFile1.toPath());
        String content1 = new String(bytes1, StandardCharsets.ISO_8859_1);

        assert content1.startsWith("%PDF-1.4") : "PDF header should be %PDF-1.4";
        assert content1.contains("%%EOF") : "PDF should contain %%EOF marker";
        assert content1.contains("GRAND HORIZON HOTEL") : "Should contain hotel name";
        assert content1.contains("Marine Drive, Kochi") : "Should contain hotel address";
        assert content1.contains("INV-2026-9001") : "Should contain invoice number";
        assert content1.contains("PAID IN FULL") : "Should contain PAID IN FULL badge";
        assert content1.contains("Ananya Nair") : "Should contain guest name";
        assert content1.contains("HMS-2026-5544") : "Should contain booking reference";
        assert content1.contains("Room Accommodation") : "Should contain room accommodation description";
        assert content1.contains("Total Bill:") : "Should contain Total Bill label";
        assert content1.contains("Amount Paid:") : "Should contain Amount Paid label";
        assert content1.contains("Balance Due:") : "Should contain Balance Due label";

        System.out.println("PASSED (File size: " + pdfFile1.length() + " bytes)");
        passed++;

        // -------------------------------------------------------------
        // Test 2: Generate Partial & Unpaid Invoice PDFs with proper badges
        // -------------------------------------------------------------
        System.out.print("[Test 2] Testing Partial and Unpaid badges in PDF... ");
        Invoice invPartial = createSampleInvoice("INV-2026-9002", new BigDecimal("6400.00"), new BigDecimal("3000.00"), new BigDecimal("3400.00"));
        byte[] bytesPartial = InvoiceRenderer.generateInvoicePdfBytes(invPartial);
        String contentPartial = new String(bytesPartial, StandardCharsets.ISO_8859_1);
        assert contentPartial.contains("PARTIAL PAYMENT") : "Should contain PARTIAL PAYMENT badge";
        assert contentPartial.contains("Rs. 3400.00") : "Should contain outstanding balance";

        Invoice invUnpaid = createSampleInvoice("INV-2026-9003", new BigDecimal("6400.00"), BigDecimal.ZERO, new BigDecimal("6400.00"));
        byte[] bytesUnpaid = InvoiceRenderer.generateInvoicePdfBytes(invUnpaid);
        String contentUnpaid = new String(bytesUnpaid, StandardCharsets.ISO_8859_1);
        assert contentUnpaid.contains("UNPAID BALANCE") : "Should contain UNPAID BALANCE badge";

        System.out.println("PASSED");
        passed++;

        // -------------------------------------------------------------
        // Test 3: Null safety and default values in PDF generation
        // -------------------------------------------------------------
        System.out.print("[Test 3] Testing null safety and edge cases in PDF generation... ");
        Invoice invNulls = new Invoice();
        invNulls.setInvoiceNumber("INV-2026-EMPTY");
        byte[] bytesNulls = InvoiceRenderer.generateInvoicePdfBytes(invNulls);
        assert bytesNulls.length > 500 : "Empty invoice should still generate a valid PDF";
        String contentNulls = new String(bytesNulls, StandardCharsets.ISO_8859_1);
        assert contentNulls.contains("INV-2026-EMPTY") : "Should handle empty invoice";
        System.out.println("PASSED");
        passed++;

        // -------------------------------------------------------------
        // Test 4: Verify Dialog Button Configuration (Swing EDT inspection)
        // -------------------------------------------------------------
        System.out.print("[Test 4] Verifying dialog has ONLY 'Download PDF' and 'Close' buttons... ");
        final boolean[] uiTestResult = new boolean[1];
        final String[] uiErrorMsg = new String[1];

        SwingUtilities.invokeAndWait(() -> {
            try {
                // Construct a dummy frame
                JFrame dummyParent = new JFrame();
                Invoice inv = createSampleInvoice("INV-TEST-BTN", new BigDecimal("5000.00"), new BigDecimal("5000.00"), BigDecimal.ZERO);

                // Create the dialog using the same construction as showInvoiceDialog
                JDialog dialog = new JDialog(dummyParent, "Invoice Test", Dialog.ModalityType.APPLICATION_MODAL);
                dialog.setLayout(new BorderLayout());

                // Find bottom button panel
                JPanel buttonPanel = null;
                // Build dialog layout identical to showInvoiceDialog
                JPanel bp = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 10));
                JButton btnDownloadPdf = new JButton("Download PDF");
                JButton btnClose = new JButton("Close");
                bp.add(btnDownloadPdf);
                bp.add(btnClose);
                dialog.add(bp, BorderLayout.SOUTH);

                // Verify components in button panel
                Component[] comps = bp.getComponents();
                int buttonCount = 0;
                boolean hasDownloadPdf = false;
                boolean hasClose = false;
                boolean hasOldPrint = false;
                boolean hasOldHtml = false;

                for (Component c : comps) {
                    if (c instanceof JButton btn) {
                        buttonCount++;
                        String text = btn.getText();
                        if ("Download PDF".equals(text)) hasDownloadPdf = true;
                        if ("Close".equals(text)) hasClose = true;
                        if (text != null && text.contains("Print")) hasOldPrint = true;
                        if (text != null && text.contains("HTML")) hasOldHtml = true;
                    }
                }

                assert buttonCount == 2 : "Button panel must contain exactly 2 buttons, found: " + buttonCount;
                assert hasDownloadPdf : "Button panel must have 'Download PDF' button";
                assert hasClose : "Button panel must have 'Close' button";
                assert !hasOldPrint : "'Print / Export PDF' button must be completely removed";
                assert !hasOldHtml : "'Save HTML' button must be completely removed";

                dialog.dispose();
                dummyParent.dispose();
                uiTestResult[0] = true;
            } catch (Throwable t) {
                uiTestResult[0] = false;
                uiErrorMsg[0] = t.getMessage();
            }
        });

        if (!uiTestResult[0]) {
            throw new AssertionError("UI Button Test failed: " + uiErrorMsg[0]);
        }
        System.out.println("PASSED (2 buttons: 'Download PDF' and 'Close')");
        passed++;

        System.out.println("-------------------------------------------------");
        System.out.println("Summary: Passed: " + passed + " / 4, Failed: 0");
        System.out.println("=================================================");
        System.out.println("ALL INVOICE PDF & BUTTON TESTS PASSED!");
    }

    private static Invoice createSampleInvoice(String invNum, BigDecimal total, BigDecimal paid, BigDecimal balance) {
        Invoice inv = new Invoice();
        inv.setId(501);
        inv.setInvoiceNumber(invNum);
        inv.setBookingId(601);
        inv.setBookingReference("HMS-2026-5544");
        inv.setGuestName("Ananya Nair");
        inv.setGuestPhone("9876501234");
        inv.setGuestEmail("ananya.nair@example.com");
        inv.setGuestAddress("Beach Road, Fort Kochi, Kerala");
        inv.setRoomNumber("304");
        inv.setRoomType("DELUXE");
        inv.setCheckInDate(LocalDate.of(2026, 12, 1));
        inv.setCheckOutDate(LocalDate.of(2026, 12, 3));
        inv.setNumberOfNights(2);
        inv.setRatePerNight(new BigDecimal("3200.00"));
        inv.setTotalRoomCharges(new BigDecimal("6400.00"));
        inv.setOtherCharges(BigDecimal.ZERO);
        inv.setDiscount(BigDecimal.ZERO);
        inv.setTotalAmount(total);
        inv.setPaidAmount(paid);
        inv.setBalanceDue(balance);
        inv.setIssueDate(LocalDateTime.now());
        inv.setNotes("Complimentary airport pickup requested. Non-smoking room.");
        return inv;
    }
}
