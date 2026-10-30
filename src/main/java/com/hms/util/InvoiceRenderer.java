package com.hms.util;

import com.hms.model.Invoice;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.print.PrinterException;
import java.io.File;
import java.io.FileWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * InvoiceRenderer
 * 
 * Shared utility for generating professional HTML invoice documents,
 * previewing in Swing dialogs, printing, and exporting to HTML.
 */
public class InvoiceRenderer {

    /**
     * Builds standard HTML markup for an Invoice.
     */
    public static String buildInvoiceHtml(Invoice inv) {
        if (inv == null) {
            return "<html><body style='font-family:sans-serif; text-align:center; padding:50px; color:#888;'>"
                    + "<h3>No Invoice Available</h3></body></html>";
        }

        String badgeColor = "#27ae60";
        String badgeText = "PAID IN FULL";
        if (inv.getBalanceDue().compareTo(BigDecimal.ZERO) > 0) {
            if (inv.getPaidAmount().compareTo(BigDecimal.ZERO) > 0) {
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
          .append("    <b>Invoice #:</b> ").append(inv.getInvoiceNumber()).append("<br>")
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
          .append("    <b>Check-in:</b> ").append(inv.getCheckInDate()).append("<br>")
          .append("    <b>Check-out:</b> ").append(inv.getCheckOutDate()).append(" (").append(inv.getNumberOfNights()).append(" nights)")
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
          .append("    <tr><td><b>Total Bill:</b></td><td align='right'><b>Rs. ").append(inv.getTotalAmount()).append("</b></td></tr>")
          .append("    <tr style='color:#27ae60;'><td><b>Amount Paid:</b></td><td align='right'><b>Rs. ").append(inv.getPaidAmount()).append("</b></td></tr>")
          .append("    <tr style='border-top:1px solid #182b49; font-size:13px; color:").append(inv.getBalanceDue().compareTo(BigDecimal.ZERO) > 0 ? "#c0392b" : "#27ae60").append(";'>")
          .append("      <td><b>Balance Due:</b></td><td align='right'><b>Rs. ").append(inv.getBalanceDue()).append("</b></td>")
          .append("    </tr>")
          .append("    </table>")
          .append("  </td>")
          .append("</tr>")
          .append("</table>")
          .append("</body></html>");

        return sb.toString();
    }

    /**
     * Shows a modal dialog with full invoice preview, print button, and save button.
     */
    public static void showInvoiceDialog(Component parent, Invoice inv) {
        if (inv == null) {
            JOptionPane.showMessageDialog(parent, "Invoice record is not available.", "No Invoice", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent), "Invoice - " + inv.getInvoiceNumber(), Dialog.ModalityType.APPLICATION_MODAL);
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

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        buttonPanel.setBackground(new Color(245, 247, 250));

        JButton btnPrint = new JButton("Print / Export PDF");
        btnPrint.setBackground(new Color(24, 43, 73));
        btnPrint.setForeground(Color.WHITE);
        btnPrint.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnPrint.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnPrint.addActionListener(e -> {
            try {
                boolean complete = editorPane.print(null, null, true, null, null, true);
                if (complete) {
                    JOptionPane.showMessageDialog(dialog, "Printed successfully or sent to printer.", "Print Job Done", JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (PrinterException ex) {
                JOptionPane.showMessageDialog(dialog, "Printing error: " + ex.getMessage(), "Print Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        JButton btnSaveHtml = new JButton("Save HTML");
        btnSaveHtml.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSaveHtml.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setSelectedFile(new File(inv.getInvoiceNumber() + ".html"));
            int ret = fileChooser.showSaveDialog(dialog);
            if (ret == JFileChooser.APPROVE_OPTION) {
                try (FileWriter fw = new FileWriter(fileChooser.getSelectedFile())) {
                    fw.write(buildInvoiceHtml(inv));
                    JOptionPane.showMessageDialog(dialog, "Invoice saved to " + fileChooser.getSelectedFile().getName(), "Export Successful", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(dialog, "Failed to save: " + ex.getMessage(), "Save Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        JButton btnClose = new JButton("Close");
        btnClose.addActionListener(e -> dialog.dispose());

        buttonPanel.add(btnPrint);
        buttonPanel.add(btnSaveHtml);
        buttonPanel.add(btnClose);

        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }
}
