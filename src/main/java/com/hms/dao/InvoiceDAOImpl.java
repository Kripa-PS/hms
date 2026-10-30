package com.hms.dao;

import com.hms.model.Invoice;
import com.hms.util.BillCalculator;
import com.hms.util.DatabaseConnection;
import com.hms.util.UserSession;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * InvoiceDAOImpl
 * 
 * JDBC Implementation of InvoiceDAO.
 * Features:
 * - PreparedStatements for safe querying and upserting into the 'invoices' table
 * - Hydrates complete invoice metadata including guest, room, stay dates, and payment history
 * - Automatic calculation of nights, charges, amount paid, and balance due
 * - Enforces customer data isolation: customers can only view their own invoices
 */
public class InvoiceDAOImpl implements InvoiceDAO {

    @Override
    public Invoice generateInvoice(int bookingId, String notes) {
        String bookingSql = "SELECT b.id AS booking_id, b.booking_reference, b.guest_id, b.check_in_date, b.check_out_date, "
                          + "       b.total_amount, b.status AS booking_status, "
                          + "       g.full_name AS guest_name, g.phone AS guest_phone, g.email AS guest_email, g.address AS guest_address, "
                          + "       r.room_number, r.room_type, r.price_per_night "
                          + "FROM bookings b "
                          + "JOIN guests g ON b.guest_id = g.id "
                          + "JOIN rooms r ON b.room_id = r.id "
                          + "WHERE b.id = ?";

        String paymentSql = "SELECT COALESCE(SUM(CASE WHEN payment_status IN ('PAID', 'PARTIAL') THEN amount "
                          + "                          WHEN payment_status = 'REFUNDED' THEN -amount "
                          + "                          ELSE 0 END), 0) AS net_paid "
                          + "FROM payments WHERE booking_id = ?";

        String existingInvoiceSql = "SELECT id, invoice_number FROM invoices WHERE booking_id = ?";
        String updateInvoiceSql = "UPDATE invoices SET total_amount = ?, paid_amount = ?, balance_due = ?, "
                                + "issue_date = NOW(), notes = ? WHERE id = ?";
        String insertInvoiceSql = "INSERT INTO invoices (invoice_number, booking_id, issue_date, total_amount, "
                                + "paid_amount, balance_due, notes) VALUES (?, ?, NOW(), ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection()) {
            // 1. Fetch booking & guest & room details
            Invoice invoice = new Invoice();
            int guestId = -1;
            try (PreparedStatement stmt = conn.prepareStatement(bookingSql)) {
                stmt.setInt(1, bookingId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        throw new IllegalArgumentException("Booking not found with ID: " + bookingId);
                    }
                    guestId = rs.getInt("guest_id");
                    invoice.setBookingId(rs.getInt("booking_id"));
                    invoice.setBookingReference(rs.getString("booking_reference"));
                    invoice.setCheckInDate(rs.getDate("check_in_date").toLocalDate());
                    invoice.setCheckOutDate(rs.getDate("check_out_date").toLocalDate());
                    invoice.setTotalAmount(rs.getBigDecimal("total_amount"));
                    invoice.setGuestName(rs.getString("guest_name"));
                    invoice.setGuestPhone(rs.getString("guest_phone"));
                    invoice.setGuestEmail(rs.getString("guest_email"));
                    invoice.setGuestAddress(rs.getString("guest_address"));
                    invoice.setRoomNumber(rs.getString("room_number"));
                    invoice.setRoomType(rs.getString("room_type"));
                    invoice.setRatePerNight(rs.getBigDecimal("price_per_night"));
                }
            }

            // Enforce customer access restriction
            if (UserSession.isCustomer()) {
                UserSession.enforceGuestAccess(guestId);
            }

            // 2. Compute nights and room charges
            long nights = BillCalculator.calculateNights(invoice.getCheckInDate(), invoice.getCheckOutDate());
            invoice.setNumberOfNights(nights);
            BigDecimal roomCharges = BillCalculator.calculateRoomCharge(invoice.getRatePerNight(), nights);
            invoice.setTotalRoomCharges(roomCharges);

