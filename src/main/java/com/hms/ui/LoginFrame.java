package com.hms.ui;

import com.formdev.flatlaf.FlatLightLaf;
import com.hms.dao.UserDAO;
import com.hms.dao.UserDAOImpl;
import com.hms.model.User;
import com.hms.util.DatabaseConnection;
import com.hms.util.UserSession;
import com.hms.util.ValidationUtil;
import com.hms.util.ValidationUtil.ValidationResult;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Optional;

/**
 * LoginFrame
 * 
 * Professional authentication window for the Hotel Management System.
 * Supports Admin/Staff Sign In, Customer Sign In, and Customer Registration.
 */
public class LoginFrame extends JFrame {

    private final UserDAO userDAO;

    // Login components
    private JTextField loginUsernameField;
    private JPasswordField loginPasswordField;
    private JButton btnSignIn;
    private JLabel lblLoginStatus;

    // Registration components
    private JTextField regUsernameField;
    private JPasswordField regPasswordField;
    private JPasswordField regConfirmPasswordField;
    private JTextField regFullNameField;
    private JTextField regPhoneField;
    private JTextField regEmailField;
    private JComboBox<String> regIdTypeCombo;
    private JTextField regIdNumberField;
    private JTextField regAddressField;
    private JButton btnRegister;
    private JLabel lblRegStatus;

    private JTabbedPane tabbedPane;

    public LoginFrame() {
        this.userDAO = new UserDAOImpl();

        setTitle("Grand Horizon Hotel - Login & Registration");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(520, 680);
        setMinimumSize(new Dimension(480, 600));
        setLocationRelativeTo(null);
        setResizable(false);

        setLayout(new BorderLayout());

        // Header Panel
        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);

