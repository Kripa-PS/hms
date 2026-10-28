package com.hms.dao;

import com.hms.model.Payment;
import com.hms.util.DatabaseConnection;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * PaymentDAOImpl
 * 
 * JDBC Implementation of PaymentDAO.
 * Features:
 * - PreparedStatements for all database queries
 * - Database transactions (setAutoCommit(false)) with rollback on failure
 * - Prevention of overpayment against booking total amount
 * - Prevention of duplicate transaction references
 * - Robust error handling
 */
public class PaymentDAOImpl implements PaymentDAO {

    @Override
    public Payment recordPayment(Payment payment) {
        if (payment == null) {
            throw new IllegalArgumentException("Payment cannot be null.");
        }
        if (payment.getBookingId() <= 0) {
            throw new IllegalArgumentException("Invalid booking ID for payment: " + payment.getBookingId());
        }
        if (payment.getAmount() == null || payment.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than ₹0.00.");
        }

        // Auto-generate transaction reference if not provided
        if (payment.getTransactionReference() == null || payment.getTransactionReference().isBlank()) {
            payment.setTransactionReference("TXN-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 900 + 100));
        }

        String checkTxnSql = "SELECT COUNT(*) FROM payments WHERE transaction_reference = ?";
        String checkBookingSql = "SELECT total_amount, status FROM bookings WHERE id = ? FOR UPDATE";
        String checkPaidSql = "SELECT COALESCE(SUM(amount), 0) FROM payments WHERE booking_id = ? AND payment_status IN ('PAID', 'PARTIAL')";
        String insertSql = "INSERT INTO payments (booking_id, amount, payment_method, payment_status, payment_date, transaction_reference, notes) "
                         + "VALUES (?, ?, ?, ?, ?, ?, ?)";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            // 1. Check duplicate transaction reference
            try (PreparedStatement stmt = conn.prepareStatement(checkTxnSql)) {
                stmt.setString(1, payment.getTransactionReference().trim());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        conn.rollback();
                        throw new DuplicateTransactionException("Duplicate payment submission: Transaction reference '" 
                                + payment.getTransactionReference() + "' already exists.");
                    }
                }
            }

