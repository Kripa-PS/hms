package com.hms.ui;

import com.hms.dao.GuestDAO;
import com.hms.dao.GuestDAOImpl;
import com.hms.model.Guest;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;

/**
 * GuestManagementPanel
 * 
 * Java Swing GUI for Guest Management (Stage 4).
 * Supports:
 * - Registering new guests (Add)
 * - Viewing all guests in a JTable
 * - Live searching by name or phone number
 * - Selecting a table row to populate form fields
 * - Updating existing guest details (Update)
 * - Input validation & duplicate phone warning
 */
public class GuestManagementPanel extends JPanel {

    private final GuestDAO guestDAO;

    // Form inputs
    private JTextField nameField;
    private JTextField phoneField;
    private JTextField emailField;
    private JComboBox<String> idProofCombo;
    private JTextField idNumberField;
    private JTextField addressField;

    // Action buttons
    private JButton btnAdd;
    private JButton btnUpdate;
    private JButton btnClear;
    private JButton btnBookForGuest;
    private JButton btnViewGuestBookings;

    // Navigation callbacks
    private java.util.function.Consumer<Guest> onBookForGuest;
    private java.util.function.Consumer<Guest> onViewGuestBookings;

    // Search bar
    private JTextField searchField;
    private JButton btnSearch;
    private JButton btnShowAll;

    // Table
    private JTable guestTable;
    private DefaultTableModel tableModel;

    // Current selected guest ID (-1 if none selected)
    private int selectedGuestId = -1;

    public GuestManagementPanel() {
        this.guestDAO = new GuestDAOImpl();

        setLayout(new BorderLayout(15, 15));
        setBorder(new EmptyBorder(10, 10, 10, 10));
        setBackground(Color.WHITE);

        // 1. Top Section: Title & Search
        JPanel topPanel = createTopPanel();
        add(topPanel, BorderLayout.NORTH);

        // 2. Center/Split Section: Left = Form, Right = Table
        JPanel formPanel = createFormPanel();
        JPanel tablePanel = createTablePanel();

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, formPanel, tablePanel);
        splitPane.setDividerLocation(360);
        splitPane.setResizeWeight(0.35);
        splitPane.setContinuousLayout(true);
        splitPane.setBorder(null);

        add(splitPane, BorderLayout.CENTER);

