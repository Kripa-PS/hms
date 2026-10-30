package com.hms.ui;

import com.hms.dao.RoomDAO;
import com.hms.dao.RoomDAOImpl;
import com.hms.model.Room;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * RoomManagementPanel
 *
 * Swing panel for the Room Management module (Member 2).
 * Provides: Add Room, View All Rooms, Edit Room, Search/Filter Rooms,
 * Manage Status, and Maintenance handling.
 *
 * Talks to the database exclusively through RoomDAO / RoomDAOImpl.
 * DatabaseConnection is NOT used here directly.
 */
public class RoomManagementPanel extends JPanel {

    // -----------------------------------------------------------------------
    // Constants
    // -----------------------------------------------------------------------

    private static final String[] ROOM_TYPES  = {"SINGLE", "DOUBLE", "DELUXE", "SUITE"};
    private static final String[] STATUS_OPTS = {"AVAILABLE", "MAINTENANCE"};   // OCCUPIED is booking module's job
    private static final String[] FILTER_TYPES   = {"ALL", "SINGLE", "DOUBLE", "DELUXE", "SUITE"};
    private static final String[] FILTER_STATUSES = {"ALL", "AVAILABLE", "OCCUPIED", "MAINTENANCE"};

    private static final String[] TABLE_COLUMNS = {
            "ID", "Room No.", "Type", "Capacity", "Rate / Night (Rs.)", "Amenities", "Status"
    };

    // Colour palette (matches MainAppFrame navy/gold theme)
    private static final Color COL_NAVY     = new Color(24,  43,  73);
    private static final Color COL_GOLD     = new Color(212, 175, 55);
    private static final Color COL_BG       = new Color(248, 249, 252);
    private static final Color COL_PANEL_BG = new Color(255, 255, 255);
    private static final Color COL_GREEN    = new Color(34,  139, 34);
    private static final Color COL_RED      = new Color(192, 40,  40);
    private static final Color COL_AMBER    = new Color(210, 140, 0);

    // -----------------------------------------------------------------------
    // Fields
    // -----------------------------------------------------------------------

    private final RoomDAO roomDAO = new RoomDAOImpl();

    // Currently selected room ID (0 = none selected / add mode)
    private int selectedRoomId = 0;

    // ---- Form fields ----
    private JTextField tfRoomNumber;
    private JComboBox<String> cbRoomType;
    private JTextField tfCapacity;
    private JTextField tfPrice;
    private JTextField tfAmenities;
    private JLabel lblStatusDisplay;   // shows current status (read-only when editing)
    private JComboBox<String> cbStatusForNew; // only active when adding a new room

    // ---- Search fields ----
    private JTextField tfSearchKeyword;
    private JComboBox<String> cbFilterType;
    private JComboBox<String> cbFilterStatus;

    // ---- Table ----
    private JTable roomTable;
    private DefaultTableModel tableModel;

    // ---- Buttons ----
    private JButton btnAdd;
    private JButton btnSave;
    private JButton btnClear;
    private JButton btnMarkMaintenance;
    private JButton btnRestoreAvailable;

    // ---- Status bar inside panel ----
    private JLabel lblPanelStatus;

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    public RoomManagementPanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(COL_BG);
        setBorder(new EmptyBorder(15, 15, 15, 15));

        add(buildHeaderBar(),   BorderLayout.NORTH);
        add(buildCenterSplit(), BorderLayout.CENTER);
        add(buildStatusBar(),   BorderLayout.SOUTH);

