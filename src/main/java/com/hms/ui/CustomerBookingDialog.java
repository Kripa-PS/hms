package com.hms.ui;

import com.hms.dao.*;
import com.hms.model.*;
import com.hms.util.BillCalculator;
import com.hms.util.InvoiceRenderer;
import com.hms.util.ValidationUtil;
import com.hms.util.ValidationUtil.ValidationResult;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * CustomerBookingDialog
 * 
 * Interactive customer booking modal with active 5-minute room hold countdown.
 * Calculates countdown from the actual database expires_at timestamp.
 * Integrates guest verification, BillCalculator breakdown, demo payment simulation,
 * atomic hold consumption, and invoice generation.
 */
public class CustomerBookingDialog extends JDialog {

    private final Room room;
    private final RoomHold hold;
    private final LocalDate checkIn;
    private final LocalDate checkOut;
    private final int guestCount;
    private final Guest currentGuest;

    private final BookingDAO bookingDAO;
    private final PaymentDAO paymentDAO;
    private final InvoiceDAO invoiceDAO;
    private final RoomHoldDAO roomHoldDAO;
    private final GuestDAO guestDAO;

    private final Runnable onSuccessCallback;

    // Countdown UI
    private JPanel bannerPanel;
    private JLabel lblCountdown;
    private JLabel lblBannerSub;
    private Timer countdownTimer;
    private boolean bookingCompleted = false;

    // Guest fields
    private JTextField nameField;
    private JTextField phoneField;
    private JTextField emailField;
    private JComboBox<String> idTypeCombo;
    private JTextField idNumberField;
    private JTextField addressField;

    // Payment fields
    private JComboBox<String> paymentMethodCombo;
    private JComboBox<String> simStatusCombo;
    private JButton btnPay;
    private JButton btnCancel;

    private final BigDecimal totalAmount;
    private final long nights;

    public CustomerBookingDialog(Window owner, Room room, RoomHold hold,
                                 LocalDate checkIn, LocalDate checkOut,
                                 int guestCount, Guest currentGuest,
                                 Runnable onSuccessCallback) {
        super(owner, "Complete Reservation - Room " + room.getRoomNumber(), ModalityType.APPLICATION_MODAL);

        this.room = room;
        this.hold = hold;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.guestCount = guestCount;
        this.currentGuest = currentGuest;
        this.onSuccessCallback = onSuccessCallback;

        this.bookingDAO = new BookingDAOImpl();
        this.paymentDAO = new PaymentDAOImpl();
        this.invoiceDAO = new InvoiceDAOImpl();
        this.roomHoldDAO = new RoomHoldDAOImpl();
        this.guestDAO = new GuestDAOImpl();

        this.nights = BillCalculator.calculateNights(checkIn, checkOut);
        this.totalAmount = BillCalculator.calculateRoomCharge(room.getPricePerNight(), nights);

        setSize(650, 750);
        setMinimumSize(new Dimension(580, 650));
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());

        // 1. Top Hold Countdown Banner
        bannerPanel = createBannerPanel();
        add(bannerPanel, BorderLayout.NORTH);

        // 2. Center Content with ScrollPane
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBorder(new EmptyBorder(15, 20, 15, 20));
        centerPanel.setBackground(Color.WHITE);

        centerPanel.add(createSummaryCard());
        centerPanel.add(Box.createVerticalStrut(15));
        centerPanel.add(createGuestCard());
        centerPanel.add(Box.createVerticalStrut(15));
        centerPanel.add(createPaymentCard());

        JScrollPane scrollPane = new JScrollPane(centerPanel);
        scrollPane.setBorder(null);
        add(scrollPane, BorderLayout.CENTER);

        // 3. Bottom Action Bar
        JPanel bottomBar = createBottomBar();
        add(bottomBar, BorderLayout.SOUTH);

