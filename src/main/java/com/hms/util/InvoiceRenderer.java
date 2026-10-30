package com.hms.util;

import com.hms.model.Invoice;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * InvoiceRenderer
 * 
 * Shared utility for generating professional HTML and PDF invoice documents,
 * previewing in Swing dialogs, and downloading directly as PDF files.
 */
public class InvoiceRenderer {

    /**
     * Builds standard HTML markup for an Invoice (used for in-app preview).
     */
    public static String buildInvoiceHtml(Invoice inv) {
        if (inv == null) {
            return "<html><body style='font-family:sans-serif; text-align:center; padding:50px; color:#888;'>"
                    + "<h3>No Invoice Available</h3></body></html>";
        }

        String badgeColor = "#27ae60";
        String badgeText = "PAID IN FULL";
        if (inv.getBalanceDue() != null && inv.getBalanceDue().compareTo(BigDecimal.ZERO) > 0) {
            if (inv.getPaidAmount() != null && inv.getPaidAmount().compareTo(BigDecimal.ZERO) > 0) {
                badgeColor = "#e67e22";
                badgeText = "PARTIAL PAYMENT";
            } else {
                badgeColor = "#c0392b";
                badgeText = "UNPAID BALANCE";
            }
        }

        String issueDateStr = inv.getIssueDate() != null 
                ? inv.getIssueDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) 
                : LocalDate.now().toString();

        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style='font-family:Segoe UI, sans-serif; margin:20px; color:#2c3e50; font-size:12px;'>")
          .append("<table width='100%' style='border-bottom:2px solid #182b49; padding-bottom:10px;'>")
          .append("<tr>")
          .append("  <td>")
          .append("    <h2 style='margin:0; color:#182b49;'>GRAND HORIZON HOTEL</h2>")
          .append("    <span style='color:#7f8c8d;'>Marine Drive, Kochi, Kerala - 682011</span><br>")
          .append("    <span style='color:#7f8c8d;'>Phone: +91 484 2233445 | Email: billing@grandhorizon.com</span>")
          .append("  </td>")
          .append("  <td align='right' valign='top'>")
          .append("    <h3 style='margin:0; color:#182b49;'>INVOICE</h3>")
          .append("    <b>Invoice #:</b> ").append(inv.getInvoiceNumber() != null ? inv.getInvoiceNumber() : "-").append("<br>")
          .append("    <b>Date:</b> ").append(issueDateStr).append("<br>")
          .append("    <span style='background:").append(badgeColor).append("; color:white; padding:3px 8px; font-weight:bold; border-radius:3px; font-size:11px;'>").append(badgeText).append("</span>")
          .append("  </td>")
          .append("</tr>")
          .append("</table>")
          .append("<br>")
          .append("<table width='100%'>")
          .append("<tr>")
          .append("  <td width='50%' valign='top' style='background:#f8f9fa; padding:10px; border-radius:4px;'>")
          .append("    <b>BILLED TO:</b><br>")
          .append("    <span style='font-size:13px; font-weight:bold; color:#182b49;'>").append(inv.getGuestName() != null ? inv.getGuestName() : "-").append("</span><br>")
          .append("    Phone: ").append(inv.getGuestPhone() != null ? inv.getGuestPhone() : "-").append("<br>")
          .append("    Email: ").append(inv.getGuestEmail() != null ? inv.getGuestEmail() : "-").append("<br>")
          .append("    Address: ").append(inv.getGuestAddress() != null ? inv.getGuestAddress() : "-")
          .append("  </td>")
          .append("  <td width='50%' valign='top' style='background:#f8f9fa; padding:10px; border-radius:4px; margin-left:10px;'>")
          .append("    <b>RESERVATION DETAILS:</b><br>")
          .append("    <b>Booking Reference:</b> ").append(inv.getBookingReference() != null ? inv.getBookingReference() : ("#" + inv.getBookingId())).append("<br>")
          .append("    <b>Room:</b> ").append(inv.getRoomNumber() != null ? inv.getRoomNumber() : "-").append(" (").append(inv.getRoomType() != null ? inv.getRoomType() : "Standard").append(")<br>")
          .append("    <b>Check-in:</b> ").append(inv.getCheckInDate() != null ? inv.getCheckInDate() : "-").append("<br>")
          .append("    <b>Check-out:</b> ").append(inv.getCheckOutDate() != null ? inv.getCheckOutDate() : "-").append(" (").append(inv.getNumberOfNights()).append(" nights)")
          .append("  </td>")
          .append("</tr>")
          .append("</table>")
          .append("<br>")
          .append("<table width='100%' border='1' cellspacing='0' cellpadding='6' style='border-collapse:collapse; border-color:#e0e0e0;'>")
          .append("<tr style='background:#182b49; color:white; font-weight:bold;'>")
          .append("  <th align='left'>Description</th>")
          .append("  <th align='center'>Rate / Night</th>")
          .append("  <th align='center'>Nights</th>")
          .append("  <th align='right'>Amount (Rs.)</th>")
          .append("</tr>")
          .append("<tr>")
          .append("  <td>Room Accommodation (").append(inv.getRoomType() != null ? inv.getRoomType() : "Standard").append(" Room ").append(inv.getRoomNumber() != null ? inv.getRoomNumber() : "-").append(")</td>")
          .append("  <td align='center'>Rs. ").append(inv.getRatePerNight() != null ? inv.getRatePerNight() : "0.00").append("</td>")
          .append("  <td align='center'>").append(inv.getNumberOfNights()).append("</td>")
          .append("  <td align='right'>Rs. ").append(inv.getTotalRoomCharges() != null ? inv.getTotalRoomCharges() : inv.getTotalAmount()).append("</td>")
          .append("</tr>");

