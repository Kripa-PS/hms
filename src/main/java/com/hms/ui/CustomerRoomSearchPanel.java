package com.hms.ui;

import com.hms.dao.*;
import com.hms.model.Guest;
import com.hms.model.Room;
import com.hms.model.RoomHold;
import com.hms.util.BillCalculator;
import com.hms.util.UserSession;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * CustomerRoomSearchPanel
 * 
 * Customer-facing interface to search and reserve rooms.
 * Allows filtering by check-in, check-out, and guest count.
 * Displays only rooms that are operationally available, not booked, and not held.
 * Automatically initiates the 5-minute room hold when starting a booking.
 */
public class CustomerRoomSearchPanel extends JPanel {

    private final RoomDAO roomDAO;
    private final RoomHoldDAO roomHoldDAO;
    private final GuestDAO guestDAO;
    private final Runnable onBookingSuccessCallback;

    // Search inputs
    private JTextField checkInField;
    private JTextField checkOutField;
    private JSpinner guestCountSpinner;
    private JComboBox<String> roomTypeFilterCombo;
    private JButton btnSearch;
    private JButton btnReset;

    // Results table
    private JTable roomTable;
    private DefaultTableModel tableModel;
    private JButton btnBookNow;
    private JLabel lblResultsCount;

    // Currently loaded rooms
    private List<Room> loadedRooms = new ArrayList<>();

    public CustomerRoomSearchPanel(Runnable onBookingSuccessCallback) {
        this.roomDAO = new RoomDAOImpl();
        this.roomHoldDAO = new RoomHoldDAOImpl();
        this.guestDAO = new GuestDAOImpl();
        this.onBookingSuccessCallback = onBookingSuccessCallback;

        setLayout(new BorderLayout(15, 15));
        setBorder(new EmptyBorder(15, 20, 15, 20));
        setBackground(Color.WHITE);

        // 1. Top Section: Header & Search Filter Card
        JPanel topContainer = new JPanel(new BorderLayout(10, 10));
        topContainer.setOpaque(false);

        JLabel heading = new JLabel("Search & Book Rooms");
        heading.setFont(new Font("Segoe UI", Font.BOLD, 20));
        heading.setForeground(new Color(24, 43, 73));
        topContainer.add(heading, BorderLayout.NORTH);

        JPanel filterCard = createFilterPanel();
        topContainer.add(filterCard, BorderLayout.CENTER);

        add(topContainer, BorderLayout.NORTH);

        // 2. Center Section: Available Rooms Table
        JPanel tablePanel = createTablePanel();
        add(tablePanel, BorderLayout.CENTER);

        // 3. Bottom Action Bar
        JPanel bottomBar = createBottomBar();
        add(bottomBar, BorderLayout.SOUTH);

        // Run initial search
        performSearch();
    }

