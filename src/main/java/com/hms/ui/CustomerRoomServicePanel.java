package com.hms.ui;

import com.hms.dao.RoomServiceDAO;
import com.hms.dao.RoomServiceDAOImpl;
import com.hms.model.RoomServiceMenuItem;
import com.hms.model.RoomServiceOrder;
import com.hms.model.StayOption;
import com.hms.util.RoomServiceMenu;
import com.hms.util.UserSession;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * CustomerRoomServicePanel
 * 
 * Customer-facing Swing portal for browsing available room service dining items,
 * configuring quantities and special preparation instructions, reviewing order totals,
 * and tracking delivery progress.
 */
public class CustomerRoomServicePanel extends JPanel {

    private static final Color COL_NAVY = new Color(24, 43, 73);
    private static final Color COL_GOLD = new Color(212, 175, 55);
    private static final Color COL_RED = new Color(192, 40, 40);
    private static final Color COL_GREEN = new Color(34, 139, 34);

    private final RoomServiceDAO roomServiceDAO = new RoomServiceDAOImpl();

    // Active Stay Selection
    private final JComboBox<StayOption> cbActiveStays = new JComboBox<>();
    private final JLabel lblStayWarning = new JLabel();

    // Menu Table & Filtering
    private final JComboBox<String> cbCategory = new JComboBox<>();
    private final JTextField tfSearch = new JTextField(14);
    private DefaultTableModel menuTableModel;
    private JTable menuTable;
    private TableRowSorter<DefaultTableModel> menuSorter;

    // Order Placement Form
    private final JLabel lblSelectedItem = new JLabel("No item selected");
    private final JSpinner spQuantity = new JSpinner(new SpinnerNumberModel(1, 1, 20, 1));
    private final JTextField tfInstructions = new JTextField(20);
    private final JLabel lblEstimatedTotal = new JLabel("Rs. 0.00");
    private final JButton btnPlaceOrder = new JButton("Place Order");

    // Orders History Table
    private DefaultTableModel ordersTableModel;
    private JTable ordersTable;
    private final JButton btnCancelOrder = new JButton("Cancel Order");
    private final JButton btnRefreshOrders = new JButton("Refresh");

    public CustomerRoomServicePanel() {
        setLayout(new BorderLayout(12, 12));
        setBorder(new EmptyBorder(15, 18, 15, 18));
        setBackground(Color.WHITE);

        // Top: Active Stay selection & Header
        add(buildHeaderPanel(), BorderLayout.NORTH);

        // Center: SplitPane with Menu on Left/Top and Order History on Right/Bottom
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildMenuSection(), buildOrdersSection());
        splitPane.setResizeWeight(0.55);
        splitPane.setDividerSize(6);
        splitPane.setBorder(null);
        add(splitPane, BorderLayout.CENTER);

