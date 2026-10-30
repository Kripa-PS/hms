package com.hms.ui;

import com.hms.dao.InvoiceDAO;
import com.hms.dao.InvoiceDAOImpl;
import com.hms.model.Invoice;
import com.hms.util.InvoiceRenderer;
import com.hms.util.UserSession;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * CustomerMyInvoicesPanel
 * 
 * Displays tax invoices generated for the currently logged-in customer.
 * Enforces customer isolation at DAO level (only retrieves own invoices).
 * Provides one-click invoice viewing and PDF printing/exporting.
 */
public class CustomerMyInvoicesPanel extends JPanel {

    private final InvoiceDAO invoiceDAO;

    private JTable invoiceTable;
    private DefaultTableModel tableModel;
    private JButton btnViewInvoice;
    private JButton btnRefresh;
    private JLabel lblStatus;

    private List<Invoice> currentInvoices = new ArrayList<>();

    public CustomerMyInvoicesPanel() {
        this.invoiceDAO = new InvoiceDAOImpl();

        setLayout(new BorderLayout(15, 15));
        setBorder(new EmptyBorder(15, 20, 15, 20));
        setBackground(Color.WHITE);

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JLabel heading = new JLabel("My Invoices & Receipts");
        heading.setFont(new Font("Segoe UI", Font.BOLD, 20));
        heading.setForeground(new Color(24, 43, 73));
        headerPanel.add(heading, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);

        btnRefresh = new JButton("Refresh");
        btnRefresh.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnRefresh.addActionListener(e -> loadInvoicesAsync());
        actions.add(btnRefresh);

        btnViewInvoice = new JButton("View & Print Invoice");
        btnViewInvoice.setEnabled(false);
        btnViewInvoice.setBackground(new Color(24, 43, 73));
        btnViewInvoice.setForeground(Color.WHITE);
        btnViewInvoice.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnViewInvoice.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnViewInvoice.addActionListener(e -> onViewInvoice());
        actions.add(btnViewInvoice);

        headerPanel.add(actions, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        // Invoices Table
        String[] cols = {"Invoice #", "Booking Ref", "Issue Date", "Total (Rs.)", "Paid (Rs.)", "Balance (Rs.)", "Payment Status"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        invoiceTable = new JTable(tableModel);
        invoiceTable.setRowHeight(28);
        invoiceTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        invoiceTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        invoiceTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        invoiceTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = invoiceTable.getSelectedRow();
                btnViewInvoice.setEnabled(row >= 0 && row < currentInvoices.size());
            }
        });

        add(new JScrollPane(invoiceTable), BorderLayout.CENTER);

        // Status bar
        lblStatus = new JLabel("Loading invoices...");
        lblStatus.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblStatus.setForeground(Color.GRAY);
        add(lblStatus, BorderLayout.SOUTH);

        // Initial load
        loadInvoicesAsync();
    }

    public void loadInvoicesAsync() {
        Integer guestId = UserSession.getCurrentGuestId();
        if (guestId == null || guestId <= 0) {
            lblStatus.setText("Guest record not linked to this customer account.");
            return;
        }

        lblStatus.setText("Fetching invoices...");
        btnRefresh.setEnabled(false);

        SwingWorker<List<Invoice>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Invoice> doInBackground() {
                return invoiceDAO.findInvoicesByGuestId(guestId);
            }

            @Override
            protected void done() {
                btnRefresh.setEnabled(true);
                try {
                    currentInvoices = get();
                    tableModel.setRowCount(0);

                    for (Invoice inv : currentInvoices) {
                        String statusStr = "PAID IN FULL";
                        if (inv.getBalanceDue() != null && inv.getBalanceDue().compareTo(BigDecimal.ZERO) > 0) {
                            if (inv.getPaidAmount() != null && inv.getPaidAmount().compareTo(BigDecimal.ZERO) > 0) {
                                statusStr = "PARTIAL";
                            } else {
                                statusStr = "UNPAID";
                            }
                        }

                        String dateStr = inv.getIssueDate() != null
                                ? inv.getIssueDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                                : "-";

                        tableModel.addRow(new Object[]{
                                inv.getInvoiceNumber(),
                                inv.getBookingReference() != null ? inv.getBookingReference() : ("#" + inv.getBookingId()),
                                dateStr,
                                "Rs. " + inv.getTotalAmount().setScale(2, RoundingMode.HALF_UP),
                                "Rs. " + inv.getPaidAmount().setScale(2, RoundingMode.HALF_UP),
                                "Rs. " + inv.getBalanceDue().setScale(2, RoundingMode.HALF_UP),
                                statusStr
                        });
                    }

                    if (currentInvoices.isEmpty()) {
                        lblStatus.setText("No invoices found.");
                    } else {
                        lblStatus.setText("Showing " + currentInvoices.size() + " invoice(s).");
                    }

                    btnViewInvoice.setEnabled(false);

                } catch (Exception ex) {
                    lblStatus.setText("Failed to load invoices: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void onViewInvoice() {
        int row = invoiceTable.getSelectedRow();
        if (row < 0 || row >= currentInvoices.size()) return;

        Invoice inv = currentInvoices.get(row);
        InvoiceRenderer.showInvoiceDialog(this, inv);
    }
}