            // 2. Lock and retrieve booking details
            BigDecimal totalBookingAmount = BigDecimal.ZERO;
            try (PreparedStatement stmt = conn.prepareStatement(checkBookingSql)) {
                stmt.setInt(1, payment.getBookingId());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        totalBookingAmount = rs.getBigDecimal("total_amount");
                    } else {
                        conn.rollback();
                        throw new IllegalArgumentException("Booking not found with ID: " + payment.getBookingId());
                    }
                }
            }

            // 3. Overpayment check (only applies if recording a successful payment)
            String status = payment.getPaymentStatus();
            if (status == null || status.isBlank()) {
                status = "PAID";
                payment.setPaymentStatus(status);
            }

            if ("PAID".equalsIgnoreCase(status) || "PARTIAL".equalsIgnoreCase(status)) {
                BigDecimal currentPaid = BigDecimal.ZERO;
                try (PreparedStatement stmt = conn.prepareStatement(checkPaidSql)) {
                    stmt.setInt(1, payment.getBookingId());
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            currentPaid = rs.getBigDecimal(1);
                        }
                    }
                }

                BigDecimal balanceDue = totalBookingAmount.subtract(currentPaid).setScale(2, RoundingMode.HALF_UP);
                if (balanceDue.compareTo(BigDecimal.ZERO) < 0) {
                    balanceDue = BigDecimal.ZERO;
                }

                BigDecimal submittingAmount = payment.getAmount().setScale(2, RoundingMode.HALF_UP);
                if (submittingAmount.compareTo(balanceDue) > 0) {
                    conn.rollback();
                    throw new OverpaymentException("Overpayment blocked: Payment amount (₹" + submittingAmount 
                            + ") exceeds remaining balance (₹" + balanceDue + ") for this booking.");
                }
            }

            // 4. Insert Payment record
            try (PreparedStatement stmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setInt(1, payment.getBookingId());
                stmt.setBigDecimal(2, payment.getAmount().setScale(2, RoundingMode.HALF_UP));
                stmt.setString(3, payment.getPaymentMethod() != null ? payment.getPaymentMethod().toUpperCase() : "CASH");
                stmt.setString(4, status.toUpperCase());
                
                LocalDateTime now = payment.getPaymentDate() != null ? payment.getPaymentDate() : LocalDateTime.now();
                stmt.setTimestamp(5, Timestamp.valueOf(now));
                stmt.setString(6, payment.getTransactionReference().trim());
                stmt.setString(7, payment.getNotes());

                int affected = stmt.executeUpdate();
                if (affected == 0) {
                    conn.rollback();
                    throw new RuntimeException("Recording payment failed: No rows affected.");
                }

                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        payment.setId(generatedKeys.getInt(1));
                        payment.setPaymentDate(now);
                    }
                }
            }

            conn.commit();
            return payment;

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    System.err.println("[PaymentDAO] Rollback failed: " + ex.getMessage());
                }
            }
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("duplicate")) {
                throw new DuplicateTransactionException("Duplicate transaction reference detected by database: " 
                        + payment.getTransactionReference());
            }
            throw new RuntimeException("Database error recording payment: " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    System.err.println("[PaymentDAO] Error restoring connection auto-commit: " + e.getMessage());
                }
            }
        }
    }

    @Override
    public List<Payment> findPaymentsByBookingId(int bookingId) {
        List<Payment> list = new ArrayList<>();
        String sql = "SELECT p.id, p.booking_id, p.amount, p.payment_method, p.payment_status, "
                   + "       p.payment_date, p.transaction_reference, p.notes, "
                   + "       b.booking_reference, g.full_name AS guest_name, r.room_number "
                   + "FROM payments p "
                   + "JOIN bookings b ON p.booking_id = b.id "
                   + "JOIN guests g ON b.guest_id = g.id "
                   + "JOIN rooms r ON b.room_id = r.id "
                   + "WHERE p.booking_id = ? "
                   + "ORDER BY p.payment_date ASC, p.id ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, bookingId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToPayment(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[PaymentDAO] Error finding payments for booking " + bookingId + ": " + e.getMessage());
        }
        return list;
    }

    @Override
    public List<Payment> findAllPayments() {
        List<Payment> list = new ArrayList<>();
        String sql = "SELECT p.id, p.booking_id, p.amount, p.payment_method, p.payment_status, "
                   + "       p.payment_date, p.transaction_reference, p.notes, "
                   + "       b.booking_reference, g.full_name AS guest_name, r.room_number "
                   + "FROM payments p "
                   + "JOIN bookings b ON p.booking_id = b.id "
                   + "JOIN guests g ON b.guest_id = g.id "
                   + "JOIN rooms r ON b.room_id = r.id "
                   + "ORDER BY p.payment_date DESC, p.id DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                list.add(mapResultSetToPayment(rs));
            }
        } catch (SQLException e) {
            System.err.println("[PaymentDAO] Error finding all payments: " + e.getMessage());
        }
        return list;
    }

    @Override
    public Optional<Payment> findById(int id) {
        String sql = "SELECT p.id, p.booking_id, p.amount, p.payment_method, p.payment_status, "
                   + "       p.payment_date, p.transaction_reference, p.notes, "
                   + "       b.booking_reference, g.full_name AS guest_name, r.room_number "
                   + "FROM payments p "
                   + "JOIN bookings b ON p.booking_id = b.id "
                   + "JOIN guests g ON b.guest_id = g.id "
                   + "JOIN rooms r ON b.room_id = r.id "
                   + "WHERE p.id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToPayment(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[PaymentDAO] Error finding payment by id " + id + ": " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public BigDecimal getTotalPaidForBooking(int bookingId) {
        String sql = "SELECT COALESCE(SUM(amount), 0) FROM payments WHERE booking_id = ? AND payment_status IN ('PAID', 'PARTIAL')";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, bookingId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBigDecimal(1).setScale(2, RoundingMode.HALF_UP);
                }
            }
        } catch (SQLException e) {
            System.err.println("[PaymentDAO] Error calculating total paid for booking " + bookingId + ": " + e.getMessage());
        }
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public boolean isTransactionReferenceExists(String txnRef) {
        if (txnRef == null || txnRef.isBlank()) return false;
        String sql = "SELECT COUNT(*) FROM payments WHERE transaction_reference = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, txnRef.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("[PaymentDAO] Error checking transaction reference: " + e.getMessage());
        }
        return false;
    }

    private Payment mapResultSetToPayment(ResultSet rs) throws SQLException {
        Payment p = new Payment(
                rs.getInt("id"),
                rs.getInt("booking_id"),
                rs.getBigDecimal("amount"),
                rs.getString("payment_method"),
                rs.getString("payment_status"),
                rs.getTimestamp("payment_date").toLocalDateTime(),
                rs.getString("transaction_reference"),
                rs.getString("notes")
        );
        p.setBookingReference(rs.getString("booking_reference"));
        p.setGuestName(rs.getString("guest_name"));
        p.setRoomNumber(rs.getString("room_number"));
        return p;
    }
}