        // Wire event listeners
        initEvents();
    }

    private JPanel buildHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout(10, 8));
        header.setOpaque(false);
        header.setBorder(new CompoundBorder(
                new LineBorder(new Color(225, 230, 238), 1),
                new EmptyBorder(12, 16, 12, 16)
        ));
        header.setBackground(new Color(248, 250, 253));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);
        JLabel title = new JLabel("Room Service & In-Room Dining");
        title.setFont(new Font("Segoe UI", Font.BOLD, 17));
        title.setForeground(COL_NAVY);
        left.add(title);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        JLabel lblOrderFor = new JLabel("Delivering to:");
        lblOrderFor.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblOrderFor.setForeground(COL_NAVY);
        right.add(lblOrderFor);

        cbActiveStays.setPreferredSize(new Dimension(280, 30));
        cbActiveStays.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        right.add(cbActiveStays);

        header.add(left, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);

        lblStayWarning.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblStayWarning.setForeground(COL_RED);
        lblStayWarning.setVisible(false);
        header.add(lblStayWarning, BorderLayout.SOUTH);

        return header;
    }

    private JPanel buildMenuSection() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createTitledBorder(
                new LineBorder(new Color(210, 218, 226), 1),
                "Available Menu Items", 0, 0,
                new Font("Segoe UI", Font.BOLD, 14), COL_NAVY));

        // Filter Bar
        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        filterBar.setOpaque(false);

        for (String cat : RoomServiceMenu.getCategories()) {
            cbCategory.addItem(cat);
        }
        filterBar.add(new JLabel("Category:"));
        filterBar.add(cbCategory);

        filterBar.add(Box.createHorizontalStrut(10));
        filterBar.add(new JLabel("Search:"));
        filterBar.add(tfSearch);

        panel.add(filterBar, BorderLayout.NORTH);

        // Menu Table
        String[] cols = {"Code", "Item Name", "Category", "Description", "Price (Rs.)", "Available"};
        menuTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        menuTable = new JTable(menuTableModel);
        menuTable.setRowHeight(26);
        menuTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        menuTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        menuTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Column widths
        menuTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        menuTable.getColumnModel().getColumn(1).setPreferredWidth(160);
        menuTable.getColumnModel().getColumn(2).setPreferredWidth(80);
        menuTable.getColumnModel().getColumn(3).setPreferredWidth(220);
        menuTable.getColumnModel().getColumn(4).setPreferredWidth(75);
        menuTable.getColumnModel().getColumn(5).setPreferredWidth(65);

        menuSorter = new TableRowSorter<>(menuTableModel);
        menuTable.setRowSorter(menuSorter);

        loadMenuItems();

        panel.add(new JScrollPane(menuTable), BorderLayout.CENTER);

        // Order Form Bar at Bottom of Menu
        panel.add(buildOrderForm(), BorderLayout.SOUTH);

        return panel;
    }

    private JPanel buildOrderForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        form.setBorder(new CompoundBorder(
                new LineBorder(new Color(225, 230, 238), 1),
                new EmptyBorder(10, 12, 10, 12)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Row 0: Selected item name
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 1;
        form.add(new JLabel("Selected Item:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 3;
        lblSelectedItem.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblSelectedItem.setForeground(COL_NAVY);
        form.add(lblSelectedItem, gbc);

        // Row 1: Quantity and Instructions
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1;
        form.add(new JLabel("Quantity:"), gbc);
        gbc.gridx = 1;
        spQuantity.setPreferredSize(new Dimension(65, 26));
        form.add(spQuantity, gbc);

        gbc.gridx = 2;
        form.add(new JLabel("Special Instructions:"), gbc);
        gbc.gridx = 3;
        tfInstructions.setToolTipText("e.g. Less spicy, extra napkins, separate dip (max 50 chars)");
        form.add(tfInstructions, gbc);

        // Row 2: Price Calculation & Submit Button
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 1;
        form.add(new JLabel("Total:"), gbc);
        gbc.gridx = 1;
        lblEstimatedTotal.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblEstimatedTotal.setForeground(COL_GREEN);
        form.add(lblEstimatedTotal, gbc);

        gbc.gridx = 2; gbc.gridwidth = 2;
        btnPlaceOrder.setBackground(COL_NAVY);
        btnPlaceOrder.setForeground(Color.WHITE);
        btnPlaceOrder.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnPlaceOrder.setCursor(new Cursor(Cursor.HAND_CURSOR));
        form.add(btnPlaceOrder, gbc);

        return form;
    }

    private JPanel buildOrdersSection() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createTitledBorder(
                new LineBorder(new Color(210, 218, 226), 1),
                "My Room Service Orders", 0, 0,
                new Font("Segoe UI", Font.BOLD, 14), COL_NAVY));

        String[] cols = {"Order #", "Room", "Item & Requests", "Qty", "Unit (Rs.)", "Total (Rs.)", "Status", "Requested At"};
        ordersTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        ordersTable = new JTable(ordersTableModel);
        ordersTable.setRowHeight(26);
        ordersTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        ordersTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        ordersTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Custom status badge renderer
        ordersTable.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object val, boolean sel, boolean focus, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(table, val, sel, focus, row, col);
                l.setHorizontalAlignment(SwingConstants.CENTER);
                String s = (val != null) ? val.toString() : "";
                if ("REQUESTED".equalsIgnoreCase(s)) {
                    l.setForeground(new Color(190, 110, 0));
                    l.setText("REQUESTED");
                } else if ("IN_PROGRESS".equalsIgnoreCase(s)) {
                    l.setForeground(new Color(25, 100, 180));
                    l.setText("PREPARING");
                } else if ("COMPLETED".equalsIgnoreCase(s)) {
                    l.setForeground(COL_GREEN);
                    l.setText("DELIVERED");
                } else if ("CANCELLED".equalsIgnoreCase(s)) {
                    l.setForeground(Color.GRAY);
                    l.setText("CANCELLED");
                }
                l.setFont(l.getFont().deriveFont(Font.BOLD));
                return l;
            }
        });

        panel.add(new JScrollPane(ordersTable), BorderLayout.CENTER);

        // Action Buttons at Bottom
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 6));
        actions.setOpaque(false);

        btnRefreshOrders.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnRefreshOrders.setCursor(new Cursor(Cursor.HAND_CURSOR));
        actions.add(btnRefreshOrders);

        btnCancelOrder.setBackground(COL_RED);
        btnCancelOrder.setForeground(Color.WHITE);
        btnCancelOrder.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnCancelOrder.setCursor(new Cursor(Cursor.HAND_CURSOR));
        actions.add(btnCancelOrder);

        panel.add(actions, BorderLayout.SOUTH);

        return panel;
    }

    private void initEvents() {
        // Menu item selection
        menuTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateSelectedMenuItem();
            }
        });

        // Quantity spinner update
        spQuantity.addChangeListener(e -> updateEstimatedTotal());

        // Category filter
        cbCategory.addActionListener(e -> applyMenuFilter());

        // Search filter
        tfSearch.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { applyMenuFilter(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { applyMenuFilter(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { applyMenuFilter(); }
        });

        // Place order button
        btnPlaceOrder.addActionListener(e -> onPlaceOrderClicked());

        // Refresh orders button
        btnRefreshOrders.addActionListener(e -> loadCustomerOrdersAsync());

        // Cancel order button
        btnCancelOrder.addActionListener(e -> onCancelOrderClicked());
    }

    private void loadMenuItems() {
        menuTableModel.setRowCount(0);
        for (RoomServiceMenuItem item : RoomServiceMenu.getAllItems()) {
            menuTableModel.addRow(new Object[]{
                    item.getId(),
                    item.getName(),
                    item.getCategory(),
                    item.getDescription(),
                    item.getPrice().toPlainString(),
                    item.isAvailable() ? "Yes" : "No"
            });
        }
    }

    private void applyMenuFilter() {
        String category = (String) cbCategory.getSelectedItem();
        String searchText = tfSearch.getText().trim().toLowerCase();

        RowFilter<DefaultTableModel, Object> rf = new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ?> entry) {
                String cat = entry.getStringValue(2);
                String name = entry.getStringValue(1).toLowerCase();
                String desc = entry.getStringValue(3).toLowerCase();

                boolean matchesCategory = (category == null || "All Categories".equals(category) || category.equalsIgnoreCase(cat));
                boolean matchesSearch = searchText.isEmpty() || name.contains(searchText) || desc.contains(searchText);

                return matchesCategory && matchesSearch;
            }
        };
        menuSorter.setRowFilter(rf);
    }

    private void updateSelectedMenuItem() {
        int selectedRow = menuTable.getSelectedRow();
        if (selectedRow >= 0) {
            int modelRow = menuTable.convertRowIndexToModel(selectedRow);
            String name = (String) menuTableModel.getValueAt(modelRow, 1);
            String priceStr = (String) menuTableModel.getValueAt(modelRow, 4);
            lblSelectedItem.setText(name + " (Rs. " + priceStr + ")");
        } else {
            lblSelectedItem.setText("No item selected");
        }
        updateEstimatedTotal();
    }

    private void updateEstimatedTotal() {
        int selectedRow = menuTable.getSelectedRow();
        if (selectedRow >= 0) {
            int modelRow = menuTable.convertRowIndexToModel(selectedRow);
            String priceStr = (String) menuTableModel.getValueAt(modelRow, 4);
            BigDecimal price = new BigDecimal(priceStr);
            int qty = (Integer) spQuantity.getValue();
            BigDecimal total = price.multiply(BigDecimal.valueOf(qty)).setScale(2, RoundingMode.HALF_UP);
            lblEstimatedTotal.setText("Rs. " + total.toPlainString());
        } else {
            lblEstimatedTotal.setText("Rs. 0.00");
        }
    }

    private void onPlaceOrderClicked() {
        StayOption activeStay = (StayOption) cbActiveStays.getSelectedItem();
        if (activeStay == null) {
            JOptionPane.showMessageDialog(this,
                    "You do not have an active room reservation.\nRoom service is available only for CONFIRMED or CHECKED_IN bookings.",
                    "Active Stay Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int selectedRow = menuTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this,
                    "Please select an item from the menu first.",
                    "No Item Selected", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int modelRow = menuTable.convertRowIndexToModel(selectedRow);
        String itemName = (String) menuTableModel.getValueAt(modelRow, 1);
        BigDecimal unitPrice = new BigDecimal((String) menuTableModel.getValueAt(modelRow, 4));
        int quantity = (Integer) spQuantity.getValue();
        String instructions = tfInstructions.getText().trim();
        BigDecimal total = unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);

        // Review dialog before submitting
        String message = String.format(
                "Please review your room service order:\n\n" +
                "  - Delivery to: %s\n" +
                "  - Item: %s\n" +
                "  - Quantity: %d\n" +
                "  - Unit Price: Rs. %s\n" +
                "  - Special Instructions: %s\n\n" +
                "  Total Amount: Rs. %s\n\n" +
                "Would you like to confirm and place this order?",
                activeStay.toString(), itemName, quantity, unitPrice.toPlainString(),
                instructions.isEmpty() ? "None" : instructions,
                total.toPlainString()
        );

        int confirm = JOptionPane.showConfirmDialog(this, message, "Confirm Room Service Order", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        Integer guestId = UserSession.getCurrentGuestId();
        if (guestId == null) {
            JOptionPane.showMessageDialog(this, "Session expired. Please log in again.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        btnPlaceOrder.setEnabled(false);
        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() {
                return roomServiceDAO.addCustomerOrder(guestId, activeStay.getBookingId(), itemName, quantity, unitPrice, instructions);
            }

            @Override
            protected void done() {
                btnPlaceOrder.setEnabled(true);
                try {
                    boolean success = get();
                    if (success) {
                        JOptionPane.showMessageDialog(CustomerRoomServicePanel.this,
                                "Your room service order has been placed successfully!\nOur staff will prepare and deliver it to your room.",
                                "Order Placed", JOptionPane.INFORMATION_MESSAGE);
                        tfInstructions.setText("");
                        spQuantity.setValue(1);
                        loadCustomerOrdersAsync();
                    } else {
                        JOptionPane.showMessageDialog(CustomerRoomServicePanel.this,
                                "Could not place order. Please ensure your booking is active.",
                                "Order Failed", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(CustomerRoomServicePanel.this,
                            "An unexpected error occurred: " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void onCancelOrderClicked() {
        int selectedRow = ordersTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this,
                    "Please select an order from the table to cancel.",
                    "No Order Selected", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int orderId = (Integer) ordersTableModel.getValueAt(selectedRow, 0);
        String status = (String) ordersTableModel.getValueAt(selectedRow, 6);

        if (!"REQUESTED".equalsIgnoreCase(status)) {
            JOptionPane.showMessageDialog(this,
                    "Only orders with 'REQUESTED' status can be cancelled.\nOrders already in progress or delivered cannot be cancelled.",
                    "Cancellation Unavailable", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to cancel Room Service Order #" + orderId + "?",
                "Confirm Cancellation", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        Integer guestId = UserSession.getCurrentGuestId();
        if (guestId == null) return;

        btnCancelOrder.setEnabled(false);
        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() {
                return roomServiceDAO.cancelCustomerOrder(guestId, orderId);
            }

            @Override
            protected void done() {
                btnCancelOrder.setEnabled(true);
                try {
                    boolean success = get();
                    if (success) {
                        JOptionPane.showMessageDialog(CustomerRoomServicePanel.this,
                                "Order #" + orderId + " has been cancelled.",
                                "Order Cancelled", JOptionPane.INFORMATION_MESSAGE);
                        loadCustomerOrdersAsync();
                    } else {
                        JOptionPane.showMessageDialog(CustomerRoomServicePanel.this,
                                "Could not cancel order. It may have already been accepted by staff.",
                                "Cancellation Failed", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(CustomerRoomServicePanel.this,
                            "Error cancelling order: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    /**
     * Refreshes active bookings and customer orders asynchronously.
     */
    public void refreshAsync() {
        loadActiveStaysAsync();
        loadCustomerOrdersAsync();
    }

    private void loadActiveStaysAsync() {
        Integer guestId = UserSession.getCurrentGuestId();
        if (guestId == null) return;

        SwingWorker<List<StayOption>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<StayOption> doInBackground() {
                return roomServiceDAO.findActiveStaysForGuest(guestId);
            }

            @Override
            protected void done() {
                try {
                    List<StayOption> stays = get();
                    cbActiveStays.removeAllItems();
                    if (stays.isEmpty()) {
                        lblStayWarning.setText("You do not have an active reservation (CONFIRMED or CHECKED_IN). Room service is restricted to active stays.");
                        lblStayWarning.setVisible(true);
                        btnPlaceOrder.setEnabled(false);
                    } else {
                        lblStayWarning.setVisible(false);
                        btnPlaceOrder.setEnabled(true);
                        for (StayOption s : stays) {
                            cbActiveStays.addItem(s);
                        }
                    }
                } catch (Exception e) {
                    lblStayWarning.setText("Error loading active stays: " + e.getMessage());
                    lblStayWarning.setVisible(true);
                }
            }
        };
        worker.execute();
    }

    public void loadCustomerOrdersAsync() {
        Integer guestId = UserSession.getCurrentGuestId();
        if (guestId == null) return;

        btnRefreshOrders.setEnabled(false);
        SwingWorker<List<RoomServiceOrder>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<RoomServiceOrder> doInBackground() {
                return roomServiceDAO.findOrdersByGuestId(guestId);
            }

            @Override
            protected void done() {
                btnRefreshOrders.setEnabled(true);
                try {
                    List<RoomServiceOrder> orders = get();
                    ordersTableModel.setRowCount(0);
                    for (RoomServiceOrder o : orders) {
                        ordersTableModel.addRow(new Object[]{
                                o.getId(),
                                o.getRoomNumber(),
                                o.getServiceItem(),
                                o.getQuantity(),
                                o.getUnitPrice().toPlainString(),
                                o.getTotalPrice().toPlainString(),
                                o.getStatus(),
                                o.getRequestedAt()
                        });
                    }
                } catch (Exception e) {
                    System.err.println("[CustomerRoomServicePanel] Error loading orders: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }
}