        loadAllRooms();
    }

    // -----------------------------------------------------------------------
    // UI construction
    // -----------------------------------------------------------------------

    /** Top header bar with module title. */
    private JPanel buildHeaderBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(COL_NAVY);
        bar.setBorder(new EmptyBorder(12, 16, 12, 16));

        JLabel title = new JLabel("Room Management");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(Color.WHITE);

        JLabel sub = new JLabel("Add, edit, search, and manage room inventory");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sub.setForeground(COL_GOLD);

        JPanel text = new JPanel(new GridLayout(2, 1, 0, 2));
        text.setOpaque(false);
        text.add(title);
        text.add(sub);

        bar.add(text, BorderLayout.WEST);
        return bar;
    }

    /** Main split: form on left, table+search on right. */
    private JSplitPane buildCenterSplit() {
        JSplitPane split = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                buildFormPanel(),
                buildTablePanel());
        split.setDividerLocation(340);
        split.setDividerSize(6);
        split.setBackground(COL_BG);
        return split;
    }

    // ---- LEFT: Form Panel ----

    private JPanel buildFormPanel() {
        JPanel outer = new JPanel(new BorderLayout(0, 12));
        outer.setBackground(COL_BG);
        outer.setBorder(new EmptyBorder(0, 0, 0, 8));

        // --- Fields card ---
        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(COL_PANEL_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 224, 230), 1),
                new EmptyBorder(16, 18, 16, 18)));

        GridBagConstraints lc = labelConstraints();
        GridBagConstraints fc = fieldConstraints();
        int row = 0;

        // Room Number
        card.add(formLabel("Room Number *"), at(lc, 0, row));
        tfRoomNumber = styledTextField(12);
        tfRoomNumber.setToolTipText("e.g. 101, A02  (max 10 chars)");
        card.add(tfRoomNumber, at(fc, 1, row++));

        // Room Type
        card.add(formLabel("Room Type *"), at(lc, 0, row));
        cbRoomType = new JComboBox<>(ROOM_TYPES);
        styleCombo(cbRoomType);
        card.add(cbRoomType, at(fc, 1, row++));

        // Capacity
        card.add(formLabel("Capacity *"), at(lc, 0, row));
        tfCapacity = styledTextField(5);
        tfCapacity.setToolTipText("Max guests (positive integer)");
        card.add(tfCapacity, at(fc, 1, row++));

        // Price per night
        card.add(formLabel("Price / Night (Rs.) *"), at(lc, 0, row));
        tfPrice = styledTextField(12);
        tfPrice.setToolTipText("e.g. 2500.00");
        card.add(tfPrice, at(fc, 1, row++));

        // Amenities
        card.add(formLabel("Amenities"), at(lc, 0, row));
        tfAmenities = styledTextField(22);
        tfAmenities.setText("WiFi, TV, AC");
        tfAmenities.setToolTipText("Comma-separated list of amenities");
        card.add(tfAmenities, at(fc, 1, row++));

        // Status (shown when adding)
        card.add(formLabel("Status *"), at(lc, 0, row));
        cbStatusForNew = new JComboBox<>(STATUS_OPTS);
        styleCombo(cbStatusForNew);
        lblStatusDisplay = new JLabel("-");
        lblStatusDisplay.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblStatusDisplay.setVisible(false);
        JPanel statusSlot = new JPanel(new CardLayout());
        statusSlot.setOpaque(false);
        statusSlot.add(cbStatusForNew,   "combo");
        statusSlot.add(lblStatusDisplay, "label");
        card.add(statusSlot, at(fc, 1, row++));

        outer.add(card, BorderLayout.NORTH);

        // --- Button panel ---
        outer.add(buildFormButtons(), BorderLayout.CENTER);

        return outer;
    }

    private JPanel buildFormButtons() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(COL_BG);
        p.setBorder(new EmptyBorder(8, 0, 0, 0));

        btnAdd  = actionButton("Add Room",      new Color(30, 120, 60));
        btnSave = actionButton("Save Changes",   new Color(30,  80, 160));
        btnClear = actionButton("Clear / New",   new Color(100, 100, 110));

        btnMarkMaintenance   = actionButton("Mark Maintenance",   new Color(180, 90, 0));
        btnRestoreAvailable  = actionButton("Restore Available",  new Color(30, 120, 60));

        btnSave.setEnabled(false);            // enabled only when a row is selected
        btnMarkMaintenance.setEnabled(false);
        btnRestoreAvailable.setEnabled(false);

        p.add(btnAdd);
        p.add(Box.createVerticalStrut(6));
        p.add(btnSave);
        p.add(Box.createVerticalStrut(6));
        p.add(btnClear);
        p.add(Box.createVerticalStrut(16));

        JLabel sep = new JLabel("--- Status Actions ---");
        sep.setFont(new Font("Segoe UI", Font.BOLD, 11));
        sep.setForeground(new Color(140, 140, 150));
        sep.setAlignmentX(Component.CENTER_ALIGNMENT);
        p.add(sep);
        p.add(Box.createVerticalStrut(6));

        p.add(btnMarkMaintenance);
        p.add(Box.createVerticalStrut(6));
        p.add(btnRestoreAvailable);

        // Wire actions
        btnAdd.addActionListener(e -> onAddRoom());
        btnSave.addActionListener(e -> onSaveChanges());
        btnClear.addActionListener(e -> clearForm());
        btnMarkMaintenance.addActionListener(e -> onSetMaintenance(true));
        btnRestoreAvailable.addActionListener(e -> onSetMaintenance(false));

        return p;
    }

    // ---- RIGHT: Table + Search Panel ----

    private JPanel buildTablePanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(COL_BG);

        panel.add(buildSearchBar(),  BorderLayout.NORTH);
        panel.add(buildTable(),      BorderLayout.CENTER);
        panel.add(buildTableButtons(), BorderLayout.SOUTH);

        return panel;
    }

    private JPanel buildSearchBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        bar.setBackground(COL_PANEL_BG);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 224, 230), 1),
                new EmptyBorder(4, 8, 4, 8)));

        bar.add(new JLabel("Search:"));

        tfSearchKeyword = new JTextField(12);
        tfSearchKeyword.setToolTipText("Search by room number");
        bar.add(tfSearchKeyword);

        bar.add(new JLabel("Type:"));
        cbFilterType = new JComboBox<>(FILTER_TYPES);
        cbFilterType.setPreferredSize(new Dimension(100, 28));
        bar.add(cbFilterType);

        bar.add(new JLabel("Status:"));
        cbFilterStatus = new JComboBox<>(FILTER_STATUSES);
        cbFilterStatus.setPreferredSize(new Dimension(120, 28));
        bar.add(cbFilterStatus);

        JButton btnSearch = new JButton("Search");
        btnSearch.setFocusPainted(false);
        btnSearch.addActionListener(e -> onSearch());
        bar.add(btnSearch);

        JButton btnClearSearch = new JButton("Clear");
        btnClearSearch.setFocusPainted(false);
        btnClearSearch.addActionListener(e -> {
            tfSearchKeyword.setText("");
            cbFilterType.setSelectedIndex(0);
            cbFilterStatus.setSelectedIndex(0);
            loadAllRooms();
        });
        bar.add(btnClearSearch);

        return bar;
    }

    private JScrollPane buildTable() {
        tableModel = new DefaultTableModel(TABLE_COLUMNS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        roomTable = new JTable(tableModel);
        roomTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        roomTable.setRowHeight(28);
        roomTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        roomTable.getTableHeader().setBackground(new Color(235, 238, 245));
        roomTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        roomTable.setGridColor(new Color(230, 233, 240));
        roomTable.setShowGrid(true);
        roomTable.setIntercellSpacing(new Dimension(1, 1));

        // Column widths
        int[] widths = {40, 80, 80, 70, 90, 160, 100};
        for (int i = 0; i < widths.length; i++) {
            roomTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        // Colour-code the Status column (index 6)
        roomTable.getColumnModel().getColumn(6).setCellRenderer(new StatusCellRenderer());

        // Row selection -> populate form
        roomTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                onTableRowSelected();
            }
        });

        JScrollPane scroll = new JScrollPane(roomTable);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(220, 224, 230), 1));
        return scroll;
    }

    private JPanel buildTableButtons() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        p.setBackground(COL_BG);

        JButton btnRefresh = new JButton("Refresh");
        btnRefresh.setFocusPainted(false);
        btnRefresh.addActionListener(e -> loadAllRooms());
        p.add(btnRefresh);

        return p;
    }

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(new Color(240, 243, 248));
        bar.setBorder(new EmptyBorder(4, 12, 4, 12));

        lblPanelStatus = new JLabel("Ready");
        lblPanelStatus.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblPanelStatus.setForeground(Color.DARK_GRAY);
        bar.add(lblPanelStatus, BorderLayout.WEST);

        return bar;
    }

    // -----------------------------------------------------------------------
    // Business Logic / Event Handlers
    // -----------------------------------------------------------------------

    /** Loads all rooms from the database into the table. */
    private void loadAllRooms() {
        List<Room> rooms = roomDAO.findAll();
        populateTable(rooms);
        setStatus("Loaded " + rooms.size() + " room(s).");
    }

    /** Fills the table with the given list of rooms. */
    private void populateTable(List<Room> rooms) {
        tableModel.setRowCount(0);
        if (rooms.isEmpty()) {
            setStatus("No rooms found.");
            return;
        }
        for (Room r : rooms) {
            tableModel.addRow(new Object[]{
                    r.getId(),
                    r.getRoomNumber(),
                    r.getRoomType(),
                    r.getCapacity(),
                    r.getPricePerNight(),
                    r.getAmenities() != null ? r.getAmenities() : "",
                    r.getOperationalStatus()
            });
        }
    }

    /** Handles clicking Search. */
    private void onSearch() {
        String keyword = tfSearchKeyword.getText();
        String type    = (String) cbFilterType.getSelectedItem();
        String status  = (String) cbFilterStatus.getSelectedItem();

        List<Room> results = roomDAO.searchRooms(keyword, type, status);
        populateTable(results);
        setStatus("Search returned " + results.size() + " room(s).");
    }

    /** Handles Add Room button. */
    private void onAddRoom() {
        // Gather and validate inputs
        String roomNumber = tfRoomNumber.getText().trim();
        String roomType   = (String) cbRoomType.getSelectedItem();
        String capacityTxt = tfCapacity.getText().trim();
        String priceTxt   = tfPrice.getText().trim();
        String amenities  = tfAmenities.getText().trim();
        String status     = (String) cbStatusForNew.getSelectedItem();

        String error = validateForm(roomNumber, capacityTxt, priceTxt, 0);
        if (error != null) {
            showError(error);
            return;
        }

        Room room = new Room(
                roomNumber.toUpperCase(),
                roomType,
                Integer.parseInt(capacityTxt),
                new BigDecimal(priceTxt),
                status,
                amenities.isEmpty() ? "WiFi, TV, AC" : amenities
        );

        boolean ok = roomDAO.addRoom(room);
        if (ok) {
            showInfo("Room " + roomNumber.toUpperCase() + " added successfully!");
            clearForm();
            loadAllRooms();
        } else {
            showError("Could not add room. Check the database connection.");
        }
    }

    /** Handles Save Changes button (edit mode). */
    private void onSaveChanges() {
        if (selectedRoomId == 0) {
            showError("Please select a room from the table to edit.");
            return;
        }

        String roomNumber  = tfRoomNumber.getText().trim();
        String roomType    = (String) cbRoomType.getSelectedItem();
        String capacityTxt = tfCapacity.getText().trim();
        String priceTxt    = tfPrice.getText().trim();
        String amenities   = tfAmenities.getText().trim();
        String currentStatus = lblStatusDisplay.getText().trim();

        String error = validateForm(roomNumber, capacityTxt, priceTxt, selectedRoomId);
        if (error != null) {
            showError(error);
            return;
        }

        Room room = new Room(
                selectedRoomId,
                roomNumber.toUpperCase(),
                roomType,
                Integer.parseInt(capacityTxt),
                new BigDecimal(priceTxt),
                currentStatus
        );
        room.setAmenities(amenities.isEmpty() ? "WiFi, TV, AC" : amenities);

        int confirm = JOptionPane.showConfirmDialog(this,
                "Save changes to Room " + roomNumber.toUpperCase() + "?",
                "Confirm Edit", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        boolean ok = roomDAO.updateRoom(room);
        if (ok) {
            showInfo("Room " + roomNumber.toUpperCase() + " updated successfully!");
            clearForm();
            loadAllRooms();
        } else {
            showError("Could not save changes. Check the database connection.");
        }
    }

    /** Handles Mark Maintenance / Restore Available. */
    private void onSetMaintenance(boolean maintenance) {
        if (selectedRoomId == 0) {
            showError("Please select a room from the table first.");
            return;
        }

        String currentStatus = lblStatusDisplay.getText().trim();

        if (maintenance) {
            if ("OCCUPIED".equals(currentStatus)) {
                showError("Cannot mark an OCCUPIED room for maintenance.\n"
                        + "The room has an active booking. Wait for checkout.");
                return;
            }
            if ("MAINTENANCE".equals(currentStatus)) {
                showError("Room is already under maintenance.");
                return;
            }
        } else {
            if ("AVAILABLE".equals(currentStatus)) {
                showError("Room is already AVAILABLE.");
                return;
            }
            if ("OCCUPIED".equals(currentStatus)) {
                showError("Cannot restore an OCCUPIED room directly.\n"
                        + "The booking module manages OCCUPIED status.");
                return;
            }
        }

        String targetStatus = maintenance ? "MAINTENANCE" : "AVAILABLE";
        int confirm = JOptionPane.showConfirmDialog(this,
                "Change Room " + getSelectedRoomNumber() + " status to " + targetStatus + "?",
                "Confirm Status Change", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        boolean ok = roomDAO.setMaintenanceStatus(selectedRoomId, maintenance);
        if (ok) {
            showInfo("Room status updated to " + targetStatus + ".");
            clearForm();
            loadAllRooms();
        } else {
            showError("Status could not be updated. The room may have changed status - please refresh.");
        }
    }

    /** Called when user clicks a table row - populates form in edit mode. */
    private void onTableRowSelected() {
        int viewRow = roomTable.getSelectedRow();
        if (viewRow < 0) {
            clearForm();
            return;
        }

        int modelRow = roomTable.convertRowIndexToModel(viewRow);
        selectedRoomId = (int) tableModel.getValueAt(modelRow, 0);

        tfRoomNumber.setText((String) tableModel.getValueAt(modelRow, 1));
        String type = (String) tableModel.getValueAt(modelRow, 2);
        cbRoomType.setSelectedItem(type);
        tfCapacity.setText(String.valueOf(tableModel.getValueAt(modelRow, 3)));
        tfPrice.setText(String.valueOf(tableModel.getValueAt(modelRow, 4)));
        tfAmenities.setText((String) tableModel.getValueAt(modelRow, 5));

        String status = (String) tableModel.getValueAt(modelRow, 6);
        lblStatusDisplay.setText(status);
        lblStatusDisplay.setForeground(statusColour(status));

        // Switch status slot to label (edit mode - status not freely editable by form)
        switchStatusSlot(false);

        // Enable edit/maintenance buttons, disable add
        btnAdd.setEnabled(false);
        btnSave.setEnabled(true);
        btnMarkMaintenance.setEnabled(!"OCCUPIED".equals(status) && !"MAINTENANCE".equals(status));
        btnRestoreAvailable.setEnabled("MAINTENANCE".equals(status));

        setStatus("Editing Room: " + tableModel.getValueAt(modelRow, 1));
    }

    /** Resets form to add-new-room mode. */
    private void clearForm() {
        selectedRoomId = 0;
        tfRoomNumber.setText("");
        cbRoomType.setSelectedIndex(0);
        tfCapacity.setText("");
        tfPrice.setText("");
        tfAmenities.setText("WiFi, TV, AC");
        cbStatusForNew.setSelectedIndex(0);
        lblStatusDisplay.setText("-");
        switchStatusSlot(true);   // show combo again

        btnAdd.setEnabled(true);
        btnSave.setEnabled(false);
        btnMarkMaintenance.setEnabled(false);
        btnRestoreAvailable.setEnabled(false);

        roomTable.clearSelection();
        setStatus("Ready to add a new room.");
    }

    // -----------------------------------------------------------------------
    // Validation
    // -----------------------------------------------------------------------

    /**
     * Validates the form fields.
     *
     * @param roomNumber   trimmed room number
     * @param capacityTxt  raw capacity string
     * @param priceTxt     raw price string
     * @param editingId    0 = new room, positive = editing existing (exclude this ID in dup check)
     * @return null if valid, or an error message string
     */
    private String validateForm(String roomNumber, String capacityTxt, String priceTxt, int editingId) {
        if (roomNumber.isEmpty()) return "Room Number is required.";
        if (roomNumber.length() > 10) return "Room Number must be 10 characters or fewer.";

        if (roomDAO.roomNumberExists(roomNumber, editingId)) {
            return "Room Number '" + roomNumber.toUpperCase() + "' already exists.";
        }

        if (capacityTxt.isEmpty()) return "Capacity is required.";
        try {
            int cap = Integer.parseInt(capacityTxt);
            if (cap <= 0) return "Capacity must be a positive integer.";
        } catch (NumberFormatException e) {
            return "Capacity must be a whole number (e.g. 2).";
        }

        if (priceTxt.isEmpty()) return "Price per night is required.";
        try {
            BigDecimal price = new BigDecimal(priceTxt);
            if (price.compareTo(BigDecimal.ZERO) < 0) return "Price cannot be negative.";
        } catch (NumberFormatException e) {
            return "Price must be a valid number (e.g. 1500.00).";
        }

        return null; // all good
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /** Returns the room number shown in the form (for messages). */
    private String getSelectedRoomNumber() {
        return tfRoomNumber.getText().trim().toUpperCase();
    }

    /** Switches the status slot between JComboBox (add mode) and JLabel (edit mode). */
    private void switchStatusSlot(boolean showCombo) {
        // The status slot is the last row's field in the form.
        // We stored it as a CardLayout panel; we toggle by iterating parent containers.
        // Since we used a local CardLayout panel, we walk up tfRoomNumber's parent chain
        // to find it, OR we simply keep a reference. Let's find it via the label's parent.
        Container parent = lblStatusDisplay.getParent();
        if (parent instanceof JPanel) {
            CardLayout cl = (CardLayout) parent.getLayout();
            cl.show(parent, showCombo ? "combo" : "label");
        }
        cbStatusForNew.setVisible(showCombo);
        lblStatusDisplay.setVisible(!showCombo);
    }

    private Color statusColour(String status) {
        if (status == null) return Color.DARK_GRAY;
        return switch (status) {
            case "AVAILABLE"   -> COL_GREEN;
            case "OCCUPIED"    -> COL_RED;
            case "MAINTENANCE" -> COL_AMBER;
            default            -> Color.DARK_GRAY;
        };
    }

    private void setStatus(String message) {
        lblPanelStatus.setText(message);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void showInfo(String message) {
        JOptionPane.showMessageDialog(this, message, "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    // -----------------------------------------------------------------------
    // GridBagLayout helpers (keeps the form code readable)
    // -----------------------------------------------------------------------

    private GridBagConstraints labelConstraints() {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 0, 6, 10);
        c.anchor = GridBagConstraints.WEST;
        c.fill   = GridBagConstraints.NONE;
        return c;
    }

    private GridBagConstraints fieldConstraints() {
        GridBagConstraints c = new GridBagConstraints();
        c.insets  = new Insets(6, 0, 6, 0);
        c.fill    = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        return c;
    }

    private GridBagConstraints at(GridBagConstraints base, int gridx, int gridy) {
        GridBagConstraints c = (GridBagConstraints) base.clone();
        c.gridx = gridx;
        c.gridy = gridy;
        return c;
    }

    private JLabel formLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lbl.setForeground(new Color(60, 70, 85));
        return lbl;
    }

    private JTextField styledTextField(int cols) {
        JTextField tf = new JTextField(cols);
        tf.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        return tf;
    }

    private void styleCombo(JComboBox<String> cb) {
        cb.setFont(new Font("Segoe UI", Font.PLAIN, 13));
    }

    private JButton actionButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setOpaque(true);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Hover effect: lighten background
        Color lighter = bg.brighter();
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                if (btn.isEnabled()) btn.setBackground(lighter);
            }
            @Override public void mouseExited(java.awt.event.MouseEvent e) {
                if (btn.isEnabled()) btn.setBackground(bg);
            }
        });

        return btn;
    }

    // -----------------------------------------------------------------------
    // Status column renderer
    // -----------------------------------------------------------------------

    /** Renders the Status column with colour-coded badges. */
    private class StatusCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {

            JLabel lbl = (JLabel) super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);

            lbl.setHorizontalAlignment(SwingConstants.CENTER);
            lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
            lbl.setOpaque(true);

            if (!isSelected) {
                String status = value != null ? value.toString() : "";
                switch (status) {
                    case "AVAILABLE"   -> { lbl.setBackground(new Color(220, 245, 220)); lbl.setForeground(COL_GREEN); }
                    case "OCCUPIED"    -> { lbl.setBackground(new Color(250, 220, 220)); lbl.setForeground(COL_RED); }
                    case "MAINTENANCE" -> { lbl.setBackground(new Color(255, 243, 205)); lbl.setForeground(COL_AMBER); }
                    default            -> { lbl.setBackground(table.getBackground()); lbl.setForeground(table.getForeground()); }
                }
            }
            return lbl;
        }
    }
}
