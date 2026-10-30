package com.hms.ui;

import com.hms.dao.BookingDAO;
import com.hms.dao.BookingDAOImpl;
import com.hms.dao.GuestDAO;
import com.hms.dao.GuestDAOImpl;
import com.hms.dao.RoomDAO;
import com.hms.dao.RoomDAOImpl;
import com.hms.dao.RoomHoldDAO;
import com.hms.dao.RoomHoldDAOImpl;
import com.hms.model.Booking;
import com.hms.model.Guest;
import com.hms.model.Room;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * BookingManagementPanel
 * 
 * Java Swing GUI for staff Booking Management.
 * Features:
 * - Guest and Room selectors loaded from MySQL
 * - Date validation (checkOut > checkIn, no past dates)
 * - Dynamic nights & total price calculation
 * - Real-time overlap prevention checks
 * - Booking confirmation with auto-generated reference number
 * - Booking cancellation releasing room availability
 * - Search by reference number and table refresh
 */
public class BookingManagementPanel extends JPanel {

    private final BookingDAO bookingDAO;
    private final GuestDAO guestDAO;
    private final RoomDAO roomDAO;
    private final RoomHoldDAO roomHoldDAO;

    // Form inputs
    private JComboBox<GuestItem> guestCombo;
    private JComboBox<RoomItem> roomCombo;
    private JTextField checkInField;
    private JTextField checkOutField;
    private JSpinner guestCountSpinner;
    private JLabel lblNights;
    private JLabel lblTotalAmount;

    // Buttons
    private JButton btnConfirmBooking;
    private JButton btnCheckAvailability;
    private JButton btnClear;
    private JButton btnCancelBooking;

    // Table
    private JTable bookingTable;
    private DefaultTableModel tableModel;
    private JTextField searchRefField;

    public BookingManagementPanel() {
        this.bookingDAO = new BookingDAOImpl();
        this.guestDAO = new GuestDAOImpl();
        this.roomDAO = new RoomDAOImpl();
        this.roomHoldDAO = new RoomHoldDAOImpl();

        setLayout(new BorderLayout(15, 15));
        setBorder(new EmptyBorder(10, 10, 10, 10));
        setBackground(Color.WHITE);

        // 1. Top Bar
        JPanel topBar = createTopBar();
        add(topBar, BorderLayout.NORTH);

        // 2. Central Split: Left = Form, Right = Table
        JPanel formPanel = createFormPanel();
        JPanel tablePanel = createTablePanel();

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, formPanel, tablePanel);
        splitPane.setDividerLocation(380);
        splitPane.setResizeWeight(0.35);
        splitPane.setContinuousLayout(true);
        splitPane.setBorder(null);

        add(splitPane, BorderLayout.CENTER);

