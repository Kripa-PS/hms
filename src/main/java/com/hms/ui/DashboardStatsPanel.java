package com.hms.ui;

import com.hms.dao.HousekeepingDAO;
import com.hms.dao.HousekeepingDAOImpl;
import com.hms.dao.RoomServiceDAO;
import com.hms.dao.RoomServiceDAOImpl;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * DashboardStatsPanel
 *
 * Live summary numbers for the Dashboard home screen (Member 4).
 * The numbers are read on a background thread so the window never freezes
 * if MySQL is slow or offline; an unreachable database shows "N/A".
 */
public class DashboardStatsPanel extends JPanel {

    private final HousekeepingDAO housekeepingDAO = new HousekeepingDAOImpl();
    private final RoomServiceDAO roomServiceDAO = new RoomServiceDAOImpl();

    private final JLabel pendingTasksValue = bigNumber();
    private final JLabel openOrdersValue = bigNumber();

    public DashboardStatsPanel() {
        setLayout(new GridLayout(1, 2, 15, 15));
        setOpaque(false);
        add(createStatBox("Pending housekeeping tasks", pendingTasksValue, new Color(210, 140, 0)));
        add(createStatBox("New room-service requests", openOrdersValue, new Color(30, 100, 180)));
    }

    /** Reloads the numbers in the background. */
    public void refresh() {
        SwingWorker<int[], Void> worker = new SwingWorker<>() {
            @Override
            protected int[] doInBackground() {
                return new int[]{
                        housekeepingDAO.countByStatus("PENDING"),
                        roomServiceDAO.countByStatus("REQUESTED")};
            }

            @Override
            protected void done() {
                try {
                    int[] counts = get();
                    pendingTasksValue.setText(counts[0] < 0 ? "N/A" : String.valueOf(counts[0]));
                    openOrdersValue.setText(counts[1] < 0 ? "N/A" : String.valueOf(counts[1]));
                } catch (Exception e) {
                    pendingTasksValue.setText("N/A");
                    openOrdersValue.setText("N/A");
                }
            }
        };
        worker.execute();
    }

    private JLabel bigNumber() {
        JLabel label = new JLabel("-", SwingConstants.LEFT);
        label.setFont(new Font("Segoe UI", Font.BOLD, 32));
        return label;
    }

    private JPanel createStatBox(String caption, JLabel value, Color accent) {
        JPanel box = new JPanel(new BorderLayout(5, 4));
        box.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(225, 230, 235), 1),
                new EmptyBorder(10, 14, 10, 14)));
        box.setBackground(new Color(250, 251, 253));

        JLabel captionLabel = new JLabel(caption);
        captionLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        captionLabel.setForeground(accent);
        value.setForeground(new Color(24, 43, 73));

        box.add(captionLabel, BorderLayout.NORTH);
        box.add(value, BorderLayout.CENTER);
        return box;
    }
}