    private JPanel createFilterPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        panel.setBackground(new Color(245, 248, 252));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 230, 242), 1),
                new EmptyBorder(10, 12, 10, 12)
        ));

        // Check-in
        panel.add(new JLabel("Check-In:"));
        checkInField = new JTextField(LocalDate.now().toString(), 9);
        checkInField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        panel.add(checkInField);

        // Check-out
        panel.add(new JLabel("Check-Out:"));
        checkOutField = new JTextField(LocalDate.now().plusDays(1).toString(), 9);
        checkOutField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        panel.add(checkOutField);

        // Guest count
        panel.add(new JLabel("Guests:"));
        guestCountSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 6, 1));
        panel.add(guestCountSpinner);

        // Room Type
        panel.add(new JLabel("Type:"));
        roomTypeFilterCombo = new JComboBox<>(new String[]{"All Types", "Single", "Double", "Deluxe", "Suite"});
        panel.add(roomTypeFilterCombo);

        // Buttons
        btnSearch = new JButton("Find Available Rooms");
        btnSearch.setBackground(new Color(24, 43, 73));
        btnSearch.setForeground(Color.WHITE);
        btnSearch.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnSearch.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSearch.addActionListener(e -> performSearch());
        panel.add(btnSearch);

        btnReset = new JButton("Reset");
        btnReset.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnReset.addActionListener(e -> {
            checkInField.setText(LocalDate.now().toString());
            checkOutField.setText(LocalDate.now().plusDays(1).toString());
            guestCountSpinner.setValue(1);
            roomTypeFilterCombo.setSelectedIndex(0);
            performSearch();
        });
        panel.add(btnReset);

        return panel;
    }

    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(220, 225, 230), 1),
                " Available Rooms for Selected Dates ", TitledBorder.LEADING, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 13), new Color(24, 43, 73)));

        String[] cols = {"Room #", "Room Type", "Capacity", "Rate / Night (Rs.)", "Amenities"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        roomTable = new JTable(tableModel);
        roomTable.setRowHeight(28);
        roomTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        roomTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        roomTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        roomTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                btnBookNow.setEnabled(roomTable.getSelectedRow() >= 0);
            }
        });

        panel.add(new JScrollPane(roomTable), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createBottomBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.setBorder(new EmptyBorder(8, 0, 0, 0));

        lblResultsCount = new JLabel("Searching rooms...");
        lblResultsCount.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblResultsCount.setForeground(Color.GRAY);
        bar.add(lblResultsCount, BorderLayout.WEST);

        btnBookNow = new JButton("Book Selected Room");
        btnBookNow.setEnabled(false);
        btnBookNow.setBackground(new Color(39, 174, 96));
        btnBookNow.setForeground(Color.WHITE);
        btnBookNow.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnBookNow.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnBookNow.addActionListener(this::onBookSelectedRoom);
        bar.add(btnBookNow, BorderLayout.EAST);

        return bar;
    }

    public void performSearch() {
        LocalDate checkIn;
        LocalDate checkOut;

        try {
            checkIn = LocalDate.parse(checkInField.getText().trim());
            checkOut = LocalDate.parse(checkOutField.getText().trim());
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this, "Dates must be in format YYYY-MM-DD.", "Invalid Date", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (checkIn.isBefore(LocalDate.now())) {
            JOptionPane.showMessageDialog(this, "Check-in date cannot be in the past.", "Invalid Date", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!checkOut.isAfter(checkIn)) {
            JOptionPane.showMessageDialog(this, "Check-out date must be strictly after check-in date.", "Invalid Date", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int requestedGuests = (int) guestCountSpinner.getValue();
        String selectedType = (String) roomTypeFilterCombo.getSelectedItem();

        lblResultsCount.setText("Searching available rooms...");
        btnSearch.setEnabled(false);

        SwingWorker<List<Room>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Room> doInBackground() {
                List<Room> available = roomDAO.findAvailableRoomsForDates(checkIn, checkOut);
                List<Room> filtered = new ArrayList<>();
                for (Room r : available) {
                    if (r.getCapacity() >= requestedGuests) {
                        if (selectedType == null || "All Types".equals(selectedType) || r.getRoomType().equalsIgnoreCase(selectedType)) {
                            filtered.add(r);
                        }
                    }
                }
                return filtered;
            }

            @Override
            protected void done() {
                btnSearch.setEnabled(true);
                try {
                    loadedRooms = get();
                    tableModel.setRowCount(0);
                    for (Room r : loadedRooms) {
                        tableModel.addRow(new Object[]{
                                r.getRoomNumber(),
                                r.getRoomType(),
                                r.getCapacity() + " guests",
                                "Rs. " + r.getPricePerNight().setScale(2, RoundingMode.HALF_UP),
                                r.getAmenities() != null ? r.getAmenities() : "Standard amenities"
                        });
                    }

                    if (loadedRooms.isEmpty()) {
                        lblResultsCount.setText("No available rooms match your criteria for " + checkIn + " to " + checkOut + ".");
                    } else {
                        lblResultsCount.setText("Found " + loadedRooms.size() + " available room" + (loadedRooms.size() > 1 ? "s" : "") + ".");
                    }
                    btnBookNow.setEnabled(false);

                } catch (Exception ex) {
                    lblResultsCount.setText("Error querying database: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void onBookSelectedRoom(ActionEvent e) {
        int row = roomTable.getSelectedRow();
        if (row < 0 || row >= loadedRooms.size()) {
            JOptionPane.showMessageDialog(this, "Please select a room to book.", "No Room Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Room selectedRoom = loadedRooms.get(row);
        LocalDate checkIn = LocalDate.parse(checkInField.getText().trim());
        LocalDate checkOut = LocalDate.parse(checkOutField.getText().trim());
        int guests = (int) guestCountSpinner.getValue();

        // 1. Initiate 5-Minute Room Hold
        try {
            RoomHold hold = roomHoldDAO.createHold(selectedRoom.getId(), checkIn, checkOut);

            // 2. Fetch logged in customer's guest profile
            Integer guestId = UserSession.getCurrentGuestId();
            Guest guest = null;
            if (guestId != null && guestId > 0) {
                guest = guestDAO.findById(guestId).orElse(null);
            }

            // 3. Open Booking Dialog
            Window owner = SwingUtilities.getWindowAncestor(this);
            CustomerBookingDialog dialog = new CustomerBookingDialog(
                    owner, selectedRoom, hold, checkIn, checkOut, guests, guest,
                    () -> {
                        performSearch(); // Refresh search table
                        if (onBookingSuccessCallback != null) {
                            onBookingSuccessCallback.run();
                        }
                    }
            );
            dialog.setVisible(true);

        } catch (RoomHoldDAO.RoomHoldOverlapException ex) {
            JOptionPane.showMessageDialog(this,
                    "Sorry, this room was just reserved or held by another guest!\nPlease choose a different room.",
                    "Room Unavailable", JOptionPane.WARNING_MESSAGE);
            performSearch();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Could not start reservation: " + ex.getMessage(),
                    "Hold Error", JOptionPane.ERROR_MESSAGE);
            performSearch();
        }
    }
}