            // 3. Fetch net paid amount
            BigDecimal netPaid = BigDecimal.ZERO;
            try (PreparedStatement stmt = conn.prepareStatement(paymentSql)) {
                stmt.setInt(1, bookingId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        netPaid = rs.getBigDecimal("net_paid");
                    }
                }
            }
            if (netPaid.compareTo(BigDecimal.ZERO) < 0) {
                netPaid = BigDecimal.ZERO;
            }
            invoice.setPaidAmount(netPaid.setScale(2, RoundingMode.HALF_UP));

            // 4. Compute balance due
            BigDecimal balanceDue = BillCalculator.calculateBalanceDue(invoice.getTotalAmount(), invoice.getPaidAmount());
            invoice.setBalanceDue(balanceDue);
            invoice.setNotes(notes != null ? notes : "Thank you for choosing Grand Horizon Hotel.");

            // Summary status
            if (balanceDue.compareTo(BigDecimal.ZERO) <= 0) {
                invoice.setPaymentStatusSummary("PAID");
            } else if (invoice.getPaidAmount().compareTo(BigDecimal.ZERO) > 0) {
                invoice.setPaymentStatusSummary("PARTIAL");
            } else {
                invoice.setPaymentStatusSummary("UNPAID");
            }

            // 5. Check if invoice exists in DB
            int existingId = -1;
            String existingInvNum = null;
            try (PreparedStatement stmt = conn.prepareStatement(existingInvoiceSql)) {
                stmt.setInt(1, bookingId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        existingId = rs.getInt("id");
                        existingInvNum = rs.getString("invoice_number");
                    }
                }
            }

            if (existingId > 0) {
                // Update existing invoice
                try (PreparedStatement stmt = conn.prepareStatement(updateInvoiceSql)) {
                    stmt.setBigDecimal(1, invoice.getTotalAmount());
                    stmt.setBigDecimal(2, invoice.getPaidAmount());
                    stmt.setBigDecimal(3, invoice.getBalanceDue());
                    stmt.setString(4, invoice.getNotes());
                    stmt.setInt(5, existingId);
                    stmt.executeUpdate();
                }
                invoice.setId(existingId);
                invoice.setInvoiceNumber(existingInvNum);
                invoice.setIssueDate(LocalDateTime.now());
            } else {
                // Insert new invoice
                int year = LocalDate.now().getYear();
                String invNum = "INV-" + year + "-" + String.format("%04d", bookingId);
                // Check if invNum already taken (in case of re-runs)
                String checkInvSql = "SELECT COUNT(*) FROM invoices WHERE invoice_number = ?";
                try (PreparedStatement stmt = conn.prepareStatement(checkInvSql)) {
                    stmt.setString(1, invNum);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next() && rs.getInt(1) > 0) {
                            invNum = "INV-" + year + "-" + bookingId + "-" + (int)(Math.random() * 900 + 100);
                        }
                    }
                }

                try (PreparedStatement stmt = conn.prepareStatement(insertInvoiceSql, Statement.RETURN_GENERATED_KEYS)) {
                    stmt.setString(1, invNum);
                    stmt.setInt(2, bookingId);
                    stmt.setBigDecimal(3, invoice.getTotalAmount());
                    stmt.setBigDecimal(4, invoice.getPaidAmount());
                    stmt.setBigDecimal(5, invoice.getBalanceDue());
                    stmt.setString(6, invoice.getNotes());

                    stmt.executeUpdate();
                    try (ResultSet keys = stmt.getGeneratedKeys()) {
                        if (keys.next()) {
                            invoice.setId(keys.getInt(1));
                        }
                    }
                }
                invoice.setInvoiceNumber(invNum);
                invoice.setIssueDate(LocalDateTime.now());
            }

            return invoice;

        } catch (SQLException e) {
            throw new RuntimeException("Database error generating invoice for booking " + bookingId + ": " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Invoice> findInvoiceByBookingId(int bookingId) {
        String sql = "SELECT i.id, i.invoice_number, i.booking_id, i.issue_date, i.total_amount, i.paid_amount, "
                   + "       i.balance_due, i.notes, "
                   + "       b.booking_reference, b.guest_id, b.check_in_date, b.check_out_date, "
                   + "       g.full_name AS guest_name, g.phone AS guest_phone, g.email AS guest_email, g.address AS guest_address, "
                   + "       r.room_number, r.room_type, r.price_per_night "
                   + "FROM invoices i "
                   + "JOIN bookings b ON i.booking_id = b.id "
                   + "JOIN guests g ON b.guest_id = g.id "
                   + "JOIN rooms r ON b.room_id = r.id "
                   + "WHERE i.booking_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, bookingId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int guestId = rs.getInt("guest_id");
                    if (UserSession.isCustomer()) {
                        UserSession.enforceGuestAccess(guestId);
                    }
                    return Optional.of(mapResultSetToInvoice(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[InvoiceDAO] Error querying invoice: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public Optional<Invoice> findInvoiceByNumber(String invoiceNumber) {
        String sql = "SELECT i.id, i.invoice_number, i.booking_id, i.issue_date, i.total_amount, i.paid_amount, "
                   + "       i.balance_due, i.notes, "
                   + "       b.booking_reference, b.guest_id, b.check_in_date, b.check_out_date, "
                   + "       g.full_name AS guest_name, g.phone AS guest_phone, g.email AS guest_email, g.address AS guest_address, "
                   + "       r.room_number, r.room_type, r.price_per_night "
                   + "FROM invoices i "
                   + "JOIN bookings b ON i.booking_id = b.id "
                   + "JOIN guests g ON b.guest_id = g.id "
                   + "JOIN rooms r ON b.room_id = r.id "
                   + "WHERE i.invoice_number = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, invoiceNumber.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int guestId = rs.getInt("guest_id");
                    if (UserSession.isCustomer()) {
                        UserSession.enforceGuestAccess(guestId);
                    }
                    return Optional.of(mapResultSetToInvoice(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[InvoiceDAO] Error querying invoice by number: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public List<Invoice> findAllInvoices() {
        if (UserSession.isCustomer()) {
            throw new SecurityException("Access Denied: Customers cannot view all system invoices.");
        }

        List<Invoice> list = new ArrayList<>();
        String sql = "SELECT i.id, i.invoice_number, i.booking_id, i.issue_date, i.total_amount, i.paid_amount, "
                   + "       i.balance_due, i.notes, "
                   + "       b.booking_reference, b.check_in_date, b.check_out_date, "
                   + "       g.full_name AS guest_name, g.phone AS guest_phone, g.email AS guest_email, g.address AS guest_address, "
                   + "       r.room_number, r.room_type, r.price_per_night "
                   + "FROM invoices i "
                   + "JOIN bookings b ON i.booking_id = b.id "
                   + "JOIN guests g ON b.guest_id = g.id "
                   + "JOIN rooms r ON b.room_id = r.id "
                   + "ORDER BY i.issue_date DESC, i.id DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                list.add(mapResultSetToInvoice(rs));
            }
        } catch (SQLException e) {
            System.err.println("[InvoiceDAO] Error finding all invoices: " + e.getMessage());
        }
        return list;
    }

    @Override
    public List<Invoice> findInvoicesByGuestId(int guestId) {
        UserSession.enforceGuestAccess(guestId);

        List<Invoice> list = new ArrayList<>();
        String sql = "SELECT i.id, i.invoice_number, i.booking_id, i.issue_date, i.total_amount, i.paid_amount, "
                   + "       i.balance_due, i.notes, "
                   + "       b.booking_reference, b.check_in_date, b.check_out_date, "
                   + "       g.full_name AS guest_name, g.phone AS guest_phone, g.email AS guest_email, g.address AS guest_address, "
                   + "       r.room_number, r.room_type, r.price_per_night "
                   + "FROM invoices i "
                   + "JOIN bookings b ON i.booking_id = b.id "
                   + "JOIN guests g ON b.guest_id = g.id "
                   + "JOIN rooms r ON b.room_id = r.id "
                   + "WHERE b.guest_id = ? "
                   + "ORDER BY i.issue_date DESC, i.id DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, guestId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToInvoice(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[InvoiceDAO] Error finding invoices by guest id: " + e.getMessage());
        }
        return list;
    }

    private Invoice mapResultSetToInvoice(ResultSet rs) throws SQLException {
        Invoice inv = new Invoice(
                rs.getInt("id"),
                rs.getString("invoice_number"),
                rs.getInt("booking_id"),
                rs.getTimestamp("issue_date").toLocalDateTime(),
                rs.getBigDecimal("total_amount"),
                rs.getBigDecimal("paid_amount"),
                rs.getBigDecimal("balance_due"),
                rs.getString("notes")
        );

        inv.setBookingReference(rs.getString("booking_reference"));
        inv.setGuestName(rs.getString("guest_name"));
        inv.setGuestPhone(rs.getString("guest_phone"));
        inv.setGuestEmail(rs.getString("guest_email"));
        inv.setGuestAddress(rs.getString("guest_address"));
        inv.setRoomNumber(rs.getString("room_number"));
        inv.setRoomType(rs.getString("room_type"));
        inv.setRatePerNight(rs.getBigDecimal("price_per_night"));

        LocalDate inDate = rs.getDate("check_in_date").toLocalDate();
        LocalDate outDate = rs.getDate("check_out_date").toLocalDate();
        inv.setCheckInDate(inDate);
        inv.setCheckOutDate(outDate);

        long nights = BillCalculator.calculateNights(inDate, outDate);
        inv.setNumberOfNights(nights);
        inv.setTotalRoomCharges(BillCalculator.calculateRoomCharge(inv.getRatePerNight(), nights));

        if (inv.getBalanceDue().compareTo(BigDecimal.ZERO) <= 0) {
            inv.setPaymentStatusSummary("PAID");
        } else if (inv.getPaidAmount().compareTo(BigDecimal.ZERO) > 0) {
            inv.setPaymentStatusSummary("PARTIAL");
        } else {
            inv.setPaymentStatusSummary("UNPAID");
        }

        return inv;
    }
}