        // Center Tabbed Pane (Sign In vs Register)
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 13));

        JPanel signInPanel = createSignInPanel();
        tabbedPane.addTab("🔑 Sign In", signInPanel);

        JPanel registerPanel = createRegisterPanel();
        tabbedPane.addTab("📝 Customer Registration", registerPanel);

        add(tabbedPane, BorderLayout.CENTER);

        // Footer status bar
        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        footerPanel.setBackground(new Color(245, 247, 250));
        footerPanel.setBorder(new EmptyBorder(8, 10, 8, 10));
        JLabel footerLabel = new JLabel("Grand Horizon Hotel System | S3 CSE B");
        footerLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        footerLabel.setForeground(Color.GRAY);
        footerPanel.add(footerLabel);
        add(footerPanel, BorderLayout.SOUTH);

        // Asynchronously check DB
        checkDbStatusAsync();
    }

    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(24, 43, 73)); // Deep navy
        panel.setBorder(new EmptyBorder(20, 25, 20, 25));

        JLabel titleLabel = new JLabel("GRAND HORIZON HOTEL");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel subtitleLabel = new JLabel("Authentication & Guest Portal");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitleLabel.setForeground(new Color(212, 175, 55)); // Gold
        subtitleLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel textPanel = new JPanel(new GridLayout(2, 1, 0, 4));
        textPanel.setOpaque(false);
        textPanel.add(titleLabel);
        textPanel.add(subtitleLabel);

        panel.add(textPanel, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createSignInPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(25, 35, 25, 35));

        JPanel form = new JPanel(new GridLayout(5, 1, 0, 10));
        form.setOpaque(false);

        // Username
        JPanel userPanel = new JPanel(new BorderLayout(0, 4));
        userPanel.setOpaque(false);
        JLabel userLbl = new JLabel("Username / Account ID:");
        userLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        loginUsernameField = new JTextField();
        loginUsernameField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        userPanel.add(userLbl, BorderLayout.NORTH);
        userPanel.add(loginUsernameField, BorderLayout.CENTER);
        form.add(userPanel);

        // Password
        JPanel passPanel = new JPanel(new BorderLayout(0, 4));
        passPanel.setOpaque(false);
        JLabel passLbl = new JLabel("Password:");
        passLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        loginPasswordField = new JPasswordField();
        loginPasswordField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        passPanel.add(passLbl, BorderLayout.NORTH);
        passPanel.add(loginPasswordField, BorderLayout.CENTER);
        form.add(passPanel);

        // Quick hint
        JLabel hintLbl = new JLabel("<html><font color='#7f8c8d'>Staff Demo: <b>admin</b> / <b>admin123</b><br>Customer: Use registered credentials</font></html>");
        hintLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        form.add(hintLbl);

        // Status label
        lblLoginStatus = new JLabel(" ");
        lblLoginStatus.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblLoginStatus.setForeground(new Color(192, 57, 43));
        form.add(lblLoginStatus);

        // Sign In Button
        btnSignIn = new JButton("Sign In");
        btnSignIn.setBackground(new Color(24, 43, 73));
        btnSignIn.setForeground(Color.WHITE);
        btnSignIn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnSignIn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSignIn.setPreferredSize(new Dimension(0, 42));
        btnSignIn.addActionListener(this::onSignIn);
        form.add(btnSignIn);

        loginPasswordField.addActionListener(this::onSignIn);
        loginUsernameField.addActionListener(e -> loginPasswordField.requestFocusInWindow());

        panel.add(form, BorderLayout.NORTH);

        // Bottom link to registration
        JPanel switchPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        switchPanel.setOpaque(false);
        JButton btnGoToReg = new JButton("New customer? Create an account");
        btnGoToReg.setBorderPainted(false);
        btnGoToReg.setContentAreaFilled(false);
        btnGoToReg.setForeground(new Color(41, 128, 185));
        btnGoToReg.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnGoToReg.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnGoToReg.addActionListener(e -> tabbedPane.setSelectedIndex(1));
        switchPanel.add(btnGoToReg);

        panel.add(switchPanel, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createRegisterPanel() {
        JPanel scrollContent = new JPanel(new BorderLayout());
        scrollContent.setBackground(Color.WHITE);
        scrollContent.setBorder(new EmptyBorder(15, 25, 15, 25));

        JPanel form = new JPanel(new GridLayout(9, 2, 8, 10));
        form.setOpaque(false);

        form.add(new JLabel("Desired Username *:"));
        regUsernameField = new JTextField();
        form.add(regUsernameField);

        form.add(new JLabel("Full Name *:"));
        regFullNameField = new JTextField();
        form.add(regFullNameField);

        form.add(new JLabel("Phone (10 digits) *:"));
        regPhoneField = new JTextField();
        form.add(regPhoneField);

        form.add(new JLabel("Email Address *:"));
        regEmailField = new JTextField();
        form.add(regEmailField);

        form.add(new JLabel("Password (min 6 chars) *:"));
        regPasswordField = new JPasswordField();
        form.add(regPasswordField);

        form.add(new JLabel("Confirm Password *:"));
        regConfirmPasswordField = new JPasswordField();
        form.add(regConfirmPasswordField);

        form.add(new JLabel("ID Proof Type:"));
        regIdTypeCombo = new JComboBox<>(new String[]{"Aadhaar", "Passport", "Driving License", "Voter ID"});
        form.add(regIdTypeCombo);

        form.add(new JLabel("ID Proof Number:"));
        regIdNumberField = new JTextField();
        form.add(regIdNumberField);

        form.add(new JLabel("City / Address:"));
        regAddressField = new JTextField();
        form.add(regAddressField);

        scrollContent.add(form, BorderLayout.NORTH);

        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(new EmptyBorder(10, 0, 5, 0));

        lblRegStatus = new JLabel(" ");
        lblRegStatus.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblRegStatus.setForeground(new Color(192, 57, 43));
        lblRegStatus.setAlignmentX(Component.CENTER_ALIGNMENT);
        bottomPanel.add(lblRegStatus);
        bottomPanel.add(Box.createVerticalStrut(6));

        btnRegister = new JButton("Register & Sign In");
        btnRegister.setBackground(new Color(39, 174, 96));
        btnRegister.setForeground(Color.WHITE);
        btnRegister.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnRegister.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnRegister.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnRegister.setMaximumSize(new Dimension(300, 38));
        btnRegister.addActionListener(this::onRegister);
        bottomPanel.add(btnRegister);

        scrollContent.add(bottomPanel, BorderLayout.SOUTH);

        JScrollPane scrollPane = new JScrollPane(scrollContent);
        scrollPane.setBorder(null);
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(scrollPane, BorderLayout.CENTER);
        return wrapper;
    }

    private void onSignIn(ActionEvent e) {
        String username = loginUsernameField.getText().trim();
        String password = new String(loginPasswordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            lblLoginStatus.setText("Please enter both username and password.");
            return;
        }

        lblLoginStatus.setForeground(new Color(41, 128, 185));
        lblLoginStatus.setText("Authenticating...");
        btnSignIn.setEnabled(false);

        SwingWorker<Optional<User>, Void> worker = new SwingWorker<>() {
            @Override
            protected Optional<User> doInBackground() {
                return userDAO.authenticate(username, password);
            }

            @Override
            protected void done() {
                btnSignIn.setEnabled(true);
                try {
                    Optional<User> userOpt = get();
                    if (userOpt.isPresent()) {
                        User user = userOpt.get();
                        UserSession.login(user);
                        openAppropriateDashboard(user);
                    } else {
                        lblLoginStatus.setForeground(new Color(192, 57, 43));
                        lblLoginStatus.setText("Invalid username or password.");
                    }
                } catch (Exception ex) {
                    lblLoginStatus.setForeground(new Color(192, 57, 43));
                    lblLoginStatus.setText("Database error: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void onRegister(ActionEvent e) {
        String username = regUsernameField.getText().trim();
        String password = new String(regPasswordField.getPassword());
        String confirmPass = new String(regConfirmPasswordField.getPassword());
        String fullName = regFullNameField.getText().trim();
        String phone = regPhoneField.getText().trim();
        String email = regEmailField.getText().trim();
        String idType = (String) regIdTypeCombo.getSelectedItem();
        String idNumber = regIdNumberField.getText().trim();
        String address = regAddressField.getText().trim();

        // Validations
        if (username.length() < 3) {
            lblRegStatus.setText("Username must be at least 3 characters.");
            return;
        }
        if (fullName.isEmpty()) {
            lblRegStatus.setText("Full Name is required.");
            return;
        }
        if (password.length() < 6) {
            lblRegStatus.setText("Password must be at least 6 characters.");
            return;
        }
        if (!password.equals(confirmPass)) {
            lblRegStatus.setText("Passwords do not match.");
            return;
        }

        ValidationResult phoneRes = ValidationUtil.validatePhone(phone);
        if (!phoneRes.isValid()) {
            lblRegStatus.setText(phoneRes.getMessage());
            return;
        }

        ValidationResult emailRes = ValidationUtil.validateEmail(email, true);
        if (!emailRes.isValid()) {
            lblRegStatus.setText(emailRes.getMessage());
            return;
        }

        if (!idNumber.isEmpty()) {
            ValidationResult idRes = ValidationUtil.validateIdProof(idType, idNumber, false);
            if (!idRes.isValid()) {
                lblRegStatus.setText(idRes.getMessage());
                return;
            }
        }

        lblRegStatus.setForeground(new Color(41, 128, 185));
        lblRegStatus.setText("Creating customer account...");
        btnRegister.setEnabled(false);

        SwingWorker<User, Void> worker = new SwingWorker<>() {
            @Override
            protected User doInBackground() {
                return userDAO.registerCustomer(username, password, fullName, phone, email, idType, idNumber, address);
            }

            @Override
            protected void done() {
                btnRegister.setEnabled(true);
                try {
                    User newUser = get();
                    JOptionPane.showMessageDialog(LoginFrame.this,
                            "Registration successful! Welcome, " + newUser.getFullName() + ".",
                            "Registration Complete", JOptionPane.INFORMATION_MESSAGE);
                    UserSession.login(newUser);
                    openAppropriateDashboard(newUser);
                } catch (Exception ex) {
                    lblRegStatus.setForeground(new Color(192, 57, 43));
                    Throwable cause = (ex.getCause() != null) ? ex.getCause() : ex;
                    lblRegStatus.setText(cause.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void openAppropriateDashboard(User user) {
        dispose(); // Close login window

        SwingUtilities.invokeLater(() -> {
            if (user.isAdmin() || user.isReceptionist()) {
                MainAppFrame adminFrame = new MainAppFrame();
                adminFrame.setVisible(true);
            } else {
                CustomerAppFrame customerFrame = new CustomerAppFrame();
                customerFrame.setVisible(true);
            }
        });
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
                        lblLoginStatus.setForeground(new Color(192, 57, 43));
                        lblLoginStatus.setText("● Database Offline (Check MySQL Server)");
                    }
                } catch (Exception e) {
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
            // Fallback
        }

        SwingUtilities.invokeLater(() -> {
            LoginFrame frame = new LoginFrame();
            frame.setVisible(true);
        });
    }
}
