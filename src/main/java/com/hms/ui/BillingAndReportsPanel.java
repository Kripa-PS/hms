package com.hms.ui;

import com.hms.dao.*;
import com.hms.model.*;
import com.hms.util.BillCalculator;
import com.hms.util.DatabaseConnection;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.print.PrinterException;
import java.io.File;
import java.io.FileWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

/**
 * BillingAndReportsPanel
 * 
 * Comprehensive Swing GUI for Member 3: Billing, Payments and Reports.
 * Features:
 * 1. Billing & Payments: Bill calculation, demo payments (Cash, Card, UPI),
 *    demo statuses (PAID, PARTIAL, PENDING, FAILED, REFUNDED), overpayment prevention,
 *    and real-time balance tracking.
 * 2. Invoices: Invoice generation with itemized charges, professional preview,
 *    and direct printing / PDF export.
 * 3. Reports:
 *    - Booking Report: Date and status filtering.
 *    - Occupancy Report: Real-time room occupancy calculation for any target date.
 *    - Revenue Report: Collections from successful payments, refunds, and daily breakdown.
 */
public class BillingAndReportsPanel extends JPanel {

    private final BookingDAO bookingDAO;
    private final PaymentDAO paymentDAO;
    private final InvoiceDAO invoiceDAO;
    private final ReportDAO reportDAO;

    private static final Color NAVY = new Color(24, 43, 73);
    private static final Color GOLD = new Color(212, 175, 55);
    private static final Color SUCCESS_GREEN = new Color(39, 174, 96);
    private static final Color DANGER_RED = new Color(192, 57, 43);
    private static final Color WARNING_ORANGE = new Color(230, 126, 34);

    private final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // Main Tabbed Pane
    private JTabbedPane mainTabbedPane;

    // --- Tab 1: Billing & Payment Components ---
    private JComboBox<BookingSelectItem> billingBookingCombo;
    private JLabel lblBillGuest;
    private JLabel lblBillRoom;
    private JLabel lblBillDates;
    private JLabel lblBillNights;
    private JLabel lblBillRate;
    private JLabel lblBillTotal;
    private JLabel lblBillPaid;
    private JLabel lblBillBalance;
    private JLabel lblBillStatusBadge;

    private JTextField paymentAmountField;
    private JComboBox<String> paymentMethodCombo;
    private JComboBox<String> paymentStatusCombo;
    private JTextField paymentTxnRefField;
    private JTextField paymentNotesField;
    private JButton btnRecordPayment;
    private JButton btnAutoTxnRef;
    private JButton btnQuickFullSettle;
    private JButton btnClearPayment;

    private JTable paymentHistoryTable;
    private DefaultTableModel paymentHistoryModel;

    // --- Tab 2: Invoices Components ---
    private JComboBox<BookingSelectItem> invoiceBookingCombo;
    private JButton btnGenerateInvoice;
    private JButton btnPrintInvoice;
    private JButton btnExportHtml;
    private JTable invoiceListTable;
    private DefaultTableModel invoiceListModel;
    private JEditorPane invoicePreviewPane;
    private Invoice currentPreviewInvoice = null;

    // --- Tab 3: Reports Components ---
    // Booking report
    private JTextField bkgRepFromDate;
    private JTextField bkgRepToDate;
    private JComboBox<String> bkgRepStatusCombo;
    private JButton btnRunBookingReport;
    private JLabel lblBookingRepStats;
    private JTable bookingReportTable;
    private DefaultTableModel bookingReportModel;

    // Occupancy report
    private JTextField occRepDateField;
    private JButton btnRunOccupancyReport;
    private JButton btnOccToday;
    private JLabel lblOccPercent;
    private JLabel lblOccTotalRooms;
    private JLabel lblOccOccupiedRooms;
    private JLabel lblOccAvailableRooms;
    private JLabel lblOccMaintRooms;
    private JTable occDetailsTable;
    private DefaultTableModel occDetailsModel;

    // Revenue report
    private JTextField revRepFromDate;
    private JTextField revRepToDate;
    private JButton btnRunRevenueReport;
    private JLabel lblRevTotal;
    private JLabel lblRevRefunds;
    private JLabel lblRevNet;
    private JLabel lblRevCount;
    private JTable dailyRevenueTable;
    private DefaultTableModel dailyRevenueModel;