        // 3. Initial async load of data
        refreshAllData();
    }

    private JPanel createTopBar() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(0, 0, 10, 0));

        JLabel title = new JLabel("Booking Management");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(new Color(24, 43, 73));
        panel.add(title, BorderLayout.WEST);

        // Search Bar on Right
        JPanel searchBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        searchBox.setOpaque(false);

        searchRefField = new JTextField(12);
        searchRefField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        searchRefField.putClientProperty("JTextField.placeholderText", "Ref (e.g. HMS-2026-1001)");
        searchRefField.addActionListener(e -> onSearchReference());

        JButton btnSearch = new JButton("Search");
        btnSearch.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSearch.addActionListener(e -> onSearchReference());

        JButton btnRefresh = new JButton("Refresh / Show All");
        btnRefresh.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnRefresh.addActionListener(e -> {
            searchRefField.setText("");
            refreshAllData();
        });

        searchBox.add(new JLabel("Find Booking:"));
        searchBox.add(searchRefField);
        searchBox.add(btnSearch);
        searchBox.add(btnRefresh);

        panel.add(searchBox, BorderLayout.EAST);
        return panel;
    }

    private JPanel createFormPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createCompoundBorder(
                new TitledBorder(BorderFactory.createLineBorder(new Color(220, 225, 230), 1),
                        "New Room Reservation", TitledBorder.LEADING, TitledBorder.TOP,
                        new Font("Segoe UI", Font.BOLD, 13), new Color(24, 43, 73)),
                new EmptyBorder(12, 12, 12, 12)
        ));

        JPanel fields = new JPanel(new GridLayout(6, 2, 8, 12));
        fields.setOpaque(false);

        // Guest selector
        fields.add(new JLabel("Select Guest *:"));
        guestCombo = new JComboBox<>();
        fields.add(guestCombo);

        // Room selector
        fields.add(new JLabel("Select Room *:"));
        roomCombo = new JComboBox<>();
        roomCombo.addActionListener(e -> updatePricingPreview());
        fields.add(roomCombo);

        // Check-in date
        fields.add(new JLabel("Check-In Date (YYYY-MM-DD) *:"));
        checkInField = new JTextField(LocalDate.now().toString());
        checkInField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) { updatePricingPreview(); }
        });
        fields.add(checkInField);

        // Check-out date
        fields.add(new JLabel("Check-Out Date (YYYY-MM-DD) *:"));
        checkOutField = new JTextField(LocalDate.now().plusDays(1).toString());
        checkOutField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) { updatePricingPreview(); }
        });
        fields.add(checkOutField);

        // Guest count
        fields.add(new JLabel("Number of Guests *:"));
        guestCountSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 10, 1));
        fields.add(guestCountSpinner);

        // Stay Summary preview
        lblNights = new JLabel("Duration: 1 night");
        lblNights.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblNights.setForeground(new Color(41, 128, 185));
        fields.add(lblNights);

        lblTotalAmount = new JLabel("Total: Rs. 0.00");
        lblTotalAmount.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblTotalAmount.setForeground(new Color(39, 174, 96));
        fields.add(lblTotalAmount);

        JPanel centerContainer = new JPanel();
        centerContainer.setLayout(new BoxLayout(centerContainer, BoxLayout.Y_AXIS));
        centerContainer.setOpaque(false);
        centerContainer.add(fields);

        panel.add(centerContainer, BorderLayout.NORTH);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 15));
        buttonPanel.setOpaque(false);

        btnCheckAvailability = new JButton("Check Availability");
        btnCheckAvailability.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnCheckAvailability.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCheckAvailability.addActionListener(e -> onCheckAvailability());

        btnConfirmBooking = new JButton("Confirm Booking");
        btnConfirmBooking.setBackground(new Color(39, 174, 96));
        btnConfirmBooking.setForeground(Color.WHITE);
        btnConfirmBooking.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnConfirmBooking.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnConfirmBooking.addActionListener(this::onConfirmBooking);

        btnClear = new JButton("Clear");
        btnClear.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnClear.addActionListener(e -> clearForm());

        buttonPanel.add(btnCheckAvailability);
        buttonPanel.add(btnConfirmBooking);
        buttonPanel.add(btnClear);

        panel.add(buttonPanel, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(new TitledBorder(BorderFactory.createLineBorder(new Color(220, 225, 230), 1),
                "Existing Bookings", TitledBorder.LEADING, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 13), new Color(24, 43, 73)));

        String[] columns = {"Ref #", "Guest Name", "Phone", "Room", "Type", "Check-In", "Check-Out", "Guests", "Total (Rs.)", "Status"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        bookingTable = new JTable(tableModel);
        bookingTable.setRowHeight(26);
        bookingTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        bookingTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        bookingTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Highlight status column
        bookingTable.getColumnModel().getColumn(9).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String status = (value != null) ? value.toString() : "";
                if ("CONFIRMED".equalsIgnoreCase(status)) {
                    setForeground(new Color(34, 139, 34)); // Forest green
                    setFont(getFont().deriveFont(Font.BOLD));
                } else if ("CANCELLED".equalsIgnoreCase(status)) {
                    setForeground(new Color(180, 40, 40)); // Crimson red
                    setFont(getFont().deriveFont(Font.ITALIC));
                } else if ("CHECKED_IN".equalsIgnoreCase(status)) {
                    setForeground(new Color(30, 100, 200)); // Blue
                    setFont(getFont().deriveFont(Font.BOLD));
                } else {
                    setForeground(Color.DARK_GRAY);
                }
                return c;
            }
        });

        JScrollPane scrollPane = new JScrollPane(bookingTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Action Toolbar at Bottom of Table
        JPanel tableBottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        tableBottom.setOpaque(false);

        btnCancelBooking = new JButton("Cancel Selected Booking");
        btnCancelBooking.setBackground(new Color(231, 76, 60));
        btnCancelBooking.setForeground(Color.WHITE);
        btnCancelBooking.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnCancelBooking.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCancelBooking.addActionListener(this::onCancelSelectedBooking);

        tableBottom.add(btnCancelBooking);
        panel.add(tableBottom, BorderLayout.SOUTH);

        return panel;
    }

    private void updatePricingPreview() {
        RoomItem selectedRoom = (RoomItem) roomCombo.getSelectedItem();
        if (selectedRoom == null) {
            lblTotalAmount.setText("Total: Rs. 0.00");
            return;
        }

        try {
            LocalDate in = LocalDate.parse(checkInField.getText().trim());
            LocalDate out = LocalDate.parse(checkOutField.getText().trim());
            if (out.isAfter(in)) {
                long nights = ChronoUnit.DAYS.between(in, out);
                lblNights.setText("Duration: " + nights + (nights == 1 ? " night" : " nights"));
                BigDecimal total = selectedRoom.room.getPricePerNight().multiply(BigDecimal.valueOf(nights));
                lblTotalAmount.setText("Total: Rs. " + total.toPlainString());
            } else {
                lblNights.setText("Check-out must be after check-in");
                lblTotalAmount.setText("Total: Rs. 0.00");
            }
        } catch (DateTimeParseException e) {
            lblNights.setText("Invalid date format (YYYY-MM-DD)");
            lblTotalAmount.setText("Total: Rs. 0.00");
        }
    }

    private void onCheckAvailability() {
        RoomItem selectedRoom = (RoomItem) roomCombo.getSelectedItem();
        if (selectedRoom == null) {
            JOptionPane.showMessageDialog(this, "Please select a room.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        LocalDate in, out;
        try {
            in = LocalDate.parse(checkInField.getText().trim());
            out = LocalDate.parse(checkOutField.getText().trim());
        } catch (DateTimeParseException e) {
            JOptionPane.showMessageDialog(this, "Please enter valid dates in YYYY-MM-DD format.", 
                    "Invalid Date", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!out.isAfter(in)) {
            JOptionPane.showMessageDialog(this, "Check-out date must be strictly after check-in date.", 
                    "Invalid Dates", JOptionPane.WARNING_MESSAGE);
            return;
        }

        boolean available = bookingDAO.isRoomAvailable(selectedRoom.room.getId(), in, out);

        if (available) {
            JOptionPane.showMessageDialog(this, 
                    "Room " + selectedRoom.room.getRoomNumber() + " (" + selectedRoom.room.getRoomType() 
                    + ") is AVAILABLE from " + in + " to " + out + "!\n\n"
                    + "You can click 'Confirm Booking' to finalize this reservation.", 
                    "Room Available", JOptionPane.INFORMATION_MESSAGE);
        } else {
            // Check if held by another guest
            boolean isHeldByOther = roomHoldDAO.isRoomHeld(selectedRoom.room.getId(), in, out);
            if (isHeldByOther) {
                JOptionPane.showMessageDialog(this, 
                    "Room " + selectedRoom.room.getRoomNumber() 
                    + " is currently on TEMPORARY HOLD by another guest for the selected dates.\n\n"
                    + "Holds expire automatically in up to 5 minutes if not confirmed.\n"
                    + "Please try again shortly or choose a different room.", 
                    "Room Temporarily Held", JOptionPane.WARNING_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, 
                    "Room " + selectedRoom.room.getRoomNumber() 
                    + " is NOT available for the selected dates.\n(Already booked or under maintenance).", 
                    "Room Unavailable", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private void onConfirmBooking(ActionEvent e) {
        GuestItem selectedGuest = (GuestItem) guestCombo.getSelectedItem();
        RoomItem selectedRoom = (RoomItem) roomCombo.getSelectedItem();

        if (selectedGuest == null) {
            JOptionPane.showMessageDialog(this, "Please select an existing guest.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (selectedRoom == null) {
            JOptionPane.showMessageDialog(this, "Please select a room.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        LocalDate in, out;
        try {
            in = LocalDate.parse(checkInField.getText().trim());
            out = LocalDate.parse(checkOutField.getText().trim());
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this, "Please enter valid dates in YYYY-MM-DD format.", "Invalid Dates", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int guestCount = (Integer) guestCountSpinner.getValue();

        Booking booking = new Booking(
                null,
                selectedGuest.guest.getId(),
                selectedRoom.room.getId(),
                in,
                out,
                guestCount,
                null
        );

        try {
            Booking created = bookingDAO.createBooking(booking);

            String msg = "Booking CONFIRMED successfully!\n\n"
                    + "Reference: " + created.getBookingReference() + "\n"
                    + "Guest: " + selectedGuest.guest.getFullName() + "\n"
                    + "Room: " + selectedRoom.room.getRoomNumber() + " (" + selectedRoom.room.getRoomType() + ")\n"
                    + "Dates: " + in + " to " + out + " (" + created.getNumberOfNights() + " nights)\n"
                    + "Total Amount: Rs. " + created.getTotalAmount();

            JOptionPane.showMessageDialog(this, msg, "Booking Confirmed", JOptionPane.INFORMATION_MESSAGE);

            clearForm();
            loadBookingsAsync(null);

        } catch (BookingDAO.BookingOverlapException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Overlap Conflict", JOptionPane.ERROR_MESSAGE);
        } catch (BookingDAO.InvalidBookingDateException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Invalid Date", JOptionPane.WARNING_MESSAGE);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Validation Error", JOptionPane.WARNING_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Database Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onCancelSelectedBooking(ActionEvent e) {
        int selectedRow = bookingTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a booking to cancel from the table.", "No Selection", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String ref = (String) tableModel.getValueAt(selectedRow, 0);
        String status = (String) tableModel.getValueAt(selectedRow, 9);

        if ("CANCELLED".equalsIgnoreCase(status)) {
            JOptionPane.showMessageDialog(this, "Booking " + ref + " is already cancelled.", "Notice", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to cancel booking " + ref + "?\nThis will immediately release the room for other guests.",
                "Confirm Cancellation", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                var opt = bookingDAO.findByReference(ref);
                if (opt.isPresent()) {
                    boolean success = bookingDAO.cancelBooking(opt.get().getId());
                    if (success) {
                        JOptionPane.showMessageDialog(this, "Booking " + ref + " has been cancelled.", 
                                "Success", JOptionPane.INFORMATION_MESSAGE);
                        loadBookingsAsync(null);
                    } else {
                        JOptionPane.showMessageDialog(this, "Cancellation failed.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void onSearchReference() {
        String query = searchRefField.getText().trim();
        if (query.isEmpty()) {
            loadBookingsAsync(null);
        } else {
            loadBookingsAsync(query);
        }
    }

    private void clearForm() {
        if (guestCombo.getItemCount() > 0) guestCombo.setSelectedIndex(0);
        if (roomCombo.getItemCount() > 0) roomCombo.setSelectedIndex(0);
        checkInField.setText(LocalDate.now().toString());
        checkOutField.setText(LocalDate.now().plusDays(1).toString());
        guestCountSpinner.setValue(1);
        updatePricingPreview();
        bookingTable.clearSelection();
    }

    public void refreshAllData() {
        loadGuestsComboAsync();
        loadRoomsComboAsync();
        loadBookingsAsync(null);
    }

    public void selectGuest(int guestId) {
        for (int i = 0; i < guestCombo.getItemCount(); i++) {
            GuestItem item = guestCombo.getItemAt(i);
            if (item != null && item.guest.getId() == guestId) {
                guestCombo.setSelectedIndex(i);
                break;
            }
        }
    }

    public void showBookingsForGuest(int guestId, String guestName) {
        SwingWorker<List<Booking>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Booking> doInBackground() {
                return bookingDAO.findBookingsByGuestId(guestId);
            }

            @Override
            protected void done() {
                try {
                    List<Booking> list = get();
                    tableModel.setRowCount(0);
                    for (Booking b : list) {
                        tableModel.addRow(new Object[]{
                                b.getBookingReference(),
                                (b.getGuestName() != null) ? b.getGuestName() : guestName,
                                (b.getGuestPhone() != null) ? b.getGuestPhone() : "-",
                                (b.getRoomNumber() != null) ? b.getRoomNumber() : ("Room #" + b.getRoomId()),
                                (b.getRoomType() != null) ? b.getRoomType() : "-",
                                b.getCheckInDate(),
                                b.getCheckOutDate(),
                                b.getGuestCount(),
                                (b.getTotalAmount() != null) ? b.getTotalAmount().toPlainString() : "0.00",
                                b.getStatus()
                        });
                    }
                    searchRefField.setText("");
                } catch (Exception e) {
                    System.err.println("[BookingManagementPanel] Could not load guest bookings: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void loadGuestsComboAsync() {
        SwingWorker<List<Guest>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Guest> doInBackground() {
                return guestDAO.findAll();
            }

            @Override
            protected void done() {
                try {
                    List<Guest> list = get();
                    guestCombo.removeAllItems();
                    for (Guest g : list) {
                        guestCombo.addItem(new GuestItem(g));
                    }
                } catch (Exception e) {
                    System.err.println("[BookingManagementPanel] Could not load guests: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void loadRoomsComboAsync() {
        SwingWorker<List<Room>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Room> doInBackground() {
                return roomDAO.findOperationallyAvailableRooms();
            }

            @Override
            protected void done() {
                try {
                    List<Room> list = get();
                    roomCombo.removeAllItems();
                    for (Room r : list) {
                        roomCombo.addItem(new RoomItem(r));
                    }
                    updatePricingPreview();
                } catch (Exception e) {
                    System.err.println("[BookingManagementPanel] Could not load rooms: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void loadBookingsAsync(String refSearch) {
        SwingWorker<List<Booking>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Booking> doInBackground() {
                if (refSearch != null && !refSearch.isBlank()) {
                    var opt = bookingDAO.findByReference(refSearch);
                    return opt.map(List::of).orElse(List.of());
                } else {
                    return bookingDAO.findAllBookings();
                }
            }

            @Override
            protected void done() {
                try {
                    List<Booking> list = get();
                    tableModel.setRowCount(0);
                    for (Booking b : list) {
                        tableModel.addRow(new Object[]{
                                b.getBookingReference(),
                                (b.getGuestName() != null) ? b.getGuestName() : ("Guest #" + b.getGuestId()),
                                (b.getGuestPhone() != null) ? b.getGuestPhone() : "-",
                                (b.getRoomNumber() != null) ? b.getRoomNumber() : ("Room #" + b.getRoomId()),
                                (b.getRoomType() != null) ? b.getRoomType() : "-",
                                b.getCheckInDate(),
                                b.getCheckOutDate(),
                                b.getGuestCount(),
                                (b.getTotalAmount() != null) ? b.getTotalAmount().toPlainString() : "0.00",
                                b.getStatus()
                        });
                    }
                } catch (Exception e) {
                    System.err.println("[BookingManagementPanel] Could not load bookings: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    // Helper wrappers for JComboBox items
    private static class GuestItem {
        final Guest guest;
        GuestItem(Guest guest) { this.guest = guest; }
        @Override
        public String toString() { return guest.getFullName() + " (" + guest.getPhone() + ")"; }
    }

    private static class RoomItem {
        final Room room;
        RoomItem(Room room) { this.room = room; }
        @Override
        public String toString() {
            return "Room " + room.getRoomNumber() + " - " + room.getRoomType() 
                    + " (Cap: " + room.getCapacity() + ", Rs. " + room.getPricePerNight() + "/night)";
        }
    }
}