        if (inv.getOtherCharges() != null && inv.getOtherCharges().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("<tr><td>Additional Services & Amenities</td><td align='center'>-</td><td align='center'>-</td><td align='right'>Rs. ").append(inv.getOtherCharges()).append("</td></tr>");
        }
        if (inv.getDiscount() != null && inv.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("<tr style='color:#27ae60;'><td>Special Promotion / Discount</td><td align='center'>-</td><td align='center'>-</td><td align='right'>-Rs. ").append(inv.getDiscount()).append("</td></tr>");
        }

        sb.append("</table>")
          .append("<br>")
          .append("<table width='100%'>")
          .append("<tr>")
          .append("  <td width='55%' valign='top' style='color:#7f8c8d; font-size:11px;'>")
          .append("    <b>Notes:</b><br>")
          .append("    ").append(inv.getNotes() != null && !inv.getNotes().isBlank() ? inv.getNotes() : "Thank you for staying with Grand Horizon Hotel!").append("<br><br>")
          .append("    * Standard check-in time: 14:00 | Check-out time: 11:00.<br>")
          .append("    * This is a computer-generated tax invoice.")
          .append("  </td>")
          .append("  <td width='45%' align='right'>")
          .append("    <table width='100%' cellpadding='4' style='font-size:12px;'>")
          .append("    <tr><td><b>Total Bill:</b></td><td align='right'><b>Rs. ").append(inv.getTotalAmount() != null ? inv.getTotalAmount() : "0.00").append("</b></td></tr>")
          .append("    <tr style='color:#27ae60;'><td><b>Amount Paid:</b></td><td align='right'><b>Rs. ").append(inv.getPaidAmount() != null ? inv.getPaidAmount() : "0.00").append("</b></td></tr>")
          .append("    <tr style='border-top:1px solid #182b49; font-size:13px; color:").append(inv.getBalanceDue() != null && inv.getBalanceDue().compareTo(BigDecimal.ZERO) > 0 ? "#c0392b" : "#27ae60").append(";'>")
          .append("      <td><b>Balance Due:</b></td><td align='right'><b>Rs. ").append(inv.getBalanceDue() != null ? inv.getBalanceDue() : "0.00").append("</b></td>")
          .append("    </tr>")
          .append("    </table>")
          .append("  </td>")
          .append("</tr>")
          .append("</table>")
          .append("</body></html>");