    public BillingAndReportsPanel() {
        this.bookingDAO = new BookingDAOImpl();
        this.paymentDAO = new PaymentDAOImpl();
        this.invoiceDAO = new InvoiceDAOImpl();
        this.reportDAO = new ReportDAOImpl();

        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        // Header Banner
        JPanel headerPanel = createHeader();
        add(headerPanel, BorderLayout.NORTH);

        // Main Tabbed Pane
        mainTabbedPane = new JTabbedPane();
        mainTabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 13));

        mainTabbedPane.addTab("💳 Billing & Payments", createBillingTab());
        mainTabbedPane.addTab("🧾 Invoices & Printing", createInvoicesTab());
        mainTabbedPane.addTab("📊 Reports", createReportsTab());

        add(mainTabbedPane, BorderLayout.CENTER);

        // Initial Data Load
        refreshBookingSelectors();
        loadAllInvoices();
    }

    private JPanel createHeader() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(NAVY);
        panel.setBorder(new EmptyBorder(12, 20, 12, 20));

        JLabel title = new JLabel("Billing, Payments & Reports");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(Color.WHITE);

        JLabel subtitle = new JLabel("Member 3 Module | Invoices, Collections, Occupancy & Revenue Analytics");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitle.setForeground(GOLD);

        JPanel textPanel = new JPanel(new GridLayout(2, 1));
        textPanel.setOpaque(false);
        textPanel.add(title);
        textPanel.add(subtitle);

        panel.add(textPanel, BorderLayout.WEST);

        JButton btnRefreshAll = new JButton("🔄 Refresh Data");
        btnRefreshAll.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnRefreshAll.addActionListener(e -> {
            refreshBookingSelectors();
            loadAllInvoices();
        });
        panel.add(btnRefreshAll, BorderLayout.EAST);

        return panel;
    }

    // =========================================================================
    // TAB 1: BILLING & PAYMENTS
    // =========================================================================

    private JPanel createBillingTab() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));

        // Top Selector Bar
        JPanel topSelector = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        topSelector.setOpaque(false);
        topSelector.add(new JLabel("Select Booking:"));

        billingBookingCombo = new JComboBox<>();
        billingBookingCombo.setPreferredSize(new Dimension(380, 30));
        billingBookingCombo.addActionListener(e -> onBookingSelectedForBilling());
        topSelector.add(billingBookingCombo);

        JButton btnReloadBookings = new JButton("Reload");
        btnReloadBookings.addActionListener(e -> refreshBookingSelectors());
        topSelector.add(btnReloadBookings);

        panel.add(topSelector, BorderLayout.NORTH);

        // Center Split: Left = Bill Summary & Payment Form, Right = Payment History
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setOpaque(false);

        // 1. Bill Summary Card
        JPanel summaryCard = createBillSummaryCard();
        leftPanel.add(summaryCard);
        leftPanel.add(Box.createVerticalStrut(10));

        // 2. Payment Form
        JPanel paymentForm = createPaymentForm();
        leftPanel.add(paymentForm);

        // Right: Payment History Table
        JPanel rightPanel = createPaymentHistoryPanel();

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        split.setDividerLocation(420);
        split.setResizeWeight(0.42);
        split.setContinuousLayout(true);
        split.setBorder(null);

        panel.add(split, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createBillSummaryCard() {
        JPanel card = new JPanel(new GridLayout(5, 2, 8, 6));
        card.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(210, 215, 225), 1),
                " Stay & Bill Summary ",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 13),
                NAVY
        ));
        card.setBackground(new Color(250, 252, 255));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160));

        lblBillGuest = new JLabel("-");
        lblBillRoom = new JLabel("-");
        lblBillDates = new JLabel("-");
        lblBillNights = new JLabel("-");
        lblBillRate = new JLabel("-");
        lblBillTotal = new JLabel("₹0.00");
        lblBillTotal.setFont(new Font("Segoe UI", Font.BOLD, 13));

        lblBillPaid = new JLabel("₹0.00");
        lblBillPaid.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblBillPaid.setForeground(SUCCESS_GREEN);

        lblBillBalance = new JLabel("₹0.00");
        lblBillBalance.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblBillBalance.setForeground(DANGER_RED);

        lblBillStatusBadge = new JLabel("UNPAID");
        lblBillStatusBadge.setFont(new Font("Segoe UI", Font.BOLD, 12));

        card.add(createLabelPair("Guest:", lblBillGuest));
        card.add(createLabelPair("Room:", lblBillRoom));
        card.add(createLabelPair("Stay Dates:", lblBillDates));
        card.add(createLabelPair("Duration & Rate:", lblBillNights));
        card.add(createLabelPair("Total Bill:", lblBillTotal));
        card.add(createLabelPair("Amount Paid:", lblBillPaid));
        card.add(createLabelPair("Balance Due:", lblBillBalance));
        card.add(createLabelPair("Payment Status:", lblBillStatusBadge));

        return card;
    }

    private JPanel createLabelPair(String label, JLabel valLabel) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        p.setOpaque(false);
        JLabel l = new JLabel(label);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        l.setForeground(Color.GRAY);
        p.add(l);
        p.add(valLabel);
        return p;
    }

    private JPanel createPaymentForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(210, 215, 225), 1),
                " Record Payment ",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 13),
                NAVY
        ));
        form.setBackground(Color.WHITE);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Row 0: Amount
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.3;
        form.add(new JLabel("Amount (₹):*"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        paymentAmountField = new JTextField();
        paymentAmountField.setFont(new Font("Segoe UI", Font.BOLD, 13));
        form.add(paymentAmountField, gbc);

        // Row 1: Payment Method
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.3;
        form.add(new JLabel("Payment Method:*"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        paymentMethodCombo = new JComboBox<>(new String[]{"CASH", "CARD", "UPI"});
        form.add(paymentMethodCombo, gbc);

        // Row 2: Status (allows testing demo statuses)
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.3;
        form.add(new JLabel("Payment Status:*"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        paymentStatusCombo = new JComboBox<>(new String[]{"PAID", "PARTIAL", "PENDING", "FAILED", "REFUNDED"});
        form.add(paymentStatusCombo, gbc);

        // Row 3: Transaction Reference
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0.3;
        form.add(new JLabel("Txn Reference:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        JPanel txnPanel = new JPanel(new BorderLayout(4, 0));
        txnPanel.setOpaque(false);
        paymentTxnRefField = new JTextField();
        btnAutoTxnRef = new JButton("Auto-Gen");
        btnAutoTxnRef.setMargin(new Insets(2, 6, 2, 6));
        btnAutoTxnRef.addActionListener(e -> generateAutoTxnRef());
        txnPanel.add(paymentTxnRefField, BorderLayout.CENTER);
        txnPanel.add(btnAutoTxnRef, BorderLayout.EAST);
        form.add(txnPanel, gbc);

        // Row 4: Notes
        gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0.3;
        form.add(new JLabel("Notes:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        paymentNotesField = new JTextField();
        form.add(paymentNotesField, gbc);

        // Row 5: Action Buttons
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2;
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 8));
        btnPanel.setOpaque(false);

        btnQuickFullSettle = new JButton("⚡ Settle Full");
        btnQuickFullSettle.setToolTipText("Populates remaining balance and sets status to PAID");
        btnQuickFullSettle.addActionListener(e -> onQuickFullSettle());
        btnPanel.add(btnQuickFullSettle);

        btnClearPayment = new JButton("Clear");
        btnClearPayment.addActionListener(e -> clearPaymentForm());
        btnPanel.add(btnClearPayment);

        btnRecordPayment = new JButton("Submit Payment");
        btnRecordPayment.setBackground(NAVY);
        btnRecordPayment.setForeground(Color.WHITE);
        btnRecordPayment.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnRecordPayment.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnRecordPayment.addActionListener(e -> onRecordPayment());
        btnPanel.add(btnRecordPayment);

        form.add(btnPanel, gbc);

        return form;
    }

    private JPanel createPaymentHistoryPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(210, 215, 225), 1),
                " Payment History for Selected Booking ",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 13),
                NAVY
        ));
        panel.setBackground(Color.WHITE);

        String[] cols = {"ID", "Txn Ref", "Method", "Status", "Amount (₹)", "Date & Time", "Notes"};
        paymentHistoryModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        paymentHistoryTable = new JTable(paymentHistoryModel);
        paymentHistoryTable.setRowHeight(24);
        paymentHistoryTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        paymentHistoryTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        // Status column color renderer
        paymentHistoryTable.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object val, boolean isSel, boolean hasFocus, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(table, val, isSel, hasFocus, row, col);
                l.setHorizontalAlignment(CENTER);
                String st = val != null ? val.toString().toUpperCase() : "";
                if ("PAID".equals(st)) {
                    l.setForeground(SUCCESS_GREEN);
                    l.setFont(l.getFont().deriveFont(Font.BOLD));
                } else if ("PARTIAL".equals(st)) {
                    l.setForeground(WARNING_ORANGE);
                    l.setFont(l.getFont().deriveFont(Font.BOLD));
                } else if ("FAILED".equals(st)) {
                    l.setForeground(DANGER_RED);
                    l.setFont(l.getFont().deriveFont(Font.BOLD));
                } else if ("REFUNDED".equals(st)) {
                    l.setForeground(new Color(142, 68, 173));
                    l.setFont(l.getFont().deriveFont(Font.BOLD));
                } else if ("PENDING".equals(st)) {
                    l.setForeground(new Color(180, 140, 20));
                    l.setFont(l.getFont().deriveFont(Font.BOLD));
                }
                return l;
            }
        });

        JScrollPane scroll = new JScrollPane(paymentHistoryTable);
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    private void generateAutoTxnRef() {
        BookingSelectItem item = (BookingSelectItem) billingBookingCombo.getSelectedItem();
        String prefix = "TXN";
        if (item != null && item.booking != null) {
            prefix = "TXN-" + item.booking.getId();
        }
        paymentTxnRefField.setText(prefix + "-" + System.currentTimeMillis() % 1000000);
    }

    private void onQuickFullSettle() {
        BookingSelectItem item = (BookingSelectItem) billingBookingCombo.getSelectedItem();
        if (item == null || item.booking == null) {
            JOptionPane.showMessageDialog(this, "Please select a booking first.", "No Booking Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        BigDecimal balanceDue = getRemainingBalanceForBooking(item.booking.getId(), item.booking.getTotalAmount());
        if (balanceDue.compareTo(BigDecimal.ZERO) <= 0) {
            JOptionPane.showMessageDialog(this, "This booking is already fully settled!", "Settled", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        paymentAmountField.setText(balanceDue.toString());
        paymentStatusCombo.setSelectedItem("PAID");
        if (paymentTxnRefField.getText().isBlank()) {
            generateAutoTxnRef();
        }
        paymentNotesField.setText("Full settlement payment");
    }

    private void clearPaymentForm() {
        paymentAmountField.setText("");
        paymentTxnRefField.setText("");
        paymentNotesField.setText("");
        paymentMethodCombo.setSelectedIndex(0);
        paymentStatusCombo.setSelectedIndex(0);
    }

    private void onRecordPayment() {
        BookingSelectItem item = (BookingSelectItem) billingBookingCombo.getSelectedItem();
        if (item == null || item.booking == null) {
            JOptionPane.showMessageDialog(this, "Please select a booking to record payment against.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String amtStr = paymentAmountField.getText().trim();
        if (amtStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a payment amount.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        BigDecimal amount;
        try {
            amount = new BigDecimal(amtStr).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid monetary amount: '" + amtStr + "'.", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String method = (String) paymentMethodCombo.getSelectedItem();
        String status = (String) paymentStatusCombo.getSelectedItem();
        String txnRef = paymentTxnRefField.getText().trim();
        String notes = paymentNotesField.getText().trim();

        if (txnRef.isEmpty()) {
            txnRef = "TXN-" + item.booking.getId() + "-" + (System.currentTimeMillis() % 1000000);
        }

        // Validate overpayment client-side first
        BigDecimal balanceDue = getRemainingBalanceForBooking(item.booking.getId(), item.booking.getTotalAmount());
        if ("PAID".equalsIgnoreCase(status) || "PARTIAL".equalsIgnoreCase(status)) {
            try {
                BillCalculator.validatePaymentAmount(amount, balanceDue);
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Overpayment Prevented", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        Payment payment = new Payment(item.booking.getId(), amount, method, status, txnRef, notes);

        try {
            paymentDAO.recordPayment(payment);
            JOptionPane.showMessageDialog(this, 
                    "Payment of ₹" + amount + " recorded successfully!\nStatus: " + status + "\nTxn Ref: " + txnRef, 
                    "Payment Success", JOptionPane.INFORMATION_MESSAGE);

            clearPaymentForm();
            onBookingSelectedForBilling(); // Refresh balance & history

        } catch (PaymentDAO.OverpaymentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Overpayment Error", JOptionPane.WARNING_MESSAGE);
        } catch (PaymentDAO.DuplicateTransactionException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Duplicate Submission", JOptionPane.WARNING_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error saving payment: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private BigDecimal getRemainingBalanceForBooking(int bookingId, BigDecimal totalBill) {
        BigDecimal paid = paymentDAO.getTotalPaidForBooking(bookingId);
        return BillCalculator.calculateBalanceDue(totalBill, paid);
    }

    private void onBookingSelectedForBilling() {
        BookingSelectItem item = (BookingSelectItem) billingBookingCombo.getSelectedItem();
        if (item == null || item.booking == null) {
            resetBillSummary();
            paymentHistoryModel.setRowCount(0);
            return;
        }

        Booking b = item.booking;
        lblBillGuest.setText(b.getGuestName() != null ? b.getGuestName() : ("Guest #" + b.getGuestId()));
        lblBillRoom.setText("Room " + (b.getRoomNumber() != null ? b.getRoomNumber() : b.getRoomId()) 
                + " (" + (b.getRoomType() != null ? b.getRoomType() : "Standard") + ")");
        lblBillDates.setText(b.getCheckInDate() + " to " + b.getCheckOutDate());
        
        long nights = b.getNumberOfNights();
        lblBillNights.setText(nights + " night" + (nights > 1 ? "s" : ""));

        BigDecimal total = b.getTotalAmount() != null ? b.getTotalAmount() : BigDecimal.ZERO;
        lblBillTotal.setText("₹" + total.setScale(2, RoundingMode.HALF_UP));

        // Load payments from DAO
        List<Payment> payments = paymentDAO.findPaymentsByBookingId(b.getId());
        BigDecimal totalPaid = BillCalculator.calculateTotalPaid(payments);
        BigDecimal balance = BillCalculator.calculateBalanceDue(total, totalPaid);

        lblBillPaid.setText("₹" + totalPaid.setScale(2, RoundingMode.HALF_UP));
        lblBillBalance.setText("₹" + balance.setScale(2, RoundingMode.HALF_UP));

        if (balance.compareTo(BigDecimal.ZERO) <= 0) {
            lblBillStatusBadge.setText("PAID (SETTLED)");
            lblBillStatusBadge.setForeground(SUCCESS_GREEN);
            lblBillBalance.setForeground(SUCCESS_GREEN);
        } else if (totalPaid.compareTo(BigDecimal.ZERO) > 0) {
            lblBillStatusBadge.setText("PARTIALLY PAID");
            lblBillStatusBadge.setForeground(WARNING_ORANGE);
            lblBillBalance.setForeground(DANGER_RED);
        } else {
            lblBillStatusBadge.setText("UNPAID");
            lblBillStatusBadge.setForeground(DANGER_RED);
            lblBillBalance.setForeground(DANGER_RED);
        }

        // Pre-fill amount field with remaining balance if > 0
        if (balance.compareTo(BigDecimal.ZERO) > 0) {
            paymentAmountField.setText(balance.toString());
        } else {
            paymentAmountField.setText("");
        }
        generateAutoTxnRef();

        // Populate payment history table
        paymentHistoryModel.setRowCount(0);
        for (Payment p : payments) {
            paymentHistoryModel.addRow(new Object[]{
                    p.getId(),
                    p.getTransactionReference(),
                    p.getPaymentMethod(),
                    p.getPaymentStatus(),
                    "₹" + p.getAmount(),
                    p.getPaymentDate() != null ? p.getPaymentDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "-",
                    p.getNotes() != null ? p.getNotes() : ""
            });
        }
    }

    private void resetBillSummary() {
        lblBillGuest.setText("-");
        lblBillRoom.setText("-");
        lblBillDates.setText("-");
        lblBillNights.setText("-");
        lblBillTotal.setText("₹0.00");
        lblBillPaid.setText("₹0.00");
        lblBillBalance.setText("₹0.00");
        lblBillStatusBadge.setText("-");
    }

    // =========================================================================
    // TAB 2: INVOICES & PRINTING
    // =========================================================================

    private JPanel createInvoicesTab() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));

        // Top Action Bar
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        topBar.setOpaque(false);
        topBar.add(new JLabel("Booking:"));

        invoiceBookingCombo = new JComboBox<>();
        invoiceBookingCombo.setPreferredSize(new Dimension(340, 30));
        topBar.add(invoiceBookingCombo);

        btnGenerateInvoice = new JButton("📄 Generate / Update Invoice");
        btnGenerateInvoice.setBackground(NAVY);
        btnGenerateInvoice.setForeground(Color.WHITE);
        btnGenerateInvoice.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnGenerateInvoice.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnGenerateInvoice.addActionListener(e -> onGenerateInvoice());
        topBar.add(btnGenerateInvoice);

        btnPrintInvoice = new JButton("🖨️ Print / Export PDF");
        btnPrintInvoice.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnPrintInvoice.addActionListener(e -> onPrintInvoice());
        topBar.add(btnPrintInvoice);

        btnExportHtml = new JButton("💾 Save HTML");
        btnExportHtml.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnExportHtml.addActionListener(e -> onExportHtml());
        topBar.add(btnExportHtml);

        panel.add(topBar, BorderLayout.NORTH);

        // Center Split: Left = Invoices List, Right = Printable Preview
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBorder(BorderFactory.createTitledBorder(" Generated Invoices "));
        leftPanel.setOpaque(false);

        String[] cols = {"Invoice #", "Booking Ref", "Guest", "Total (₹)", "Paid (₹)", "Balance (₹)"};
        invoiceListModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        invoiceListTable = new JTable(invoiceListModel);
        invoiceListTable.setRowHeight(24);
        invoiceListTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = invoiceListTable.getSelectedRow();
                if (row >= 0) {
                    String invNum = (String) invoiceListTable.getValueAt(row, 0);
                    displayInvoicePreview(invNum);
                }
            }
        });
        leftPanel.add(new JScrollPane(invoiceListTable), BorderLayout.CENTER);

        // Right Preview Panel
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBorder(BorderFactory.createTitledBorder(" Invoice Document Preview "));
        rightPanel.setOpaque(false);

        invoicePreviewPane = new JEditorPane();
        invoicePreviewPane.setContentType("text/html");
        invoicePreviewPane.setEditable(false);
        invoicePreviewPane.setText(buildEmptyInvoiceHtml());

        rightPanel.add(new JScrollPane(invoicePreviewPane), BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        split.setDividerLocation(380);
        split.setResizeWeight(0.35);
        split.setBorder(null);

        panel.add(split, BorderLayout.CENTER);
        return panel;
    }

    private void onGenerateInvoice() {
        BookingSelectItem item = (BookingSelectItem) invoiceBookingCombo.getSelectedItem();
        if (item == null || item.booking == null) {
            JOptionPane.showMessageDialog(this, "Please select a booking to generate invoice.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            Invoice inv = invoiceDAO.generateInvoice(item.booking.getId(), "Thank you for staying at Grand Horizon Hotel!");
            JOptionPane.showMessageDialog(this, 
                    "Invoice " + inv.getInvoiceNumber() + " generated successfully!\nTotal: ₹" + inv.getTotalAmount() 
                    + " | Paid: ₹" + inv.getPaidAmount() + " | Balance: ₹" + inv.getBalanceDue(),
                    "Invoice Generated", JOptionPane.INFORMATION_MESSAGE);

            loadAllInvoices();
            displayInvoice(inv);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error generating invoice: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadAllInvoices() {
        invoiceListModel.setRowCount(0);
        List<Invoice> list = invoiceDAO.findAllInvoices();
        for (Invoice inv : list) {
            invoiceListModel.addRow(new Object[]{
                    inv.getInvoiceNumber(),
                    inv.getBookingReference(),
                    inv.getGuestName(),
                    "₹" + inv.getTotalAmount(),
                    "₹" + inv.getPaidAmount(),
                    "₹" + inv.getBalanceDue()
            });
        }
    }

    private void displayInvoicePreview(String invoiceNumber) {
        Optional<Invoice> opt = invoiceDAO.findInvoiceByNumber(invoiceNumber);
        if (opt.isPresent()) {
            displayInvoice(opt.get());
        }
    }

    private void displayInvoice(Invoice inv) {
        this.currentPreviewInvoice = inv;
        invoicePreviewPane.setText(buildInvoiceHtml(inv));
        invoicePreviewPane.setCaretPosition(0);
    }

    private void onPrintInvoice() {
        if (currentPreviewInvoice == null) {
            JOptionPane.showMessageDialog(this, "Please select or generate an invoice to print.", "No Invoice", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            boolean complete = invoicePreviewPane.print(null, null, true, null, null, true);
            if (complete) {
                JOptionPane.showMessageDialog(this, "Printing completed or sent to printer.", "Print Job Done", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (PrinterException e) {
            JOptionPane.showMessageDialog(this, "Printing error: " + e.getMessage(), "Print Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onExportHtml() {
        if (currentPreviewInvoice == null) {
            JOptionPane.showMessageDialog(this, "Please select or generate an invoice to export.", "No Invoice", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File(currentPreviewInvoice.getInvoiceNumber() + ".html"));
        int ret = fileChooser.showSaveDialog(this);
        if (ret == JFileChooser.APPROVE_OPTION) {
            try (FileWriter fw = new FileWriter(fileChooser.getSelectedFile())) {
                fw.write(buildInvoiceHtml(currentPreviewInvoice));
                JOptionPane.showMessageDialog(this, "Invoice exported to " + fileChooser.getSelectedFile().getName(), "Export Successful", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Failed to export: " + e.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private String buildEmptyInvoiceHtml() {
        return "<html><body style='font-family:sans-serif; text-align:center; padding:50px; color:#888;'>"
                + "<h3>No Invoice Selected</h3>"
                + "<p>Select a booking and click <b>'Generate Invoice'</b> or pick an invoice from the table on the left to preview.</p>"
                + "</body></html>";
    }

    private String buildInvoiceHtml(Invoice inv) {
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

        return "<html><body style='font-family:Segoe UI, sans-serif; margin:20px; color:#2c3e50; font-size:12px;'>"
                + "<table width='100%' style='border-bottom:2px solid #182b49; padding-bottom:10px;'>"
                + "<tr>"
                + "  <td>"
                + "    <h2 style='margin:0; color:#182b49;'>GRAND HORIZON HOTEL</h2>"
                + "    <span style='color:#7f8c8d;'>Marine Drive, Kochi, Kerala - 682011</span><br>"
                + "    <span style='color:#7f8c8d;'>Phone: +91 484 2233445 | Email: billing@grandhorizon.com</span>"
                + "  </td>"
                + "  <td align='right' valign='top'>"
                + "    <h3 style='margin:0; color:#182b49;'>INVOICE</h3>"
                + "    <b>Invoice #:</b> " + inv.getInvoiceNumber() + "<br>"
                + "    <b>Date:</b> " + issueDateStr + "<br>"
                + "    <span style='background:" + badgeColor + "; color:white; padding:3px 8px; font-weight:bold; border-radius:3px; font-size:11px;'>" + badgeText + "</span>"
                + "  </td>"
                + "</tr>"
                + "</table>"
                + "<br>"
                + "<table width='100%'>"
                + "<tr>"
                + "  <td width='50%' valign='top' style='background:#f8f9fa; padding:10px; border-radius:4px;'>"
                + "    <b>BILLED TO:</b><br>"
                + "    <span style='font-size:13px; font-weight:bold; color:#182b49;'>" + (inv.getGuestName() != null ? inv.getGuestName() : "-") + "</span><br>"
                + "    Phone: " + (inv.getGuestPhone() != null ? inv.getGuestPhone() : "-") + "<br>"
                + "    Email: " + (inv.getGuestEmail() != null ? inv.getGuestEmail() : "-") + "<br>"
                + "    Address: " + (inv.getGuestAddress() != null ? inv.getGuestAddress() : "-")
                + "  </td>"
                + "  <td width='50%' valign='top' style='background:#f8f9fa; padding:10px; border-radius:4px; margin-left:10px;'>"
                + "    <b>RESERVATION DETAILS:</b><br>"
                + "    <b>Booking Reference:</b> " + (inv.getBookingReference() != null ? inv.getBookingReference() : ("#" + inv.getBookingId())) + "<br>"
                + "    <b>Room:</b> " + (inv.getRoomNumber() != null ? inv.getRoomNumber() : "-") + " (" + (inv.getRoomType() != null ? inv.getRoomType() : "Standard") + ")<br>"
                + "    <b>Check-in:</b> " + inv.getCheckInDate() + "<br>"
                + "    <b>Check-out:</b> " + inv.getCheckOutDate() + " (" + inv.getNumberOfNights() + " nights)"
                + "  </td>"
                + "</tr>"
                + "</table>"
                + "<br>"
                + "<table width='100%' border='1' cellspacing='0' cellpadding='6' style='border-collapse:collapse; border-color:#e0e0e0;'>"
                + "<tr style='background:#182b49; color:white; font-weight:bold;'>"
                + "  <th align='left'>Description</th>"
                + "  <th align='center'>Rate / Night</th>"
                + "  <th align='center'>Nights</th>"
                + "  <th align='right'>Amount (₹)</th>"
                + "</tr>"
                + "<tr>"
                + "  <td>Room Accommodation (" + inv.getRoomType() + " Room " + inv.getRoomNumber() + ")</td>"
                + "  <td align='center'>₹" + (inv.getRatePerNight() != null ? inv.getRatePerNight() : "0.00") + "</td>"
                + "  <td align='center'>" + inv.getNumberOfNights() + "</td>"
                + "  <td align='right'>₹" + (inv.getTotalRoomCharges() != null ? inv.getTotalRoomCharges() : inv.getTotalAmount()) + "</td>"
                + "</tr>"
                + "<tr style='background:#fafafa;'>"
                + "  <td colspan='3' align='right'><b>Total Charges:</b></td>"
                + "  <td align='right'><b>₹" + inv.getTotalAmount() + "</b></td>"
                + "</tr>"
                + "<tr>"
                + "  <td colspan='3' align='right' style='color:#27ae60;'><b>Amount Paid:</b></td>"
                + "  <td align='right' style='color:#27ae60;'><b>₹" + inv.getPaidAmount() + "</b></td>"
                + "</tr>"
                + "<tr style='background:#fff3cd;'>"
                + "  <td colspan='3' align='right' style='color:#c0392b;'><b>Balance Due:</b></td>"
                + "  <td align='right' style='color:#c0392b; font-size:13px;'><b>₹" + inv.getBalanceDue() + "</b></td>"
                + "</tr>"
                + "</table>"
                + "<br>"
                + "<div style='font-size:11px; color:#7f8c8d; border-top:1px dashed #ccc; padding-top:8px;'>"
                + "<b>Terms & Notes:</b> " + (inv.getNotes() != null ? inv.getNotes() : "Thank you for your business.") + "<br>"
                + "All payments are subject to hotel reservation terms. Computer generated invoice."
                + "</div>"
                + "</body></html>";
    }

    // =========================================================================
    // TAB 3: REPORTS (Booking, Occupancy, Revenue)
    // =========================================================================

    private JPanel createReportsTab() {
        JTabbedPane reportTabs = new JTabbedPane(JTabbedPane.TOP);
        reportTabs.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        reportTabs.addTab("📅 Booking Report", createBookingReportSubTab());
        reportTabs.addTab("🏨 Occupancy Report", createOccupancyReportSubTab());
        reportTabs.addTab("💰 Revenue Analytics", createRevenueReportSubTab());

        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Color.WHITE);
        p.add(reportTabs, BorderLayout.CENTER);
        return p;
    }

    // 3a. Booking Report
    private JPanel createBookingReportSubTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Filter Bar
        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        filterBar.setOpaque(false);

        filterBar.add(new JLabel("From:"));
        bkgRepFromDate = new JTextField(LocalDate.now().minusDays(30).toString(), 8);
        filterBar.add(bkgRepFromDate);

        filterBar.add(new JLabel("To:"));
        bkgRepToDate = new JTextField(LocalDate.now().plusDays(30).toString(), 8);
        filterBar.add(bkgRepToDate);

        filterBar.add(new JLabel("Status:"));
        bkgRepStatusCombo = new JComboBox<>(new String[]{"ALL", "CONFIRMED", "CHECKED_IN", "CHECKED_OUT", "CANCELLED"});
        filterBar.add(bkgRepStatusCombo);

        btnRunBookingReport = new JButton("Generate Report");
        btnRunBookingReport.setBackground(NAVY);
        btnRunBookingReport.setForeground(Color.WHITE);
        btnRunBookingReport.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnRunBookingReport.addActionListener(e -> onRunBookingReport());
        filterBar.add(btnRunBookingReport);

        lblBookingRepStats = new JLabel("Total Bookings: 0 | Total Value: ₹0.00");
        lblBookingRepStats.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblBookingRepStats.setForeground(NAVY);
        filterBar.add(Box.createHorizontalStrut(15));
        filterBar.add(lblBookingRepStats);

        panel.add(filterBar, BorderLayout.NORTH);

        // Table
        String[] cols = {"ID", "Reference", "Guest Name", "Phone", "Room", "Type", "Check-In", "Check-Out", "Nights", "Total (₹)", "Status"};
        bookingReportModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        bookingReportTable = new JTable(bookingReportModel);
        bookingReportTable.setRowHeight(24);
        bookingReportTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        panel.add(new JScrollPane(bookingReportTable), BorderLayout.CENTER);

        return panel;
    }

    private void onRunBookingReport() {
        LocalDate from = parseDateSafe(bkgRepFromDate.getText());
        LocalDate to = parseDateSafe(bkgRepToDate.getText());
        String status = (String) bkgRepStatusCombo.getSelectedItem();

        List<BookingReportItem> list = reportDAO.generateBookingReport(from, to, status);
        bookingReportModel.setRowCount(0);

        BigDecimal sumTotal = BigDecimal.ZERO;
        for (BookingReportItem item : list) {
            bookingReportModel.addRow(new Object[]{
                    item.getBookingId(),
                    item.getBookingReference(),
                    item.getGuestName(),
                    item.getGuestPhone(),
                    item.getRoomNumber(),
                    item.getRoomType(),
                    item.getCheckInDate(),
                    item.getCheckOutDate(),
                    item.getNights(),
                    "₹" + item.getTotalAmount(),
                    item.getBookingStatus()
            });
            if (item.getTotalAmount() != null) {
                sumTotal = sumTotal.add(item.getTotalAmount());
            }
        }
        lblBookingRepStats.setText("Total Bookings: " + list.size() + " | Total Value: ₹" + sumTotal.setScale(2, RoundingMode.HALF_UP));
    }

    // 3b. Occupancy Report
    private JPanel createOccupancyReportSubTab() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Filter Bar
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        topBar.setOpaque(false);

        topBar.add(new JLabel("Target Date:"));
        occRepDateField = new JTextField(LocalDate.now().toString(), 10);
        topBar.add(occRepDateField);

        btnOccToday = new JButton("Today");
        btnOccToday.addActionListener(e -> occRepDateField.setText(LocalDate.now().toString()));
        topBar.add(btnOccToday);

        btnRunOccupancyReport = new JButton("Calculate Occupancy");
        btnRunOccupancyReport.setBackground(NAVY);
        btnRunOccupancyReport.setForeground(Color.WHITE);
        btnRunOccupancyReport.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnRunOccupancyReport.addActionListener(e -> onRunOccupancyReport());
        topBar.add(btnRunOccupancyReport);

        panel.add(topBar, BorderLayout.NORTH);

        // Center Content: Cards Grid + Occupied Rooms Table
        JPanel center = new JPanel(new BorderLayout(10, 10));
        center.setOpaque(false);

        // KPI Cards Grid
        JPanel kpiGrid = new JPanel(new GridLayout(1, 5, 12, 12));
        kpiGrid.setOpaque(false);
        kpiGrid.setPreferredSize(new Dimension(0, 90));

        lblOccPercent = new JLabel("0.00%", SwingConstants.CENTER);
        lblOccPercent.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblOccPercent.setForeground(new Color(41, 128, 185));

        lblOccTotalRooms = new JLabel("0", SwingConstants.CENTER);
        lblOccTotalRooms.setFont(new Font("Segoe UI", Font.BOLD, 20));

        lblOccOccupiedRooms = new JLabel("0", SwingConstants.CENTER);
        lblOccOccupiedRooms.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblOccOccupiedRooms.setForeground(DANGER_RED);

        lblOccAvailableRooms = new JLabel("0", SwingConstants.CENTER);
        lblOccAvailableRooms.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblOccAvailableRooms.setForeground(SUCCESS_GREEN);

        lblOccMaintRooms = new JLabel("0", SwingConstants.CENTER);
        lblOccMaintRooms.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblOccMaintRooms.setForeground(Color.GRAY);

        kpiGrid.add(createKpiCard("Occupancy Rate", lblOccPercent));
        kpiGrid.add(createKpiCard("Total Rooms", lblOccTotalRooms));
        kpiGrid.add(createKpiCard("Occupied", lblOccOccupiedRooms));
        kpiGrid.add(createKpiCard("Available", lblOccAvailableRooms));
        kpiGrid.add(createKpiCard("Maintenance", lblOccMaintRooms));

        center.add(kpiGrid, BorderLayout.NORTH);

        // Occupied Rooms Details Table
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBorder(BorderFactory.createTitledBorder(" Occupied Room Details for Selected Date "));
        tablePanel.setOpaque(false);

        String[] cols = {"Room #", "Room Type", "Booking Reference", "Guest Name", "Check-In", "Check-Out"};
        occDetailsModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        occDetailsTable = new JTable(occDetailsModel);
        occDetailsTable.setRowHeight(24);
        tablePanel.add(new JScrollPane(occDetailsTable), BorderLayout.CENTER);

        center.add(tablePanel, BorderLayout.CENTER);

        panel.add(center, BorderLayout.CENTER);
        return panel;
    }

    private void onRunOccupancyReport() {
        LocalDate date = parseDateSafe(occRepDateField.getText());
        if (date == null) date = LocalDate.now();

        OccupancyReport rep = reportDAO.generateOccupancyReport(date);

        lblOccPercent.setText(rep.getOccupancyPercentage() + "%");
        lblOccTotalRooms.setText(String.valueOf(rep.getTotalRooms()));
        lblOccOccupiedRooms.setText(String.valueOf(rep.getOccupiedRooms()));
        lblOccAvailableRooms.setText(String.valueOf(rep.getAvailableRooms()));
        lblOccMaintRooms.setText(String.valueOf(rep.getMaintenanceRooms()));

        occDetailsModel.setRowCount(0);
        for (OccupancyReport.OccupiedRoomDetail d : rep.getOccupiedDetails()) {
            occDetailsModel.addRow(new Object[]{
                    d.getRoomNumber(),
                    d.getRoomType(),
                    d.getBookingReference(),
                    d.getGuestName(),
                    d.getCheckInDate(),
                    d.getCheckOutDate()
            });
        }
    }

    // 3c. Revenue Report
    private JPanel createRevenueReportSubTab() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Filter Bar
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        topBar.setOpaque(false);

        topBar.add(new JLabel("From:"));
        revRepFromDate = new JTextField(LocalDate.now().minusDays(30).toString(), 8);
        topBar.add(revRepFromDate);

        topBar.add(new JLabel("To:"));
        revRepToDate = new JTextField(LocalDate.now().toString(), 8);
        topBar.add(revRepToDate);

        btnRunRevenueReport = new JButton("Generate Revenue Report");
        btnRunRevenueReport.setBackground(NAVY);
        btnRunRevenueReport.setForeground(Color.WHITE);
        btnRunRevenueReport.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnRunRevenueReport.addActionListener(e -> onRunRevenueReport());
        topBar.add(btnRunRevenueReport);

        panel.add(topBar, BorderLayout.NORTH);

        // Center Content: Financial KPIs + Daily Breakdown Table
        JPanel center = new JPanel(new BorderLayout(10, 10));
        center.setOpaque(false);

        JPanel kpiGrid = new JPanel(new GridLayout(1, 4, 12, 12));
        kpiGrid.setOpaque(false);
        kpiGrid.setPreferredSize(new Dimension(0, 90));

        lblRevTotal = new JLabel("₹0.00", SwingConstants.CENTER);
        lblRevTotal.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblRevTotal.setForeground(SUCCESS_GREEN);

        lblRevRefunds = new JLabel("₹0.00", SwingConstants.CENTER);
        lblRevRefunds.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblRevRefunds.setForeground(DANGER_RED);

        lblRevNet = new JLabel("₹0.00", SwingConstants.CENTER);
        lblRevNet.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblRevNet.setForeground(NAVY);

        lblRevCount = new JLabel("0", SwingConstants.CENTER);
        lblRevCount.setFont(new Font("Segoe UI", Font.BOLD, 20));

        kpiGrid.add(createKpiCard("Gross Collections (PAID)", lblRevTotal));
        kpiGrid.add(createKpiCard("Refunds Subtracted", lblRevRefunds));
        kpiGrid.add(createKpiCard("Net Revenue", lblRevNet));
        kpiGrid.add(createKpiCard("Successful Txns", lblRevCount));

        center.add(kpiGrid, BorderLayout.NORTH);

        // Daily Breakdown Table
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBorder(BorderFactory.createTitledBorder(" Date-Wise Collections & Payment Method Breakdown "));
        tablePanel.setOpaque(false);

        String[] cols = {"Date", "Cash (₹)", "Card (₹)", "UPI (₹)", "Refunds (₹)", "Net Daily Total (₹)", "Transactions"};
        dailyRevenueModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        dailyRevenueTable = new JTable(dailyRevenueModel);
        dailyRevenueTable.setRowHeight(24);
        dailyRevenueTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        tablePanel.add(new JScrollPane(dailyRevenueTable), BorderLayout.CENTER);

        center.add(tablePanel, BorderLayout.CENTER);

        panel.add(center, BorderLayout.CENTER);
        return panel;
    }

    private void onRunRevenueReport() {
        LocalDate from = parseDateSafe(revRepFromDate.getText());
        LocalDate to = parseDateSafe(revRepToDate.getText());
        if (from == null) from = LocalDate.now().minusDays(30);
        if (to == null) to = LocalDate.now();

        RevenueReport rep = reportDAO.generateRevenueReport(from, to);

        lblRevTotal.setText("₹" + rep.getTotalRevenue().setScale(2, RoundingMode.HALF_UP));
        lblRevRefunds.setText("₹" + rep.getTotalRefunds().setScale(2, RoundingMode.HALF_UP));
        lblRevNet.setText("₹" + rep.getNetRevenue().setScale(2, RoundingMode.HALF_UP));
        lblRevCount.setText(String.valueOf(rep.getPaymentCount()));

        dailyRevenueModel.setRowCount(0);
        for (RevenueReport.DailyRevenueItem d : rep.getDailyBreakdown()) {
            dailyRevenueModel.addRow(new Object[]{
                    d.getDate(),
                    "₹" + d.getCashAmount(),
                    "₹" + d.getCardAmount(),
                    "₹" + d.getUpiAmount(),
                    "₹" + d.getRefundAmount(),
                    "₹" + d.getNetAmount(),
                    d.getTransactionCount()
            });
        }
    }

    private JPanel createKpiCard(String title, JLabel valLabel) {
        JPanel card = new JPanel(new BorderLayout(4, 4));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(225, 230, 238), 1),
                new EmptyBorder(8, 10, 8, 10)
        ));
        card.setBackground(new Color(250, 252, 255));

        JLabel titleLbl = new JLabel(title, SwingConstants.CENTER);
        titleLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        titleLbl.setForeground(Color.GRAY);

        card.add(titleLbl, BorderLayout.NORTH);
        card.add(valLabel, BorderLayout.CENTER);
        return card;
    }

    private LocalDate parseDateSafe(String text) {
        if (text == null || text.isBlank()) return null;
        try {
            return LocalDate.parse(text.trim(), DATE_FMT);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    // =========================================================================
    // HELPERS: DATA REFRESH & SELECTORS
    // =========================================================================

    public void refreshBookingSelectors() {
        billingBookingCombo.removeAllItems();
        invoiceBookingCombo.removeAllItems();

        try {
            List<Booking> bookings = bookingDAO.findAllBookings();
            for (Booking b : bookings) {
                BookingSelectItem item = new BookingSelectItem(b);
                billingBookingCombo.addItem(item);
                invoiceBookingCombo.addItem(item);
            }
        } catch (Exception e) {
            System.err.println("[BillingAndReportsPanel] Notice: MySQL offline or unable to load bookings: " + e.getMessage());
        }

        if (billingBookingCombo.getItemCount() > 0) {
            billingBookingCombo.setSelectedIndex(0);
        } else {
            resetBillSummary();
        }
    }

    /**
     * Item wrapper for JComboBox display
     */
    public static class BookingSelectItem {
        public final Booking booking;

        public BookingSelectItem(Booking booking) {
            this.booking = booking;
        }

        @Override
        public String toString() {
            if (booking == null) return "None";
            String ref = booking.getBookingReference() != null ? booking.getBookingReference() : ("ID:" + booking.getId());
            String gName = booking.getGuestName() != null ? booking.getGuestName() : "Guest";
            String rNum = booking.getRoomNumber() != null ? ("Room " + booking.getRoomNumber()) : "";
            return ref + " - " + gName + " (" + rNum + ") - ₹" + booking.getTotalAmount();
        }
    }
}
