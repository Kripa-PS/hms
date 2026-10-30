package com.hms.ui;

import com.formdev.flatlaf.FlatLightLaf;
import com.hms.model.User;
import com.hms.util.DatabaseConnection;
import com.hms.util.UserSession;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * CustomerAppFrame
 * 
 * Main desktop application window for the Customer experience.
 * Features:
 * - Dedicated customer portal navigation
 * - Search & Book Rooms with automatic 5-minute hold
 * - My Bookings management
 * - My Invoices viewing and printing
 * - Customer Profile management
 * - Secure logout returning to LoginFrame
 */
public class CustomerAppFrame extends JFrame {

    private final JTabbedPane tabbedPane;
    private final CustomerRoomSearchPanel searchPanel;
    private final CustomerMyBookingsPanel myBookingsPanel;
    private final CustomerMyInvoicesPanel myInvoicesPanel;
    private final CustomerRoomServicePanel roomServicePanel;
    private final CustomerProfilePanel profilePanel;

    private JLabel lblUserInfo;
    private JLabel lblDbStatus;

    public CustomerAppFrame() {
        setTitle("Grand Horizon Hotel - Guest Portal");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 720);
        setMinimumSize(new Dimension(950, 620));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // 1. Top Header Bar
        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);

        // 2. Central Tabbed Navigation
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 13));

        searchPanel = new CustomerRoomSearchPanel(this::onBookingCompleted);
        tabbedPane.addTab("Search & Book Rooms", searchPanel);

        myBookingsPanel = new CustomerMyBookingsPanel();
        tabbedPane.addTab("My Bookings", myBookingsPanel);

        myInvoicesPanel = new CustomerMyInvoicesPanel();
        tabbedPane.addTab("My Invoices", myInvoicesPanel);

        roomServicePanel = new CustomerRoomServicePanel();
        tabbedPane.addTab("Room Service", roomServicePanel);

        profilePanel = new CustomerProfilePanel();
        tabbedPane.addTab("My Profile", profilePanel);

        // When switching tabs, auto-refresh the active tab
        tabbedPane.addChangeListener(e -> {
            int idx = tabbedPane.getSelectedIndex();
            if (idx == 1) {
                myBookingsPanel.loadBookingsAsync();
            } else if (idx == 2) {
                myInvoicesPanel.loadInvoicesAsync();
            } else if (idx == 3) {
                roomServicePanel.refreshAsync();
            }
        });

        add(tabbedPane, BorderLayout.CENTER);

        // 3. Bottom Status Bar
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(new EmptyBorder(6, 12, 6, 12));
        statusBar.setBackground(new Color(245, 247, 250));

        JLabel infoLabel = new JLabel("Grand Horizon Hotel Guest Portal | Online Booking System");
        infoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        infoLabel.setForeground(Color.GRAY);
        statusBar.add(infoLabel, BorderLayout.WEST);

        lblDbStatus = new JLabel("Database: Connected");
        lblDbStatus.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblDbStatus.setForeground(new Color(34, 139, 34));
        statusBar.add(lblDbStatus, BorderLayout.EAST);

        add(statusBar, BorderLayout.SOUTH);

        checkDbStatusAsync();
    }

    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(24, 43, 73)); // Deep navy blue
        panel.setBorder(new EmptyBorder(12, 20, 12, 20));

        // Left Branding
        JPanel textPanel = new JPanel(new GridLayout(2, 1));
        textPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("GRAND HORIZON HOTEL");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setForeground(Color.WHITE);

        JLabel subtitleLabel = new JLabel("Guest Self-Service Portal");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitleLabel.setForeground(new Color(212, 175, 55)); // Gold

        textPanel.add(titleLabel);
        textPanel.add(subtitleLabel);
        panel.add(textPanel, BorderLayout.WEST);

        // Right User Info & Logout
        JPanel userPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 4));
        userPanel.setOpaque(false);

        User currentUser = UserSession.getCurrentUser();
        String displayName = currentUser != null ? currentUser.getFullName() : "Guest";

        lblUserInfo = new JLabel(displayName);
        lblUserInfo.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblUserInfo.setForeground(Color.WHITE);
        userPanel.add(lblUserInfo);

        JButton btnLogout = new JButton("Logout");
        btnLogout.setBackground(new Color(192, 57, 43)); // Crimson red
        btnLogout.setForeground(Color.WHITE);
        btnLogout.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnLogout.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnLogout.addActionListener(this::onLogout);
        userPanel.add(btnLogout);

        panel.add(userPanel, BorderLayout.EAST);
        return panel;
    }

    private void onBookingCompleted() {
        // Switch to My Bookings tab and refresh
        tabbedPane.setSelectedIndex(1);
        myBookingsPanel.loadBookingsAsync();
        myInvoicesPanel.loadInvoicesAsync();
        roomServicePanel.refreshAsync();
    }

    private void onLogout(ActionEvent e) {
        int res = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to log out?",
                "Confirm Logout", JOptionPane.YES_NO_OPTION);
        if (res == JOptionPane.YES_OPTION) {
            UserSession.logout();
            dispose();
            SwingUtilities.invokeLater(() -> {
                LoginFrame loginFrame = new LoginFrame();
                loginFrame.setVisible(true);
            });
        }
    }

    private void checkDbStatusAsync() {
        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() {
                return DatabaseConnection.testConnection();
            }

            @Override
            protected void done() {
                try {
                    boolean ok = get();
                    if (!ok) {
                        lblDbStatus.setText("Database: Offline");
                        lblDbStatus.setForeground(new Color(192, 57, 43));
                    }
                } catch (Exception ex) {
                    // Ignore
                }
            }
        };
        worker.execute();
    }

    public static void main(String[] args) {
        try {
            FlatLightLaf.setup();
        } catch (Exception e) {
            // Ignore
        }

        SwingUtilities.invokeLater(() -> {
            CustomerAppFrame frame = new CustomerAppFrame();
            frame.setVisible(true);
        });
    }
}