        // Initial table load (asynchronously so UI doesn't hang if DB is offline)
        loadGuestsAsync(null);
    }

    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setOpaque(false);

        JLabel heading = new JLabel("Guest Management");
        heading.setFont(new Font("Segoe UI", Font.BOLD, 20));
        heading.setForeground(new Color(24, 43, 73));
        panel.add(heading, BorderLayout.WEST);

        // Search Bar
        JPanel searchBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        searchBox.setOpaque(false);

        searchField = new JTextField(15);
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        searchField.putClientProperty("JTextField.placeholderText", "Search by name or phone...");
        searchField.addActionListener(e -> performSearch());

        btnSearch = new JButton("Search");
        btnSearch.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSearch.addActionListener(e -> performSearch());

        btnShowAll = new JButton("Refresh / Show All");
        btnShowAll.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnShowAll.addActionListener(e -> {
            searchField.setText("");
            loadGuestsAsync(null);
        });

        searchBox.add(new JLabel("Search:"));
        searchBox.add(searchField);
        searchBox.add(btnSearch);
        searchBox.add(btnShowAll);

        panel.add(searchBox, BorderLayout.EAST);
        panel.setBorder(new EmptyBorder(0, 0, 10, 0));
        return panel;
    }

    private JPanel createFormPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createCompoundBorder(
                new TitledBorder(BorderFactory.createLineBorder(new Color(220, 225, 230), 1), 
                        "Guest Details", TitledBorder.LEADING, TitledBorder.TOP, 
                        new Font("Segoe UI", Font.BOLD, 13), new Color(24, 43, 73)),
                new EmptyBorder(12, 12, 12, 12)
        ));

        JPanel fields = new JPanel(new GridLayout(6, 2, 8, 12));
        fields.setOpaque(false);

        fields.add(new JLabel("Full Name *:"));
        nameField = new JTextField();
        fields.add(nameField);

        fields.add(new JLabel("Phone Number *:"));
        phoneField = new JTextField();
        fields.add(phoneField);

        fields.add(new JLabel("Email Address:"));
        emailField = new JTextField();
        fields.add(emailField);

        fields.add(new JLabel("ID Proof Type:"));
        idProofCombo = new JComboBox<>(new String[]{"Aadhaar", "Passport", "Driving License", "Voter ID", "Other"});
        fields.add(idProofCombo);

        fields.add(new JLabel("ID Proof Number:"));
        idNumberField = new JTextField();
        fields.add(idNumberField);

        fields.add(new JLabel("Address:"));
        addressField = new JTextField();
        fields.add(addressField);

        panel.add(fields, BorderLayout.NORTH);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 15));
        buttonPanel.setOpaque(false);

        btnAdd = new JButton("Add Guest");
        btnAdd.setBackground(new Color(39, 174, 96));
        btnAdd.setForeground(Color.WHITE);
        btnAdd.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnAdd.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnAdd.addActionListener(this::onAddGuest);

        btnUpdate = new JButton("Update Guest");
        btnUpdate.setEnabled(false); // Enabled only when a row is selected
        btnUpdate.setBackground(new Color(41, 128, 185));
        btnUpdate.setForeground(Color.WHITE);
        btnUpdate.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnUpdate.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnUpdate.addActionListener(this::onUpdateGuest);

        btnClear = new JButton("Clear");
        btnClear.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnClear.addActionListener(e -> clearForm());

        buttonPanel.add(btnAdd);
        buttonPanel.add(btnUpdate);
        buttonPanel.add(btnClear);

        panel.add(buttonPanel, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(new TitledBorder(BorderFactory.createLineBorder(new Color(220, 225, 230), 1),
                "Registered Guests", TitledBorder.LEADING, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 13), new Color(24, 43, 73)));

        String[] columns = {"ID", "Full Name", "Phone", "Email", "ID Type", "ID Number", "Address"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // View only in table, edit via form
            }
        };

        guestTable = new JTable(tableModel);
        guestTable.setRowHeight(26);
        guestTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        guestTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        guestTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Row selection listener to populate form
        guestTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                onTableRowSelected();
            }
        });

        JScrollPane scrollPane = new JScrollPane(guestTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Action bar for selected guest
        JPanel guestActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        guestActions.setOpaque(false);

        btnBookForGuest = new JButton("📅 Book for Guest");
        btnBookForGuest.setEnabled(false);
        btnBookForGuest.setBackground(new Color(24, 43, 73));
        btnBookForGuest.setForeground(Color.WHITE);
        btnBookForGuest.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnBookForGuest.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnBookForGuest.addActionListener(e -> triggerBookForGuest());
        guestActions.add(btnBookForGuest);

        btnViewGuestBookings = new JButton("📋 View Guest Bookings");
        btnViewGuestBookings.setEnabled(false);
        btnViewGuestBookings.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnViewGuestBookings.addActionListener(e -> triggerViewGuestBookings());
        guestActions.add(btnViewGuestBookings);

        panel.add(guestActions, BorderLayout.SOUTH);

        return panel;
    }

    public void setOnBookForGuest(java.util.function.Consumer<Guest> callback) {
        this.onBookForGuest = callback;
    }

    public void setOnViewGuestBookings(java.util.function.Consumer<Guest> callback) {
        this.onViewGuestBookings = callback;
    }

    private Guest getSelectedGuest() {
        if (selectedGuestId <= 0) return null;
        Guest g = new Guest(nameField.getText().trim(), phoneField.getText().trim(),
                emailField.getText().trim(), (String) idProofCombo.getSelectedItem(),
                idNumberField.getText().trim(), addressField.getText().trim());
        g.setId(selectedGuestId);
        return g;
    }

    private void triggerBookForGuest() {
        Guest g = getSelectedGuest();
        if (g != null && onBookForGuest != null) {
            onBookForGuest.accept(g);
        }
    }

    private void triggerViewGuestBookings() {
        Guest g = getSelectedGuest();
        if (g != null && onViewGuestBookings != null) {
            onViewGuestBookings.accept(g);
        }
    }

    private void onTableRowSelected() {
        int selectedRow = guestTable.getSelectedRow();
        if (selectedRow >= 0) {
            selectedGuestId = (int) tableModel.getValueAt(selectedRow, 0);
            nameField.setText((String) tableModel.getValueAt(selectedRow, 1));
            phoneField.setText((String) tableModel.getValueAt(selectedRow, 2));
            emailField.setText((String) tableModel.getValueAt(selectedRow, 3));
            idProofCombo.setSelectedItem(tableModel.getValueAt(selectedRow, 4));
            idNumberField.setText((String) tableModel.getValueAt(selectedRow, 5));
            addressField.setText((String) tableModel.getValueAt(selectedRow, 6));

            btnAdd.setEnabled(false);
            btnUpdate.setEnabled(true);
            btnBookForGuest.setEnabled(true);
            btnViewGuestBookings.setEnabled(true);
        }
    }

    private void onAddGuest(ActionEvent e) {
        String name = nameField.getText().trim();
        String phone = phoneField.getText().trim();
        String email = emailField.getText().trim();
        String idType = (String) idProofCombo.getSelectedItem();
        String idNumber = idNumberField.getText().trim();
        String address = addressField.getText().trim();

        // Validation
        if (name.isEmpty() || phone.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                    "Full Name and Phone Number are required fields.", 
                    "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Guest guest = new Guest(name, phone, email, idType, idNumber, address);

        try {
            boolean success = guestDAO.addGuest(guest);
            if (success) {
                JOptionPane.showMessageDialog(this, 
                        "Guest registered successfully!\nGenerated ID: " + guest.getId(), 
                        "Success", JOptionPane.INFORMATION_MESSAGE);
                clearForm();
                loadGuestsAsync(null);
            } else {
                JOptionPane.showMessageDialog(this, 
                        "Could not add guest. Please check database connection.", 
                        "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (GuestDAO.DuplicatePhoneException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Duplicate Phone Number", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, 
                    "Database Error: " + ex.getMessage() + "\n(Ensure MySQL Server is running).", 
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onUpdateGuest(ActionEvent e) {
        if (selectedGuestId <= 0) {
            JOptionPane.showMessageDialog(this, "Please select a guest from the table to update.", 
                    "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String name = nameField.getText().trim();
        String phone = phoneField.getText().trim();
        String email = emailField.getText().trim();
        String idType = (String) idProofCombo.getSelectedItem();
        String idNumber = idNumberField.getText().trim();
        String address = addressField.getText().trim();

        if (name.isEmpty() || phone.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Full Name and Phone Number are required fields.", 
                    "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Guest guest = new Guest(name, phone, email, idType, idNumber, address);
        guest.setId(selectedGuestId);

        try {
            boolean success = guestDAO.updateGuest(guest);
            if (success) {
                JOptionPane.showMessageDialog(this, "Guest details updated successfully!", 
                        "Success", JOptionPane.INFORMATION_MESSAGE);
                clearForm();
                loadGuestsAsync(null);
            } else {
                JOptionPane.showMessageDialog(this, "Update failed. Guest record may not exist.", 
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (GuestDAO.DuplicatePhoneException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Duplicate Phone Number", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Database Error: " + ex.getMessage(), 
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void performSearch() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) {
            loadGuestsAsync(null);
        } else {
            loadGuestsAsync(query);
        }
    }

    private void clearForm() {
        selectedGuestId = -1;
        nameField.setText("");
        phoneField.setText("");
        emailField.setText("");
        idProofCombo.setSelectedIndex(0);
        idNumberField.setText("");
        addressField.setText("");
        guestTable.clearSelection();
        btnAdd.setEnabled(true);
        btnUpdate.setEnabled(false);
        if (btnBookForGuest != null) btnBookForGuest.setEnabled(false);
        if (btnViewGuestBookings != null) btnViewGuestBookings.setEnabled(false);
    }

    /**
     * Loads guests into JTable asynchronously so the Swing EDT remains responsive.
     */
    public void loadGuestsAsync(String searchQuery) {
        SwingWorker<List<Guest>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Guest> doInBackground() {
                if (searchQuery != null && !searchQuery.isBlank()) {
                    return guestDAO.searchByNameOrPhone(searchQuery);
                } else {
                    return guestDAO.findAll();
                }
            }

            @Override
            protected void done() {
                try {
                    List<Guest> guests = get();
                    tableModel.setRowCount(0); // Clear existing rows
                    for (Guest g : guests) {
                        tableModel.addRow(new Object[]{
                                g.getId(),
                                g.getFullName(),
                                g.getPhone(),
                                (g.getEmail() != null) ? g.getEmail() : "",
                                g.getIdProofType(),
                                (g.getIdProofNumber() != null) ? g.getIdProofNumber() : "",
                                (g.getAddress() != null) ? g.getAddress() : ""
                        });
                    }
                } catch (Exception e) {
                    System.err.println("[GuestManagementPanel] Could not load guests: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }
}