        // Window closing listener to safely release hold if user closes
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cancelAndClose();
            }
        });

        // Start countdown timer
        startCountdown();
    }

    private JPanel createBannerPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 5));
        panel.setBackground(new Color(243, 156, 18)); // Soft warning amber
        panel.setBorder(new EmptyBorder(12, 20, 12, 20));

        JPanel left = new JPanel(new GridLayout(2, 1, 2, 2));
        left.setOpaque(false);

        JLabel title = new JLabel("5-MINUTE ROOM HOLD ACTIVE");
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));
        title.setForeground(Color.WHITE);

        lblBannerSub = new JLabel("Your room is held. Complete payment before the timer expires.");
        lblBannerSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblBannerSub.setForeground(new Color(255, 245, 230));

        left.add(title);
        left.add(lblBannerSub);
        panel.add(left, BorderLayout.WEST);

        lblCountdown = new JLabel("05:00", SwingConstants.RIGHT);
        lblCountdown.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblCountdown.setForeground(Color.WHITE);
        panel.add(lblCountdown, BorderLayout.EAST);

        return panel;
    }

    private JPanel createSummaryCard() {
        JPanel card = new JPanel(new BorderLayout(10, 10));
        card.setOpaque(false);
        card.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(220, 225, 230), 1),
                " 1. Room & Stay Summary ", TitledBorder.LEADING, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 13), new Color(24, 43, 73)));

        JPanel grid = new JPanel(new GridLayout(4, 2, 8, 8));
        grid.setOpaque(false);
        grid.setBorder(new EmptyBorder(10, 15, 10, 15));

        grid.add(new JLabel("Room:"));
        grid.add(new JLabel("Room " + room.getRoomNumber() + " (" + room.getRoomType() + ")"));

        grid.add(new JLabel("Stay Dates:"));
        grid.add(new JLabel(checkIn + " to " + checkOut + " (" + nights + " night" + (nights > 1 ? "s" : "") + ")"));

        grid.add(new JLabel("Room Rate:"));
        grid.add(new JLabel("Rs. " + room.getPricePerNight().setScale(2, RoundingMode.HALF_UP) + " / night"));

        JLabel totalLabel = new JLabel("Total Bill Amount:");
        totalLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        grid.add(totalLabel);

        JLabel totalValue = new JLabel("Rs. " + totalAmount.setScale(2, RoundingMode.HALF_UP));
        totalValue.setFont(new Font("Segoe UI", Font.BOLD, 15));
        totalValue.setForeground(new Color(39, 174, 96));
        grid.add(totalValue);

        card.add(grid, BorderLayout.CENTER);
        return card;
    }

    private JPanel createGuestCard() {
        JPanel card = new JPanel(new BorderLayout(10, 10));
        card.setOpaque(false);
        card.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(220, 225, 230), 1),
                " 2. Guest Information ", TitledBorder.LEADING, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 13), new Color(24, 43, 73)));

        JPanel grid = new JPanel(new GridLayout(6, 2, 8, 8));
        grid.setOpaque(false);
        grid.setBorder(new EmptyBorder(10, 15, 10, 15));

        grid.add(new JLabel("Full Name *:"));
        nameField = new JTextField(currentGuest != null ? currentGuest.getFullName() : "");
        grid.add(nameField);

        grid.add(new JLabel("Phone (10 digits) *:"));
        phoneField = new JTextField(currentGuest != null ? currentGuest.getPhone() : "");
        grid.add(phoneField);

        grid.add(new JLabel("Email Address *:"));
        emailField = new JTextField(currentGuest != null && currentGuest.getEmail() != null ? currentGuest.getEmail() : "");
        grid.add(emailField);

        grid.add(new JLabel("ID Proof Type:"));
        idTypeCombo = new JComboBox<>(new String[]{"Aadhaar", "Passport", "Driving License", "Voter ID"});
        if (currentGuest != null && currentGuest.getIdProofType() != null) {
            idTypeCombo.setSelectedItem(currentGuest.getIdProofType());
        }
        grid.add(idTypeCombo);

        grid.add(new JLabel("ID Proof Number:"));
        idNumberField = new JTextField(currentGuest != null && currentGuest.getIdProofNumber() != null ? currentGuest.getIdProofNumber() : "");
        grid.add(idNumberField);

        grid.add(new JLabel("Address / City:"));
        addressField = new JTextField(currentGuest != null && currentGuest.getAddress() != null ? currentGuest.getAddress() : "");
        grid.add(addressField);

        card.add(grid, BorderLayout.CENTER);
        return card;
    }

    private JPanel createPaymentCard() {
        JPanel card = new JPanel(new BorderLayout(10, 10));
        card.setOpaque(false);
        card.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(220, 225, 230), 1),
                " 3. Payment Simulation (Demonstration Mode) ", TitledBorder.LEADING, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 13), new Color(24, 43, 73)));

        JPanel grid = new JPanel(new GridLayout(3, 2, 8, 10));
        grid.setOpaque(false);
        grid.setBorder(new EmptyBorder(10, 15, 10, 15));

        grid.add(new JLabel("Payment Method:"));
        paymentMethodCombo = new JComboBox<>(new String[]{"UPI", "CARD", "CASH"});
        grid.add(paymentMethodCombo);

        grid.add(new JLabel("Demo Simulation Mode:"));
        simStatusCombo = new JComboBox<>(new String[]{
                "Simulate SUCCESS (Status: PAID)",
                "Simulate FAILURE (Status: FAILED)"
        });
        grid.add(simStatusCombo);

        grid.add(new JLabel("Amount to Pay:"));
        JLabel amt = new JLabel("Rs. " + totalAmount.setScale(2, RoundingMode.HALF_UP));
        amt.setFont(new Font("Segoe UI", Font.BOLD, 14));
        amt.setForeground(new Color(41, 128, 185));
        grid.add(amt);

        card.add(grid, BorderLayout.CENTER);
        return card;
    }

    private JPanel createBottomBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        bar.setBackground(new Color(245, 247, 250));
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(225, 230, 235)));

        btnCancel = new JButton("Cancel Reservation");
        btnCancel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCancel.addActionListener(e -> cancelAndClose());
        bar.add(btnCancel);

        btnPay = new JButton("Pay Rs. " + totalAmount.setScale(2, RoundingMode.HALF_UP) + " & Confirm");
        btnPay.setBackground(new Color(39, 174, 96));
        btnPay.setForeground(Color.WHITE);
        btnPay.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnPay.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnPay.addActionListener(e -> onPayAndConfirm());
        bar.add(btnPay);

        return bar;
    }

    private void startCountdown() {
        countdownTimer = new Timer(1000, e -> updateCountdownDisplay());
        countdownTimer.start();
        updateCountdownDisplay(); // Initial update
    }

    private void updateCountdownDisplay() {
        if (hold == null || hold.getExpiresAt() == null) {
            lblCountdown.setText("05:00");
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        long secondsRemaining = ChronoUnit.SECONDS.between(now, hold.getExpiresAt());

        if (secondsRemaining <= 0) {
            countdownTimer.stop();
            lblCountdown.setText("00:00");
            bannerPanel.setBackground(new Color(192, 57, 43)); // Dark red
            lblBannerSub.setText("Reservation expired! Releasing room to public inventory...");
            btnPay.setEnabled(false);

            // Release hold in DB
            try {
                roomHoldDAO.releaseHold(hold.getHoldToken());
            } catch (Exception ex) {
                // Ignore
            }

            JOptionPane.showMessageDialog(this,
                    "Your 5-minute reservation hold has expired.\nThe room has been released to inventory. Please search again.",
                    "Reservation Expired", JOptionPane.WARNING_MESSAGE);
            dispose();
            return;
        }

        long minutes = secondsRemaining / 60;
        long seconds = secondsRemaining % 60;
        lblCountdown.setText(String.format("%02d:%02d", minutes, seconds));

        if (secondsRemaining <= 60) {
            bannerPanel.setBackground(new Color(231, 76, 60)); // Urgent red
            lblBannerSub.setText("WARNING: Less than 1 minute remaining! Complete payment now.");
        } else {
            bannerPanel.setBackground(new Color(243, 156, 18)); // Soft warning amber
            lblBannerSub.setText("Your room is held. Complete payment before the timer expires.");
        }
    }

    private void onPayAndConfirm() {
        // Validate Guest inputs
        String name = nameField.getText().trim();
        String phone = phoneField.getText().trim();
        String email = emailField.getText().trim();
        String idType = (String) idTypeCombo.getSelectedItem();
        String idNumber = idNumberField.getText().trim();
        String address = addressField.getText().trim();

        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Guest full name is required.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            nameField.requestFocus();
            return;
        }

        ValidationResult phoneRes = ValidationUtil.validatePhone(phone);
        if (!phoneRes.isValid()) {
            JOptionPane.showMessageDialog(this, phoneRes.getMessage(), "Validation Error", JOptionPane.WARNING_MESSAGE);
            phoneField.requestFocus();
            return;
        }

        ValidationResult emailRes = ValidationUtil.validateEmail(email, true);
        if (!emailRes.isValid()) {
            JOptionPane.showMessageDialog(this, emailRes.getMessage(), "Validation Error", JOptionPane.WARNING_MESSAGE);
            emailField.requestFocus();
            return;
        }

        if (!idNumber.isEmpty()) {
            ValidationResult idRes = ValidationUtil.validateIdProof(idType, idNumber, false);
            if (!idRes.isValid()) {
                JOptionPane.showMessageDialog(this, idRes.getMessage(), "Validation Error", JOptionPane.WARNING_MESSAGE);
                idNumberField.requestFocus();
                return;
            }
        }

        // Check if demo simulation is FAILURE
        String simMode = (String) simStatusCombo.getSelectedItem();
        if (simMode != null && simMode.contains("FAILURE")) {
            JOptionPane.showMessageDialog(this,
                    "Payment Simulation Failed!\n"
                            + "Reason: Transaction declined by bank / UPI gateway timeout.\n\n"
                            + "Your 5-minute room hold remains ACTIVE! (" + lblCountdown.getText() + " remaining).\n"
                            + "You may change payment method and retry before expiry.",
                    "Payment Declined (Demo)", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // SUCCESSFUL PAYMENT FLOW
        btnPay.setEnabled(false);
        btnCancel.setEnabled(false);

        SwingWorker<Invoice, Void> worker = new SwingWorker<>() {
            @Override
            protected Invoice doInBackground() throws Exception {
                // 1. Update guest details if changed
                if (currentGuest != null) {
                    currentGuest.setFullName(name);
                    currentGuest.setPhone(phone);
                    currentGuest.setEmail(email);
                    currentGuest.setIdProofType(idType);
                    currentGuest.setIdProofNumber(idNumber);
                    currentGuest.setAddress(address);
                    guestDAO.updateGuest(currentGuest);
                }

                // 2. Create Booking atomically with hold token
                Booking booking = new Booking();
                booking.setGuestId(currentGuest != null ? currentGuest.getId() : 1);
                booking.setRoomId(room.getId());
                booking.setCheckInDate(checkIn);
                booking.setCheckOutDate(checkOut);
                booking.setGuestCount(guestCount);
                booking.setTotalAmount(totalAmount);
                booking.setStatus("CONFIRMED");

                Booking createdBooking = bookingDAO.createBooking(booking, hold.getHoldToken());

                // 3. Record Payment
                String method = (String) paymentMethodCombo.getSelectedItem();
                Payment payment = new Payment();
                payment.setBookingId(createdBooking.getId());
                payment.setAmount(totalAmount);
                payment.setPaymentMethod(method != null ? method : "UPI");
                payment.setPaymentStatus("PAID");
                payment.setTransactionReference("TXN-" + createdBooking.getId() + "-" + (System.currentTimeMillis() % 1000000));
                payment.setNotes("Customer portal instant online payment");
                paymentDAO.recordPayment(payment);

                // 4. Generate Invoice
                return invoiceDAO.generateInvoice(createdBooking.getId(), "Customer Portal Instant Reservation");
            }

            @Override
            protected void done() {
                try {
                    Invoice inv = get();
                    bookingCompleted = true;
                    if (countdownTimer != null) {
                        countdownTimer.stop();
                    }

                    // Show success confirmation
                    int choice = JOptionPane.showOptionDialog(CustomerBookingDialog.this,
                            "Booking Confirmed Successfully!\n\n"
                                    + "Booking Reference: " + inv.getBookingReference() + "\n"
                                    + "Room: " + room.getRoomNumber() + " (" + room.getRoomType() + ")\n"
                                    + "Invoice Number: " + inv.getInvoiceNumber() + "\n"
                                    + "Amount Paid: Rs. " + inv.getPaidAmount() + " (PAID IN FULL)\n\n"
                                    + "Would you like to view/print your invoice now?",
                            "Reservation Confirmed",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.INFORMATION_MESSAGE,
                            null,
                            new String[]{"View & Print Invoice", "Go to My Bookings"},
                            "View & Print Invoice");

                    if (choice == 0) {
                        InvoiceRenderer.showInvoiceDialog(CustomerBookingDialog.this, inv);
                    }

                    if (onSuccessCallback != null) {
                        onSuccessCallback.run();
                    }

                    dispose();

                } catch (Exception ex) {
                    btnPay.setEnabled(true);
                    btnCancel.setEnabled(true);
                    Throwable cause = (ex.getCause() != null) ? ex.getCause() : ex;
                    JOptionPane.showMessageDialog(CustomerBookingDialog.this,
                            "Failed to confirm booking: " + cause.getMessage(),
                            "Booking Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };

        worker.execute();
    }

    private void cancelAndClose() {
        if (!bookingCompleted) {
            int res = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to cancel this reservation?\nThis will release the room hold for other guests.",
                    "Cancel Reservation", JOptionPane.YES_NO_OPTION);
            if (res == JOptionPane.YES_OPTION) {
                if (countdownTimer != null) countdownTimer.stop();
                try {
                    roomHoldDAO.releaseHold(hold.getHoldToken());
                } catch (Exception e) {
                    // Ignore
                }
                dispose();
            }
        } else {
            if (countdownTimer != null) countdownTimer.stop();
            dispose();
        }
    }
}
