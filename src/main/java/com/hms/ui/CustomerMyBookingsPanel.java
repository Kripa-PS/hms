package com.hms.ui;

import com.hms.dao.BookingDAO;
import com.hms.dao.BookingDAOImpl;
import com.hms.dao.InvoiceDAO;
import com.hms.dao.InvoiceDAOImpl;
import com.hms.model.Booking;
import com.hms.model.Invoice;
import com.hms.util.InvoiceRenderer;
import com.hms.util.UserSession;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * CustomerMyBookingsPanel
 * 
 * Displays all bookings for the currently authenticated customer.
 * Enforces customer data isolation: guests only see their own reservations.
 * Allows viewing invoices and cancelling active bookings.
 */
public class CustomerMyBookingsPanel extends JPanel {

    private final BookingDAO bookingDAO;
    private final InvoiceDAO invoiceDAO;

    private JTable bookingsTable;
    private DefaultTableModel tableModel;
    private JButton btnViewInvoice;
    private JButton btnCancelBooking;
    private JButton btnRefresh;
    private JLabel lblStatus;

    private List<Booking> currentBookings = new ArrayList<>();

    public CustomerMyBookingsPanel() {
        this.bookingDAO = new BookingDAOImpl();
        this.invoiceDAO = new InvoiceDAOImpl();

        setLayout(new BorderLayout(15, 15));
        setBorder(new EmptyBorder(15, 20, 15, 20));
        setBackground(Color.WHITE);

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JLabel heading = new JLabel("My Reservations");
        heading.setFont(new Font("Segoe UI", Font.BOLD, 20));
        heading.setForeground(new Color(24, 43, 73));
        headerPanel.add(heading, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);

        btnRefresh = new JButton("Refresh");
        btnRefresh.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnRefresh.addActionListener(e -> loadBookingsAsync());
        actions.add(btnRefresh);

        btnViewInvoice = new JButton("View Invoice");
        btnViewInvoice.setEnabled(false);
        btnViewInvoice.setBackground(new Color(24, 43, 73));
        btnViewInvoice.setForeground(Color.WHITE);
        btnViewInvoice.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnViewInvoice.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnViewInvoice.addActionListener(e -> onViewInvoice());
        actions.add(btnViewInvoice);

        btnCancelBooking = new JButton("Cancel Booking");
        btnCancelBooking.setEnabled(false);
        btnCancelBooking.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCancelBooking.addActionListener(e -> onCancelBooking());
        actions.add(btnCancelBooking);

        headerPanel.add(actions, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        // Bookings Table
        String[] cols = {"Reference #", "Room", "Room Type", "Check-In", "Check-Out", "Nights", "Total (Rs.)", "Status"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        bookingsTable = new JTable(tableModel);
        bookingsTable.setRowHeight(28);
        bookingsTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        bookingsTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        bookingsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        bookingsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = bookingsTable.getSelectedRow();
                boolean selected = (row >= 0 && row < currentBookings.size());
                btnViewInvoice.setEnabled(selected);

                if (selected) {
                    Booking b = currentBookings.get(row);
                    boolean canCancel = "CONFIRMED".equalsIgnoreCase(b.getStatus())
                            && b.getCheckInDate() != null
                            && !b.getCheckInDate().isBefore(LocalDate.now());
                    btnCancelBooking.setEnabled(canCancel);
                } else {
                    btnCancelBooking.setEnabled(false);
                }
            }
        });

        add(new JScrollPane(bookingsTable), BorderLayout.CENTER);

        // Status bar
        lblStatus = new JLabel("Loading your bookings...");
        lblStatus.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblStatus.setForeground(Color.GRAY);
        add(lblStatus, BorderLayout.SOUTH);

