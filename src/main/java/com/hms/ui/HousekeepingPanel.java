package com.hms.ui;

import com.hms.dao.HousekeepingDAO;
import com.hms.dao.HousekeepingDAOImpl;
import com.hms.dao.RoomDAO;
import com.hms.dao.RoomDAOImpl;
import com.hms.model.HousekeepingTask;
import com.hms.model.Room;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * HousekeepingPanel
 *
 * Swing screen for room cleaning and maintenance tasks (Member 4: Services).
 * Talks to the database only through HousekeepingDAO and RoomDAO.
 */
public class HousekeepingPanel extends JPanel {

    private static final Color COL_NAVY = new Color(24, 43, 73);
    private static final String[] FILTERS = {"ALL", "PENDING", "IN_PROGRESS", "COMPLETED"};
    private static final String[] COLUMNS = {"ID", "Room", "Task", "Assigned To", "Status", "Created", "Completed"};

    private final HousekeepingDAO housekeepingDAO = new HousekeepingDAOImpl();
    private final RoomDAO roomDAO = new RoomDAOImpl();

    private final JComboBox<Room> cbRoom = new JComboBox<>();
    private final JTextField tfTask = new JTextField(24);
    private final JTextField tfStaff = new JTextField(14);
    private final JComboBox<String> cbFilter = new JComboBox<>(FILTERS);
    private final DefaultTableModel tableModel = new DefaultTableModel(COLUMNS, 0) {
        @Override
        public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = new JTable(tableModel);

    public HousekeepingPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        // Show "Room 101 (SINGLE)" in the room drop-down
        cbRoom.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Room) {
                    Room room = (Room) value;
                    setText("Room " + room.getRoomNumber() + " (" + room.getRoomType() + ")");
                }
                return this;
            }
        });

        add(buildFormPanel(), BorderLayout.NORTH);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(24);
        add(new JScrollPane(table), BorderLayout.CENTER);

        add(buildActionPanel(), BorderLayout.SOUTH);
    }

    private JPanel buildFormPanel() {
        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        form.setBorder(BorderFactory.createTitledBorder("New housekeeping task"));

        JButton btnAdd = actionButton("Add Task");
        btnAdd.addActionListener(e -> addTask());

        form.add(new JLabel("Room:"));
        form.add(cbRoom);
        form.add(new JLabel("Task:"));
        form.add(tfTask);
        form.add(new JLabel("Assigned to:"));
        form.add(tfStaff);
        form.add(btnAdd);
        return form;
    }

    private JPanel buildActionPanel() {
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));

        JButton btnInProgress = actionButton("Mark In Progress");
        JButton btnCompleted = actionButton("Mark Completed");
        JButton btnRefresh = new JButton("Refresh");

        cbFilter.addActionListener(e -> loadTasks());
        btnInProgress.addActionListener(e -> changeStatus("IN_PROGRESS"));
        btnCompleted.addActionListener(e -> changeStatus("COMPLETED"));
        btnRefresh.addActionListener(e -> refresh());

        actions.add(new JLabel("Show:"));
        actions.add(cbFilter);
        actions.add(btnInProgress);
        actions.add(btnCompleted);
        actions.add(btnRefresh);
        return actions;
    }

    /** Reloads the room drop-down and the task table from the database. */
    public void refresh() {
        Room previous = (Room) cbRoom.getSelectedItem();
        cbRoom.removeAllItems();
        for (Room room : roomDAO.findAll()) {
            cbRoom.addItem(room);
            if (previous != null && room.getId() == previous.getId()) {
                cbRoom.setSelectedItem(room);
            }
        }
        loadTasks();
    }

    private void loadTasks() {
        String filter = (String) cbFilter.getSelectedItem();
        List<HousekeepingTask> tasks = housekeepingDAO.findAll(filter);

        tableModel.setRowCount(0);
        for (HousekeepingTask t : tasks) {
            tableModel.addRow(new Object[]{
                    t.getId(), t.getRoomNumber(), t.getTaskDescription(),
                    t.getAssignedStaff() == null ? "" : t.getAssignedStaff(),
                    t.getStatus(), t.getCreatedAt(), t.getCompletedAt()});
        }
    }

    private void addTask() {
        Room room = (Room) cbRoom.getSelectedItem();
        String task = tfTask.getText().trim();

        if (room == null) {
            showInfo("Please select a room.");
            return;
        }
        if (task.isEmpty()) {
            showInfo("Please describe the task, for example: Deep clean after checkout.");
            return;
        }
        if (task.length() > 255) {
            showInfo("The task description must be 255 characters or fewer.");
            return;
        }

        if (housekeepingDAO.addTask(room.getId(), task, tfStaff.getText().trim())) {
            tfTask.setText("");
            tfStaff.setText("");
            loadTasks();
        } else {
            showError("Could not save the task. Please check the database connection.");
        }
    }

    private void changeStatus(String newStatus) {
        int row = table.getSelectedRow();
        if (row < 0) {
            showInfo("Please select a task in the table first.");
            return;
        }
        int taskId = (Integer) tableModel.getValueAt(row, 0);
        String currentStatus = (String) tableModel.getValueAt(row, 4);

        if ("COMPLETED".equals(currentStatus)) {
            showInfo("This task is already completed.");
            return;
        }
        if (newStatus.equals(currentStatus)) {
            showInfo("This task is already " + newStatus + ".");
            return;
        }

        if (housekeepingDAO.updateStatus(taskId, newStatus)) {
            loadTasks();
        } else {
            showError("Could not update the task. Please check the database connection.");
        }
    }

    private JButton actionButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(COL_NAVY);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        return button;
    }

    private void showInfo(String message) {
        JOptionPane.showMessageDialog(this, message, "Housekeeping", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Housekeeping", JOptionPane.ERROR_MESSAGE);
    }
}
