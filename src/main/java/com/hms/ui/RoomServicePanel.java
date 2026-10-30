package com.hms.ui;

import com.hms.dao.RoomServiceDAO;
import com.hms.dao.RoomServiceDAOImpl;
import com.hms.model.RoomServiceOrder;
import com.hms.model.StayOption;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * RoomServicePanel
 *
 * Swing screen for room-service requests (Member 4: Services).
 * Each order is linked to a booking, so the guest and room are known.
 * Talks to the database only through RoomServiceDAO.
 */
public class RoomServicePanel extends JPanel {

    private static final Color COL_NAVY = new Color(24, 43, 73);
    private static final Color COL_RED = new Color(192, 40, 40);
    private static final String[] FILTERS = {"ALL", "REQUESTED", "IN_PROGRESS", "COMPLETED", "CANCELLED"};
    private static final String[] COLUMNS =
            {"ID", "Room", "Guest", "Item", "Qty", "Unit Price", "Total", "Status", "Requested"};

    private final RoomServiceDAO roomServiceDAO = new RoomServiceDAOImpl();

    private final JComboBox<StayOption> cbStay = new JComboBox<>();
    private final JTextField tfItem = new JTextField(16);
    private final JSpinner spQuantity = new JSpinner(new SpinnerNumberModel(1, 1, 50, 1));
    private final JTextField tfPrice = new JTextField(7);
    private final JComboBox<String> cbFilter = new JComboBox<>(FILTERS);
    private final DefaultTableModel tableModel = new DefaultTableModel(COLUMNS, 0) {
        @Override
        public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = new JTable(tableModel);

    public RoomServicePanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        add(buildFormPanel(), BorderLayout.NORTH);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(24);
        add(new JScrollPane(table), BorderLayout.CENTER);

        add(buildActionPanel(), BorderLayout.SOUTH);
    }

    private JPanel buildFormPanel() {
        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        form.setBorder(BorderFactory.createTitledBorder("New room-service request"));

        JButton btnAdd = actionButton("Add Request", COL_NAVY);
        btnAdd.addActionListener(e -> addOrder());

        form.add(new JLabel("Booking:"));
        form.add(cbStay);
        form.add(new JLabel("Item:"));
        form.add(tfItem);
        form.add(new JLabel("Qty:"));
        form.add(spQuantity);
        form.add(new JLabel("Unit price (Rs.):"));
        form.add(tfPrice);
        form.add(btnAdd);
        return form;
    }

    private JPanel buildActionPanel() {
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));

        JButton btnInProgress = actionButton("Mark In Progress", COL_NAVY);
        JButton btnCompleted = actionButton("Mark Completed", COL_NAVY);
        JButton btnCancel = actionButton("Cancel Request", COL_RED);
        JButton btnRefresh = new JButton("Refresh");

        cbFilter.addActionListener(e -> loadOrders());
        btnInProgress.addActionListener(e -> changeStatus("IN_PROGRESS"));
        btnCompleted.addActionListener(e -> changeStatus("COMPLETED"));
        btnCancel.addActionListener(e -> changeStatus("CANCELLED"));
        btnRefresh.addActionListener(e -> refresh());

        actions.add(new JLabel("Show:"));
        actions.add(cbFilter);
        actions.add(btnInProgress);
        actions.add(btnCompleted);
        actions.add(btnCancel);
        actions.add(btnRefresh);
        return actions;
    }

    /** Reloads the booking drop-down and the order table from the database. */
    public void refresh() {
        StayOption previous = (StayOption) cbStay.getSelectedItem();
        cbStay.removeAllItems();
        for (StayOption stay : roomServiceDAO.findOrderableStays()) {
            cbStay.addItem(stay);
            if (previous != null && stay.getBookingId() == previous.getBookingId()) {
                cbStay.setSelectedItem(stay);
            }
        }
        loadOrders();
    }

    private void loadOrders() {
        String filter = (String) cbFilter.getSelectedItem();
        List<RoomServiceOrder> orders = roomServiceDAO.findAll(filter);

        tableModel.setRowCount(0);
        for (RoomServiceOrder o : orders) {
            tableModel.addRow(new Object[]{
                    o.getId(), o.getRoomNumber(), o.getGuestName(), o.getServiceItem(),
                    o.getQuantity(), o.getUnitPrice(), o.getTotalPrice(),
                    o.getStatus(), o.getRequestedAt()});
        }
    }

    private void addOrder() {
        StayOption stay = (StayOption) cbStay.getSelectedItem();
        String item = tfItem.getText().trim();

        if (stay == null) {
            showInfo("There is no booking to order for. Create a booking first.");
            return;
        }
        if (item.isEmpty()) {
            showInfo("Please enter the item, for example: Club Sandwich.");
            return;
        }
        if (item.length() > 100) {
            showInfo("The item name must be 100 characters or fewer.");
            return;
        }

        BigDecimal price;
        try {
            price = new BigDecimal(tfPrice.getText().trim());
        } catch (NumberFormatException ex) {
            showInfo("Unit price must be a number, for example 150.50");
            return;
        }
        if (price.signum() < 0) {
            showInfo("Unit price cannot be negative.");
            return;
        }
        if (price.compareTo(new BigDecimal("99999999.99")) > 0) {
            showInfo("Unit price is too large.");
            return;
        }

        int quantity = (Integer) spQuantity.getValue();
        if (roomServiceDAO.addOrder(stay.getBookingId(), item, quantity, price)) {
            tfItem.setText("");
            tfPrice.setText("");
            spQuantity.setValue(1);
            loadOrders();
        } else {
            showError("Could not save the request. Please check the database connection.");
        }
    }

    private void changeStatus(String newStatus) {
        int row = table.getSelectedRow();
        if (row < 0) {
            showInfo("Please select a request in the table first.");
            return;
        }
        int orderId = (Integer) tableModel.getValueAt(row, 0);
        String currentStatus = (String) tableModel.getValueAt(row, 7);

        if ("COMPLETED".equals(currentStatus) || "CANCELLED".equals(currentStatus)) {
            showInfo("This request is already " + currentStatus + " and cannot be changed.");
            return;
        }
        if (newStatus.equals(currentStatus)) {
            showInfo("This request is already " + newStatus + ".");
            return;
        }
        if ("CANCELLED".equals(newStatus)) {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Cancel this room-service request?", "Confirm Cancel", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }
        }

        if (roomServiceDAO.updateStatus(orderId, newStatus)) {
            loadOrders();
        } else {
            showError("Could not update the request. Please check the database connection.");
        }
    }

    private JButton actionButton(String text, Color background) {
        JButton button = new JButton(text);
        button.setBackground(background);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        return button;
    }

    private void showInfo(String message) {
        JOptionPane.showMessageDialog(this, message, "Room Service", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Room Service", JOptionPane.ERROR_MESSAGE);
    }
}
