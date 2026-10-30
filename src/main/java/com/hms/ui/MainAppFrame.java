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
 * MainAppFrame
 *
 * Main Swing Window for the Hotel Management System.
 * Uses FlatLaf for a clean, modern desktop interface on Windows.
 *
 * Coordinates module navigation:
 * - Guests (Assigned Module)
 * - Bookings (Assigned Module)
 * - Rooms (Member 2 - Room Management Module)
 * - Billing (Assigned Module)
 * - Services (Teammate Placeholder)
 */
public class MainAppFrame extends JFrame {

    private final CardLayout cardLayout;
    private final JPanel contentPanel;
    private final JLabel dbStatusLabel;

    public MainAppFrame() {
        setTitle("Hotel Management System - BTech CSE Mini-Project");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 700);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null); // Center on screen

        // Root layout
        setLayout(new BorderLayout());

        // 1. Top Header Bar
        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);

        // 2. Navigation Sidebar
        JPanel sidebarPanel = createSidebarPanel();
        add(sidebarPanel, BorderLayout.WEST);

        // 3. Central Content Panel with CardLayout
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Add views
        contentPanel.add(createWelcomePanel(), "WELCOME");
        GuestManagementPanel guestManagementPanel = new GuestManagementPanel();
        contentPanel.add(guestManagementPanel, "GUESTS");
        BookingManagementPanel bookingManagementPanel = new BookingManagementPanel();
        contentPanel.add(bookingManagementPanel, "BOOKINGS");
        guestManagementPanel.setOnBookForGuest(guest -> {
            cardLayout.show(contentPanel, "BOOKINGS");
            bookingManagementPanel.selectGuest(guest.getId());
        });
        guestManagementPanel.setOnViewGuestBookings(guest -> {
            cardLayout.show(contentPanel, "BOOKINGS");
            bookingManagementPanel.showBookingsForGuest(guest.getId(), guest.getFullName());
        });
        // Member 2: Room Management module
        RoomManagementPanel roomManagementPanel = new RoomManagementPanel();
        contentPanel.add(roomManagementPanel, "ROOMS");
        // Billing module
        BillingAndReportsPanel billingAndReportsPanel = new BillingAndReportsPanel();
        contentPanel.add(billingAndReportsPanel, "BILLING");
        contentPanel.add(createPlaceholderPanel("Hotel Services",
                "Teammate Module: Housekeeping tasks and room service orders."), "SERVICES");

        add(contentPanel, BorderLayout.CENTER);

        // 4. Bottom Status Bar
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(new EmptyBorder(6, 12, 6, 12));
        statusBar.setBackground(new Color(245, 247, 250));

        JLabel infoLabel = new JLabel("Hotel Management System v1.0 | S3 CSE B");
        infoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        infoLabel.setForeground(Color.GRAY);
        statusBar.add(infoLabel, BorderLayout.WEST);

        dbStatusLabel = new JLabel("\u25cf Database: Checking...");
        dbStatusLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        updateDbStatus(false);
        statusBar.add(dbStatusLabel, BorderLayout.EAST);

        add(statusBar, BorderLayout.SOUTH);

        // Asynchronously check DB status so UI opens instantly without freeze
        checkDatabaseStatusAsync();
    }

    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(24, 43, 73)); // Deep navy blue
        panel.setBorder(new EmptyBorder(14, 20, 14, 20));

        JLabel titleLabel = new JLabel("GRAND HORIZON HOTEL");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setForeground(Color.WHITE);

        JLabel subtitleLabel = new JLabel("Management & Reservation System");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitleLabel.setForeground(new Color(212, 175, 55)); // Elegant gold accent

        JPanel textPanel = new JPanel(new GridLayout(2, 1));
        textPanel.setOpaque(false);
        textPanel.add(titleLabel);
        textPanel.add(subtitleLabel);

        panel.add(textPanel, BorderLayout.WEST);

        // Right side: Logged in user info & Logout
        JPanel userPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 4));
        userPanel.setOpaque(false);

        User currentUser = UserSession.getCurrentUser();
        String roleStr = currentUser != null ? (" (" + currentUser.getRole() + ")") : " (STAFF)";
        String nameStr = currentUser != null ? currentUser.getFullName() : "Administrator";

        JLabel lblUser = new JLabel("👤 " + nameStr + roleStr);
        lblUser.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblUser.setForeground(Color.WHITE);
        userPanel.add(lblUser);

        JButton btnLogout = new JButton("🚪 Logout");
        btnLogout.setBackground(new Color(192, 57, 43));
        btnLogout.setForeground(Color.WHITE);
        btnLogout.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnLogout.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnLogout.addActionListener(this::onLogout);
        userPanel.add(btnLogout);

        panel.add(userPanel, BorderLayout.EAST);

        return panel;
    }

    private JPanel createSidebarPanel() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(new Color(240, 243, 246));
        sidebar.setBorder(new EmptyBorder(15, 10, 15, 10));
        sidebar.setPreferredSize(new Dimension(220, 0));

        JLabel navTitle = new JLabel("NAVIGATION");
        navTitle.setFont(new Font("Segoe UI", Font.BOLD, 11));
        navTitle.setForeground(new Color(120, 130, 140));
        navTitle.setBorder(new EmptyBorder(5, 10, 10, 10));
        sidebar.add(navTitle);

        sidebar.add(createNavButton("\uD83C\uDFE0 Dashboard", "WELCOME"));
        sidebar.add(Box.createVerticalStrut(6));

        JLabel hotelMgmtLabel = new JLabel("HOTEL MANAGEMENT");
        hotelMgmtLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        hotelMgmtLabel.setForeground(new Color(30, 100, 180));
        hotelMgmtLabel.setBorder(new EmptyBorder(15, 10, 6, 10));
        sidebar.add(hotelMgmtLabel);

        sidebar.add(createNavButton("\uD83D\uDC64 Guest Management", "GUESTS"));
        sidebar.add(Box.createVerticalStrut(4));
        sidebar.add(createNavButton("\uD83D\uDCC5 Booking Management", "BOOKINGS"));
        sidebar.add(Box.createVerticalStrut(4));
        sidebar.add(createNavButton("\uD83D\uDECF\uFE0F Rooms", "ROOMS"));
        sidebar.add(Box.createVerticalStrut(4));
        sidebar.add(createNavButton("\uD83D\uDCB3 Billing & Payments", "BILLING"));
        sidebar.add(Box.createVerticalStrut(4));
        sidebar.add(createNavButton("\uD83E\uDDF9 Services", "SERVICES"));

        sidebar.add(Box.createVerticalGlue()); // Push items to top

        return sidebar;
    }

    private JButton createNavButton(String text, String cardName) {
        JButton btn = new JButton(text);
        btn.setMaximumSize(new Dimension(200, 38));
        btn.setPreferredSize(new Dimension(200, 38));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addActionListener((ActionEvent e) -> cardLayout.show(contentPanel, cardName));
        return btn;
    }

    private JPanel createWelcomePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(30, 40, 30, 40));

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);

        JLabel welcomeTitle = new JLabel("Welcome to Grand Horizon Hotel");
        welcomeTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));
        welcomeTitle.setForeground(new Color(24, 43, 73));
        content.add(welcomeTitle);

        content.add(Box.createVerticalStrut(10));

        JLabel desc = new JLabel("<html>This desktop application manages hotel operations, customer registrations, and room bookings.<br>"
                + "Developed using <b>Java Swing</b>, <b>JDBC</b>, and <b>MySQL</b>.</html>");
        desc.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        desc.setForeground(new Color(70, 80, 90));
        content.add(desc);

        content.add(Box.createVerticalStrut(30));

        // Quick feature summary cards
        JPanel cardsGrid = new JPanel(new GridLayout(1, 3, 15, 15));
        cardsGrid.setOpaque(false);
        cardsGrid.setMaximumSize(new Dimension(900, 140));

        cardsGrid.add(createSummaryBox("Guest Management", "Register guests, track phone & IDs, and maintain stay histories.", new Color(41, 128, 185)));
        cardsGrid.add(createSummaryBox("Booking & 5-Min Hold", "Check room availability, prevent date overlaps, and lock rooms temporarily.", new Color(39, 174, 96)));
        cardsGrid.add(createSummaryBox("System Architecture", "Shared MySQL database, JDBC PreparedStatements, and clean OOP design.", new Color(142, 68, 173)));

        content.add(cardsGrid);

        panel.add(content, BorderLayout.NORTH);
        return panel;
    }

    private JPanel createSummaryBox(String title, String text, Color accent) {
        JPanel box = new JPanel(new BorderLayout(5, 8));
        box.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(225, 230, 235), 1),
                new EmptyBorder(12, 14, 12, 14)
        ));
        box.setBackground(new Color(250, 251, 253));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        titleLbl.setForeground(accent);

        JLabel textLbl = new JLabel("<html>" + text + "</html>");
        textLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        textLbl.setForeground(Color.DARK_GRAY);

        box.add(titleLbl, BorderLayout.NORTH);
        box.add(textLbl, BorderLayout.CENTER);
        return box;
    }

    private JPanel createPlaceholderPanel(String title, String description) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setOpaque(false);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel descLabel = new JLabel("<html><center>" + description.replace("\n", "<br>") + "</center></html>");
        descLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        descLabel.setForeground(Color.GRAY);
        descLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        inner.add(titleLabel);
        inner.add(Box.createVerticalStrut(10));
        inner.add(descLabel);

        panel.add(inner);
        return panel;
    }

    private void checkDatabaseStatusAsync() {
        dbStatusLabel.setText("\u25cf Database: Checking...");
        dbStatusLabel.setForeground(new Color(200, 150, 0));

        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() {
                return DatabaseConnection.testConnection();
            }

            @Override
            protected void done() {
                try {
                    boolean connected = get();
                    updateDbStatus(connected);
                } catch (Exception e) {
                    updateDbStatus(false);
                }
            }
        };
        worker.execute();
    }

    private void updateDbStatus(boolean connected) {
        if (connected) {
            dbStatusLabel.setText("\u25cf Database: Connected (hotel_management)");
            dbStatusLabel.setForeground(new Color(34, 139, 34)); // Forest green
        } else {
            dbStatusLabel.setText("\u25cf Database: Offline (MySQL Not Running)");
            dbStatusLabel.setForeground(new Color(180, 40, 40)); // Crimson red
        }
    }

    private void onLogout(ActionEvent e) {
        int res = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to log out of the Management Portal?",
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

    public static void main(String[] args) {
        // Apply FlatLaf modern Look & Feel
        try {
            FlatLightLaf.setup();
        } catch (Exception e) {
            System.err.println("Could not setup FlatLaf: " + e.getMessage());
        }

        SwingUtilities.invokeLater(() -> {
            MainAppFrame frame = new MainAppFrame();
            frame.setVisible(true);
        });
    }
}