        // Initial load
        loadBookingsAsync();
    }

    public void loadBookingsAsync() {
        Integer guestId = UserSession.getCurrentGuestId();
        if (guestId == null || guestId <= 0) {
            lblStatus.setText("Guest record not linked to this customer account.");
            return;
        }

        lblStatus.setText("Fetching reservations...");
        btnRefresh.setEnabled(false);

        SwingWorker<List<Booking>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Booking> doInBackground() {
                return bookingDAO.findBookingsByGuestId(guestId);
            }

            @Override
            protected void done() {
                btnRefresh.setEnabled(true);
                try {
                    currentBookings = get();
                    tableModel.setRowCount(0);

                    for (Booking b : currentBookings) {
                        tableModel.addRow(new Object[]{
                                b.getBookingReference(),
                                (b.getRoomNumber() != null ? b.getRoomNumber() : ("#" + b.getRoomId())),
                                (b.getRoomType() != null ? b.getRoomType() : "Standard"),
                                b.getCheckInDate(),
                                b.getCheckOutDate(),
                                b.getNumberOfNights(),
                                (b.getTotalAmount() != null ? ("Rs. " + b.getTotalAmount().setScale(2, RoundingMode.HALF_UP)) : "Rs. 0.00"),
                                b.getStatus()
                        });
                    }

                    if (currentBookings.isEmpty()) {
                        lblStatus.setText("No reservations found. Use 'Search & Book Rooms' to make your first booking!");
                    } else {
                        lblStatus.setText("Showing " + currentBookings.size() + " reservation(s).");
                    }

                    btnViewInvoice.setEnabled(false);
                    btnCancelBooking.setEnabled(false);

                } catch (Exception ex) {
                    lblStatus.setText("Failed to load reservations: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void onViewInvoice() {
        int row = bookingsTable.getSelectedRow();
        if (row < 0 || row >= currentBookings.size()) return;

        Booking b = currentBookings.get(row);
        btnViewInvoice.setEnabled(false);

        SwingWorker<Optional<Invoice>, Void> worker = new SwingWorker<>() {
            @Override
            protected Optional<Invoice> doInBackground() {
                return invoiceDAO.findInvoiceByBookingId(b.getId());
            }

            @Override
            protected void done() {
                btnViewInvoice.setEnabled(true);
                try {
                    Optional<Invoice> invOpt = get();
                    if (invOpt.isPresent()) {
                        InvoiceRenderer.showInvoiceDialog(CustomerMyBookingsPanel.this, invOpt.get());
                    } else {
                        JOptionPane.showMessageDialog(CustomerMyBookingsPanel.this,
                                "No invoice has been generated for booking " + b.getBookingReference() + " yet.",
                                "Invoice Not Available", JOptionPane.INFORMATION_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(CustomerMyBookingsPanel.this,
                            "Error retrieving invoice: " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void onCancelBooking() {
        int row = bookingsTable.getSelectedRow();
        if (row < 0 || row >= currentBookings.size()) return;

        Booking b = currentBookings.get(row);
        int res = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to cancel reservation " + b.getBookingReference() + "?\n"
                        + "Room " + b.getRoomNumber() + " (" + b.getCheckInDate() + " to " + b.getCheckOutDate() + ")\n\n"
                        + "This action will release the room for other guests.",
                "Confirm Cancellation", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (res == JOptionPane.YES_OPTION) {
            SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
                @Override
                protected Boolean doInBackground() {
                    return bookingDAO.cancelBooking(b.getId());
                }

                @Override
                protected void done() {
                    try {
                        boolean ok = get();
                        if (ok) {
                            JOptionPane.showMessageDialog(CustomerMyBookingsPanel.this,
                                    "Reservation " + b.getBookingReference() + " has been cancelled.",
                                    "Cancelled", JOptionPane.INFORMATION_MESSAGE);
                            loadBookingsAsync();
                        } else {
                            JOptionPane.showMessageDialog(CustomerMyBookingsPanel.this,
                                    "Unable to cancel reservation. Please contact hotel front desk.",
                                    "Cancellation Error", JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(CustomerMyBookingsPanel.this,
                                "Error cancelling reservation: " + ex.getMessage(),
                                "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            };
            worker.execute();
        }
    }
}
