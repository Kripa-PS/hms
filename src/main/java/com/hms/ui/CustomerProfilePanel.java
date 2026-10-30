package com.hms.ui;

import com.hms.dao.GuestDAO;
import com.hms.dao.GuestDAOImpl;
import com.hms.dao.UserDAO;
import com.hms.dao.UserDAOImpl;
import com.hms.model.Guest;
import com.hms.model.User;
import com.hms.util.UserSession;
import com.hms.util.ValidationUtil;
import com.hms.util.ValidationUtil.ValidationResult;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.Optional;

/**
 * CustomerProfilePanel
 * 
 * Allows customers to review and manage their account profile and guest records.
 * Uses strict validation rules from ValidationUtil.
 */
public class CustomerProfilePanel extends JPanel {

    private final UserDAO userDAO;
    private final GuestDAO guestDAO;

    private JTextField usernameField;
    private JTextField fullNameField;
    private JTextField phoneField;
    private JTextField emailField;
    private JComboBox<String> idTypeCombo;
    private JTextField idNumberField;
    private JTextField addressField;

    private JButton btnSave;
    private JLabel lblStatus;

    public CustomerProfilePanel() {
        this.userDAO = new UserDAOImpl();
        this.guestDAO = new GuestDAOImpl();

        setLayout(new BorderLayout(15, 15));
        setBorder(new EmptyBorder(15, 20, 15, 20));
        setBackground(Color.WHITE);

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JLabel heading = new JLabel("My Account Profile");
        heading.setFont(new Font("Segoe UI", Font.BOLD, 20));
        heading.setForeground(new Color(24, 43, 73));
        headerPanel.add(heading, BorderLayout.WEST);

        add(headerPanel, BorderLayout.NORTH);

        // Center Form
        JPanel formContainer = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        formContainer.setOpaque(false);

        JPanel formCard = new JPanel(new BorderLayout(10, 15));
        formCard.setOpaque(false);
        formCard.setPreferredSize(new Dimension(550, 480));
        formCard.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(220, 225, 230), 1),
                " Personal Information & ID Proof ", TitledBorder.LEADING, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 13), new Color(24, 43, 73)));

        JPanel grid = new JPanel(new GridLayout(7, 2, 10, 14));
        grid.setOpaque(false);
        grid.setBorder(new EmptyBorder(15, 20, 15, 20));

        grid.add(new JLabel("Username:"));
        usernameField = new JTextField();
        usernameField.setEditable(false);
        usernameField.setBackground(new Color(245, 247, 250));
        grid.add(usernameField);

        grid.add(new JLabel("Full Name *:"));
        fullNameField = new JTextField();
        grid.add(fullNameField);

        grid.add(new JLabel("Phone Number (10 digits) *:"));
        phoneField = new JTextField();
        grid.add(phoneField);

        grid.add(new JLabel("Email Address *:"));
        emailField = new JTextField();
        grid.add(emailField);

        grid.add(new JLabel("ID Proof Type:"));
        idTypeCombo = new JComboBox<>(new String[]{"Aadhaar", "Passport", "Driving License", "Voter ID"});
        grid.add(idTypeCombo);

        grid.add(new JLabel("ID Proof Number:"));
        idNumberField = new JTextField();
        grid.add(idNumberField);

        grid.add(new JLabel("City / Address:"));
        addressField = new JTextField();
        grid.add(addressField);

        formCard.add(grid, BorderLayout.CENTER);

        JPanel bottomBtn = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 10));
        bottomBtn.setOpaque(false);

        lblStatus = new JLabel(" ");
        lblStatus.setFont(new Font("Segoe UI", Font.BOLD, 12));
        bottomBtn.add(lblStatus);

        btnSave = new JButton("💾 Save Profile Changes");
        btnSave.setBackground(new Color(39, 174, 96));
        btnSave.setForeground(Color.WHITE);
        btnSave.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnSave.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSave.addActionListener(e -> onSaveProfile());
        bottomBtn.add(btnSave);

        formCard.add(bottomBtn, BorderLayout.SOUTH);

        formContainer.add(formCard);
        add(formContainer, BorderLayout.CENTER);

        // Load profile data
        loadProfileData();
    }

    private void loadProfileData() {
        User user = UserSession.getCurrentUser();
        if (user == null) return;

        usernameField.setText(user.getUsername());
        fullNameField.setText(user.getFullName() != null ? user.getFullName() : "");

        if (user.getGuestId() != null && user.getGuestId() > 0) {
            SwingWorker<Optional<Guest>, Void> worker = new SwingWorker<>() {
                @Override
                protected Optional<Guest> doInBackground() {
                    return guestDAO.findById(user.getGuestId());
                }

                @Override
                protected void done() {
                    try {
                        Optional<Guest> gOpt = get();
                        if (gOpt.isPresent()) {
                            Guest g = gOpt.get();
                            phoneField.setText(g.getPhone() != null ? g.getPhone() : "");
                            emailField.setText(g.getEmail() != null ? g.getEmail() : "");
                            if (g.getIdProofType() != null) {
                                idTypeCombo.setSelectedItem(g.getIdProofType());
                            }
                            idNumberField.setText(g.getIdProofNumber() != null ? g.getIdProofNumber() : "");
                            addressField.setText(g.getAddress() != null ? g.getAddress() : "");
                        }
                    } catch (Exception ex) {
                        lblStatus.setText("Could not load guest details: " + ex.getMessage());
                    }
                }
            };
            worker.execute();
        }
    }

    private void onSaveProfile() {
        User user = UserSession.getCurrentUser();
        if (user == null) return;

        String fullName = fullNameField.getText().trim();
        String phone = phoneField.getText().trim();
        String email = emailField.getText().trim();
        String idType = (String) idTypeCombo.getSelectedItem();
        String idNumber = idNumberField.getText().trim();
        String address = addressField.getText().trim();

        if (fullName.isEmpty()) {
            lblStatus.setForeground(new Color(192, 57, 43));
            lblStatus.setText("Full name is required.");
            fullNameField.requestFocus();
            return;
        }

        ValidationResult phoneRes = ValidationUtil.validatePhone(phone);
        if (!phoneRes.isValid()) {
            lblStatus.setForeground(new Color(192, 57, 43));
            lblStatus.setText(phoneRes.getMessage());
            phoneField.requestFocus();
            return;
        }

        ValidationResult emailRes = ValidationUtil.validateEmail(email, true);
        if (!emailRes.isValid()) {
            lblStatus.setForeground(new Color(192, 57, 43));
            lblStatus.setText(emailRes.getMessage());
            emailField.requestFocus();
            return;
        }

        if (!idNumber.isEmpty()) {
            ValidationResult idRes = ValidationUtil.validateIdProof(idType, idNumber, false);
            if (!idRes.isValid()) {
                lblStatus.setForeground(new Color(192, 57, 43));
                lblStatus.setText(idRes.getMessage());
                idNumberField.requestFocus();
                return;
            }
        }

        lblStatus.setForeground(new Color(41, 128, 185));
        lblStatus.setText("Saving profile changes...");
        btnSave.setEnabled(false);

        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() {
                return userDAO.updateCustomerProfile(user.getId(), fullName, phone, email, idType, idNumber, address);
            }

            @Override
            protected void done() {
                btnSave.setEnabled(true);
                try {
                    boolean ok = get();
                    if (ok) {
                        lblStatus.setForeground(new Color(39, 174, 96));
                        lblStatus.setText("Profile updated successfully!");
                    } else {
                        lblStatus.setForeground(new Color(192, 57, 43));
                        lblStatus.setText("Update failed. Check data format.");
                    }
                } catch (Exception ex) {
                    lblStatus.setForeground(new Color(192, 57, 43));
                    lblStatus.setText("Error: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }
}