        return sb.toString();
    }

    /**
     * Shows a modal dialog with full invoice preview, a single 'Download PDF' button,
     * and a 'Close' button.
     */
    public static void showInvoiceDialog(Component parent, Invoice inv) {
        if (inv == null) {
            JOptionPane.showMessageDialog(parent, "Invoice record is not available.", "No Invoice", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String invNum = inv.getInvoiceNumber() != null ? inv.getInvoiceNumber() : "Invoice";
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent), "Invoice - " + invNum, Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(750, 680);
        dialog.setLocationRelativeTo(parent);
        dialog.setLayout(new BorderLayout());

        JEditorPane editorPane = new JEditorPane();
        editorPane.setContentType("text/html");
        editorPane.setEditable(false);
        editorPane.setText(buildInvoiceHtml(inv));
        editorPane.setCaretPosition(0);

        JScrollPane scrollPane = new JScrollPane(editorPane);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(220, 225, 230)));
        dialog.add(scrollPane, BorderLayout.CENTER);

        // Buttons Bar: Contains ONLY "Download PDF" and "Close"
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 10));
        buttonPanel.setBackground(new Color(245, 247, 250));
        buttonPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(220, 225, 230)));

        JButton btnDownloadPdf = new JButton("Download PDF");
        btnDownloadPdf.setBackground(new Color(24, 43, 73));
        btnDownloadPdf.setForeground(Color.WHITE);
        btnDownloadPdf.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnDownloadPdf.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnDownloadPdf.setFocusPainted(false);
        btnDownloadPdf.addActionListener(e -> promptAndDownloadPdf(dialog, inv));

        JButton btnClose = new JButton("Close");
        btnClose.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnClose.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnClose.setFocusPainted(false);
        btnClose.addActionListener(e -> dialog.dispose());

        buttonPanel.add(btnDownloadPdf);
        buttonPanel.add(btnClose);

        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    /**
     * Opens a Save File dialog allowing the user to choose the destination folder
     * and PDF filename, then directly generates and saves the invoice PDF.
     * Does NOT open a browser or system print dialog.
     *
     * @param parent Component used for dialog positioning
     * @param inv    The Invoice object
     * @return true if successfully saved, false if cancelled or failed
     */
    public static boolean promptAndDownloadPdf(Component parent, Invoice inv) {
        if (inv == null) {
            JOptionPane.showMessageDialog(parent, "Invoice record is not available.", "No Invoice", JOptionPane.WARNING_MESSAGE);
            return false;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Invoice as PDF");
        String defaultName = (inv.getInvoiceNumber() != null ? inv.getInvoiceNumber() : "Invoice") + ".pdf";
        fileChooser.setSelectedFile(new File(defaultName));
        fileChooser.setFileFilter(new FileNameExtensionFilter("PDF Documents (*.pdf)", "pdf"));

        int userSelection = fileChooser.showSaveDialog(parent);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            if (!selectedFile.getName().toLowerCase().endsWith(".pdf")) {
                selectedFile = new File(selectedFile.getParentFile(), selectedFile.getName() + ".pdf");
            }

            try {
                generateInvoicePdf(inv, selectedFile);
                JOptionPane.showMessageDialog(parent,
                        "Invoice PDF successfully downloaded to:\n" + selectedFile.getAbsolutePath(),
                        "Download Complete", JOptionPane.INFORMATION_MESSAGE);
                return true;
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(parent,
                        "Failed to generate and save PDF:\n" + ex.getMessage(),
                        "Download Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
        return false;
    }

    /**
     * Generates a professional, self-contained PDF document for the invoice
     * and writes it directly to the specified file.
     */
    public static void generateInvoicePdf(Invoice inv, File targetFile) throws IOException {
        if (targetFile.getParentFile() != null && !targetFile.getParentFile().exists()) {
            targetFile.getParentFile().mkdirs();
        }
        byte[] pdfBytes = generateInvoicePdfBytes(inv);
        try (FileOutputStream fos = new FileOutputStream(targetFile)) {
            fos.write(pdfBytes);
            fos.flush();
        }
    }

    /**
     * Pure Java implementation of standard PDF 1.4 invoice document generation.
     * Preserves all hotel details, customer details, reservation details, room charges,
     * total bill, amount paid, balance due, and payment status badge.
     * Zero external dependencies required.
     */
    public static byte[] generateInvoicePdfBytes(Invoice inv) throws IOException {
        ByteArrayOutputStream streamContent = new ByteArrayOutputStream();
        PrintWriter pw = new PrintWriter(new OutputStreamWriter(streamContent, "ISO-8859-1"));

        // Header Section
        // Hotel Name
        drawTextLeft(pw, "F2", 18, 0.094, 0.169, 0.286, 40, 782, "GRAND HORIZON HOTEL");
        drawTextLeft(pw, "F1", 9, 0.45, 0.50, 0.55, 40, 767, "Marine Drive, Kochi, Kerala - 682011");
        drawTextLeft(pw, "F1", 9, 0.45, 0.50, 0.55, 40, 754, "Phone: +91 484 2233445 | Email: billing@grandhorizon.com");

        // Top Right: INVOICE title & metadata
        drawTextRight(pw, "F2", 18, 0.094, 0.169, 0.286, 555, 782, "INVOICE");
        String invNum = inv.getInvoiceNumber() != null ? inv.getInvoiceNumber() : "INV-DRAFT";
        drawTextRight(pw, "F1", 9.5, 0.2, 0.25, 0.3, 555, 767, "Invoice #: " + invNum);
        String issueDateStr = inv.getIssueDate() != null 
                ? inv.getIssueDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) 
                : LocalDate.now().toString();
        drawTextRight(pw, "F1", 9.5, 0.2, 0.25, 0.3, 555, 754, "Date: " + issueDateStr);

        // Status Badge
        String badgeText;
        double badgeR, badgeG, badgeB;
        if (inv.getBalanceDue() != null && inv.getBalanceDue().compareTo(BigDecimal.ZERO) == 0) {
            badgeText = "PAID IN FULL";
            badgeR = 0.153; badgeG = 0.682; badgeB = 0.376; // Emerald Green
        } else if (inv.getPaidAmount() != null && inv.getPaidAmount().compareTo(BigDecimal.ZERO) > 0) {
            badgeText = "PARTIAL PAYMENT";
            badgeR = 0.902; badgeG = 0.494; badgeB = 0.133; // Amber
        } else {
            badgeText = "UNPAID BALANCE";
            badgeR = 0.753; badgeG = 0.224; badgeB = 0.169; // Crimson Red
        }

        // Draw badge box (x=435, y=728, w=120, h=18)
        pw.printf(Locale.US, "%.3f %.3f %.3f rg 435 728 120 18 re f%n", badgeR, badgeG, badgeB);
        drawTextCenter(pw, "F2", 9, 1.0, 1.0, 1.0, 495, 733, badgeText);

        // Navy divider line under header
        pw.println("0.094 0.169 0.286 RG 2 w 40 722 m 555 722 l S");

        // Info Cards: Billed To (Left) & Reservation Details (Right)
        // Card Left: x=40, y=628, w=250, h=82
        pw.println("0.973 0.976 0.980 rg 40 628 250 82 re f");
        pw.println("0.880 0.900 0.920 RG 0.75 w 40 628 250 82 re S");

        drawTextLeft(pw, "F2", 8.5, 0.45, 0.50, 0.55, 50, 697, "BILLED TO:");
        String guestName = inv.getGuestName() != null ? inv.getGuestName() : "-";
        drawTextLeft(pw, "F2", 11, 0.094, 0.169, 0.286, 50, 682, guestName);
        drawTextLeft(pw, "F1", 8.5, 0.2, 0.25, 0.3, 50, 669, "Phone: " + (inv.getGuestPhone() != null ? inv.getGuestPhone() : "-"));
        drawTextLeft(pw, "F1", 8.5, 0.2, 0.25, 0.3, 50, 656, "Email: " + (inv.getGuestEmail() != null ? inv.getGuestEmail() : "-"));
        drawTextLeft(pw, "F1", 8.5, 0.2, 0.25, 0.3, 50, 643, "Address: " + (inv.getGuestAddress() != null ? inv.getGuestAddress() : "-"));

        // Card Right: x=305, y=628, w=250, h=82
        pw.println("0.973 0.976 0.980 rg 305 628 250 82 re f");
        pw.println("0.880 0.900 0.920 RG 0.75 w 305 628 250 82 re S");

        drawTextLeft(pw, "F2", 8.5, 0.45, 0.50, 0.55, 315, 697, "RESERVATION DETAILS:");
        String bRef = inv.getBookingReference() != null ? inv.getBookingReference() : ("#" + inv.getBookingId());
        drawTextLeft(pw, "F1", 8.5, 0.2, 0.25, 0.3, 315, 682, "Booking Reference: " + bRef);
        String rNum = inv.getRoomNumber() != null ? inv.getRoomNumber() : "-";
        String rType = inv.getRoomType() != null ? inv.getRoomType() : "Standard";
        drawTextLeft(pw, "F1", 8.5, 0.2, 0.25, 0.3, 315, 669, "Room: Room " + rNum + " (" + rType + ")");
        String cIn = inv.getCheckInDate() != null ? inv.getCheckInDate().toString() : "-";
        drawTextLeft(pw, "F1", 8.5, 0.2, 0.25, 0.3, 315, 656, "Check-in: " + cIn);
        String cOut = inv.getCheckOutDate() != null ? inv.getCheckOutDate().toString() : "-";
        long nights = inv.getNumberOfNights();
        drawTextLeft(pw, "F1", 8.5, 0.2, 0.25, 0.3, 315, 643, "Check-out: " + cOut + " (" + nights + " night" + (nights > 1 ? "s" : "") + ")");

        // Charges Table
        double tableY = 600;
        double rowH = 24;

        // Table Header: x=40, w=515, h=24
        pw.println("0.094 0.169 0.286 rg 40 " + (tableY - rowH) + " 515 " + rowH + " re f");
        drawTextLeft(pw, "F2", 9.5, 1.0, 1.0, 1.0, 52, tableY - 16, "Description");
        drawTextCenter(pw, "F2", 9.5, 1.0, 1.0, 1.0, 315, tableY - 16, "Rate / Night");
        drawTextCenter(pw, "F2", 9.5, 1.0, 1.0, 1.0, 395, tableY - 16, "Nights");
        drawTextRight(pw, "F2", 9.5, 1.0, 1.0, 1.0, 545, tableY - 16, "Amount (Rs.)");

        double currY = tableY - rowH;

        // Row 1: Room charges
        currY -= rowH;
        pw.println("0.880 0.900 0.920 RG 0.5 w 40 " + currY + " 515 " + rowH + " re S");
        String desc = "Room Accommodation (" + rType + " Room " + rNum + ")";
        drawTextLeft(pw, "F1", 9, 0.15, 0.2, 0.25, 52, currY + 7, desc);
        String rateStr = "Rs. " + (inv.getRatePerNight() != null ? inv.getRatePerNight().setScale(2, java.math.RoundingMode.HALF_UP).toPlainString() : "0.00");
        drawTextCenter(pw, "F1", 9, 0.15, 0.2, 0.25, 315, currY + 7, rateStr);
        drawTextCenter(pw, "F1", 9, 0.15, 0.2, 0.25, 395, currY + 7, String.valueOf(nights));
        BigDecimal roomTotal = inv.getTotalRoomCharges() != null ? inv.getTotalRoomCharges() : (inv.getTotalAmount() != null ? inv.getTotalAmount() : BigDecimal.ZERO);
        String roomTotalStr = "Rs. " + roomTotal.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
        drawTextRight(pw, "F1", 9, 0.15, 0.2, 0.25, 545, currY + 7, roomTotalStr);

        // Row 2: Additional services if > 0
        if (inv.getOtherCharges() != null && inv.getOtherCharges().compareTo(BigDecimal.ZERO) > 0) {
            currY -= rowH;
            pw.println("0.880 0.900 0.920 RG 0.5 w 40 " + currY + " 515 " + rowH + " re S");
            drawTextLeft(pw, "F1", 9, 0.15, 0.2, 0.25, 52, currY + 7, "Additional Services & Amenities");
            drawTextCenter(pw, "F1", 9, 0.15, 0.2, 0.25, 315, currY + 7, "-");
            drawTextCenter(pw, "F1", 9, 0.15, 0.2, 0.25, 395, currY + 7, "-");
            String otherStr = "Rs. " + inv.getOtherCharges().setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
            drawTextRight(pw, "F1", 9, 0.15, 0.2, 0.25, 545, currY + 7, otherStr);
        }

        // Row 3: Discounts if > 0
        if (inv.getDiscount() != null && inv.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            currY -= rowH;
            pw.println("0.880 0.900 0.920 RG 0.5 w 40 " + currY + " 515 " + rowH + " re S");
            drawTextLeft(pw, "F1", 9, 0.15, 0.65, 0.35, 52, currY + 7, "Special Promotion / Discount");
            drawTextCenter(pw, "F1", 9, 0.15, 0.65, 0.35, 315, currY + 7, "-");
            drawTextCenter(pw, "F1", 9, 0.15, 0.65, 0.35, 395, currY + 7, "-");
            String discStr = "-Rs. " + inv.getDiscount().setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
            drawTextRight(pw, "F1", 9, 0.15, 0.65, 0.35, 545, currY + 7, discStr);
        }

        // Lower Section: Notes on left, Totals summary box on right
        double bottomSectionY = currY - 20;

        // Notes Box (Left: x=40, y=bottomSectionY - 95, w=275, h=95)
        drawTextLeft(pw, "F2", 9.5, 0.094, 0.169, 0.286, 42, bottomSectionY - 12, "Notes & Policies:");
        String notesText = (inv.getNotes() != null && !inv.getNotes().isBlank()) ? inv.getNotes() : "Thank you for staying with Grand Horizon Hotel!";
        drawTextLeft(pw, "F1", 8.5, 0.25, 0.30, 0.35, 42, bottomSectionY - 28, notesText);
        drawTextLeft(pw, "F1", 8, 0.45, 0.50, 0.55, 42, bottomSectionY - 48, "* Standard check-in time: 14:00 | Check-out time: 11:00.");
        drawTextLeft(pw, "F1", 8, 0.45, 0.50, 0.55, 42, bottomSectionY - 62, "* All accommodations are non-smoking. Complimentary Wi-Fi included.");
        drawTextLeft(pw, "F1", 8, 0.45, 0.50, 0.55, 42, bottomSectionY - 76, "* This is a computer-generated tax invoice.");

        // Totals Box (Right: x=330, y=bottomSectionY - 95, w=225, h=95)
        double boxY = bottomSectionY - 95;
        pw.println("0.973 0.976 0.980 rg 330 " + boxY + " 225 95 re f");
        pw.println("0.850 0.880 0.900 RG 0.75 w 330 " + boxY + " 225 95 re S");

        // Total Bill
        drawTextLeft(pw, "F2", 10, 0.1, 0.15, 0.2, 345, boxY + 70, "Total Bill:");
        String totStr = "Rs. " + (inv.getTotalAmount() != null ? inv.getTotalAmount().setScale(2, java.math.RoundingMode.HALF_UP).toPlainString() : "0.00");
        drawTextRight(pw, "F2", 10, 0.1, 0.15, 0.2, 545, boxY + 70, totStr);

        // Amount Paid
        drawTextLeft(pw, "F2", 10, 0.153, 0.682, 0.376, 345, boxY + 48, "Amount Paid:");
        String paidStr = "Rs. " + (inv.getPaidAmount() != null ? inv.getPaidAmount().setScale(2, java.math.RoundingMode.HALF_UP).toPlainString() : "0.00");
        drawTextRight(pw, "F2", 10, 0.153, 0.682, 0.376, 545, boxY + 48, paidStr);

        // Divider in totals box
        pw.println("0.750 0.800 0.850 RG 1 w 340 " + (boxY + 36) + " m 545 " + (boxY + 36) + " l S");

        // Balance Due
        boolean isZeroBalance = (inv.getBalanceDue() == null || inv.getBalanceDue().compareTo(BigDecimal.ZERO) == 0);
        if (isZeroBalance) {
            drawTextLeft(pw, "F2", 11, 0.153, 0.682, 0.376, 345, boxY + 16, "Balance Due:");
            drawTextRight(pw, "F2", 11, 0.153, 0.682, 0.376, 545, boxY + 16, "Rs. 0.00");
        } else {
            drawTextLeft(pw, "F2", 11, 0.753, 0.224, 0.169, 345, boxY + 16, "Balance Due:");
            String balStr = "Rs. " + inv.getBalanceDue().setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
            drawTextRight(pw, "F2", 11, 0.753, 0.224, 0.169, 545, boxY + 16, balStr);
        }

        // Footer Bar & Watermark
        pw.println("0.850 0.880 0.900 RG 0.5 w 40 45 m 555 45 l S");
        drawTextCenter(pw, "F1", 8, 0.55, 0.60, 0.65, 297, 32, "Grand Horizon Hotel Management System - Official Customer Invoice");

        pw.flush();
        byte[] contentBytes = streamContent.toByteArray();

        // Assemble full standard PDF 1.4
        ByteArrayOutputStream pdfOut = new ByteArrayOutputStream();
        List<Long> offsets = new ArrayList<>();

        // 1. PDF Header
        pdfOut.write(new byte[]{'%', 'P', 'D', 'F', '-', '1', '.', '4', '\n', '%', (byte)0xE2, (byte)0xE3, (byte)0xCF, (byte)0xD3, '\n'});

        // Obj 1: Catalog
        offsets.add((long) pdfOut.size());
        writeAscii(pdfOut, "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");

        // Obj 2: Pages
        offsets.add((long) pdfOut.size());
        writeAscii(pdfOut, "2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n");

        // Obj 3: Page
        offsets.add((long) pdfOut.size());
        writeAscii(pdfOut, "3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595.28 841.89] /Contents 4 0 R /Resources << /Font << /F1 5 0 R /F2 6 0 R >> >> >>\nendobj\n");

        // Obj 4: Content Stream
        offsets.add((long) pdfOut.size());
        writeAscii(pdfOut, "4 0 obj\n<< /Length " + contentBytes.length + " >>\nstream\n");
        pdfOut.write(contentBytes);
        writeAscii(pdfOut, "\nendstream\nendobj\n");

        // Obj 5: Font F1 (Helvetica)
        offsets.add((long) pdfOut.size());
        writeAscii(pdfOut, "5 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>\nendobj\n");

        // Obj 6: Font F2 (Helvetica-Bold)
        offsets.add((long) pdfOut.size());
        writeAscii(pdfOut, "6 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>\nendobj\n");

        // Xref table
        long startxref = pdfOut.size();
        int totalObjs = offsets.size() + 1; // including obj 0
        writeAscii(pdfOut, "xref\n0 " + totalObjs + "\n");
        writeAscii(pdfOut, "0000000000 65535 f \r\n");
        for (Long offset : offsets) {
            writeAscii(pdfOut, String.format(Locale.US, "%010d 00000 n \r\n", offset));
        }

        // Trailer
        writeAscii(pdfOut, "trailer\n<< /Size " + totalObjs + " /Root 1 0 R >>\nstartxref\n" + startxref + "\n%%EOF\n");

        return pdfOut.toByteArray();
    }

    private static void drawTextLeft(PrintWriter pw, String font, double fontSize, double r, double g, double b, double leftX, double y, String text) {
        pw.printf(Locale.US, "BT /%s %.2f Tf %.3f %.3f %.3f rg %.2f %.2f Td (%s) Tj ET%n",
                font, fontSize, r, g, b, leftX, y, escapePdf(text));
    }

    private static void drawTextRight(PrintWriter pw, String font, double fontSize, double r, double g, double b, double rightX, double y, String text) {
        double w = estimateTextWidth(text, fontSize, font.contains("F2"));
        double startX = rightX - w;
        pw.printf(Locale.US, "BT /%s %.2f Tf %.3f %.3f %.3f rg %.2f %.2f Td (%s) Tj ET%n",
                font, fontSize, r, g, b, startX, y, escapePdf(text));
    }

    private static void drawTextCenter(PrintWriter pw, String font, double fontSize, double r, double g, double b, double centerX, double y, String text) {
        double w = estimateTextWidth(text, fontSize, font.contains("F2"));
        double startX = centerX - (w / 2.0);
        pw.printf(Locale.US, "BT /%s %.2f Tf %.3f %.3f %.3f rg %.2f %.2f Td (%s) Tj ET%n",
                font, fontSize, r, g, b, startX, y, escapePdf(text));
    }

    private static double estimateTextWidth(String text, double fontSize, boolean bold) {
        if (text == null || text.isEmpty()) return 0;
        double factor = bold ? 0.58 : 0.52;
        return text.length() * (fontSize * factor);
    }

    private static void writeAscii(ByteArrayOutputStream baos, String s) throws IOException {
        baos.write(s.getBytes("ISO-8859-1"));
    }

    private static String escapePdf(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("(", "\\(")
                .replace(")", "\\)");
    }
}
