package com.hms.dao;

import com.hms.model.RoomServiceOrder;
import com.hms.model.StayOption;
import com.hms.util.DatabaseConnection;
import com.hms.util.UserSession;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * RoomServiceDAOImpl
 *
 * JDBC implementation of RoomServiceDAO. Uses PreparedStatements for all queries.
 * Enforces backend customer data isolation and booking ownership.
 */
public class RoomServiceDAOImpl implements RoomServiceDAO {

    private static final Set<String> VALID_STATUSES =
            Set.of("REQUESTED", "IN_PROGRESS", "COMPLETED", "CANCELLED");

    @Override
    public List<StayOption> findOrderableStays() {
        String sql = "SELECT b.id, b.room_id, b.booking_reference, r.room_number, g.full_name "
                   + "FROM bookings b "
                   + "JOIN rooms r ON b.room_id = r.id "
                   + "JOIN guests g ON b.guest_id = g.id "
                   + "WHERE b.status IN ('CONFIRMED', 'CHECKED_IN') "
                   + "ORDER BY r.room_number ASC, b.check_in_date ASC";

        List<StayOption> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                list.add(new StayOption(
                        rs.getInt("id"),
                        rs.getInt("room_id"),
                        rs.getString("room_number"),
                        rs.getString("full_name"),
                        rs.getString("booking_reference")));
            }
        } catch (SQLException e) {
            System.err.println("[RoomServiceDAO] Error loading bookings: " + e.getMessage());
        }
        return list;
    }

    @Override
    public List<StayOption> findActiveStaysForGuest(int guestId) {
        UserSession.enforceGuestAccess(guestId);

        String sql = "SELECT b.id, b.room_id, b.booking_reference, r.room_number, g.full_name "
                   + "FROM bookings b "
                   + "JOIN rooms r ON b.room_id = r.id "
                   + "JOIN guests g ON b.guest_id = g.id "
                   + "WHERE b.guest_id = ? AND b.status IN ('CONFIRMED', 'CHECKED_IN') "
                   + "ORDER BY b.check_in_date ASC, b.id DESC";

        List<StayOption> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, guestId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(new StayOption(
                            rs.getInt("id"),
                            rs.getInt("room_id"),
                            rs.getString("room_number"),
                            rs.getString("full_name"),
                            rs.getString("booking_reference")));
                }
            }
        } catch (SQLException e) {
            System.err.println("[RoomServiceDAO] Error loading guest bookings: " + e.getMessage());
        }
        return list;
    }

    @Override
    public boolean addOrder(int bookingId, String serviceItem, int quantity, BigDecimal unitPrice) {
        if (bookingId <= 0 || serviceItem == null || serviceItem.isBlank()
                || quantity <= 0 || unitPrice == null || unitPrice.signum() < 0) {
            return false;
        }
        BigDecimal unit = unitPrice.setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = unit.multiply(BigDecimal.valueOf(quantity));

        // The room_id is read from the booking itself so an order can never point at the wrong room.
        String sql = "INSERT INTO room_service "
                   + "(booking_id, room_id, service_item, quantity, unit_price, total_price, status) "
                   + "SELECT b.id, b.room_id, ?, ?, ?, ?, 'REQUESTED' "
                   + "FROM bookings b WHERE b.id = ? AND b.status IN ('CONFIRMED', 'CHECKED_IN')";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, serviceItem.trim());
            stmt.setInt(2, quantity);
            stmt.setBigDecimal(3, unit);
            stmt.setBigDecimal(4, total);
            stmt.setInt(5, bookingId);
            return stmt.executeUpdate() == 1;
        } catch (SQLException e) {
            System.err.println("[RoomServiceDAO] Error adding order: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean addCustomerOrder(int guestId, int bookingId, String serviceItem, int quantity, BigDecimal unitPrice, String specialInstructions) {
        UserSession.enforceGuestAccess(guestId);

        if (guestId <= 0 || bookingId <= 0 || serviceItem == null || serviceItem.isBlank()
                || quantity <= 0 || quantity > 50 || unitPrice == null || unitPrice.signum() < 0) {
            return false;
        }

        // Format service_item with instructions while respecting VARCHAR(100)
        String combinedItem = serviceItem.trim();
        if (specialInstructions != null && !specialInstructions.isBlank()) {
            String sanitizedNote = specialInstructions.trim().replaceAll("[\\r\\n]+", " ");
            combinedItem = combinedItem + " (" + sanitizedNote + ")";
        }
        if (combinedItem.length() > 100) {
            combinedItem = combinedItem.substring(0, 97) + "...";
        }

        BigDecimal unit = unitPrice.setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = unit.multiply(BigDecimal.valueOf(quantity));

        // Securely binds to guestId and active booking status
        String sql = "INSERT INTO room_service "
                   + "(booking_id, room_id, service_item, quantity, unit_price, total_price, status) "
                   + "SELECT b.id, b.room_id, ?, ?, ?, ?, 'REQUESTED' "
                   + "FROM bookings b "
                   + "WHERE b.id = ? AND b.guest_id = ? AND b.status IN ('CONFIRMED', 'CHECKED_IN')";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, combinedItem);
            stmt.setInt(2, quantity);
            stmt.setBigDecimal(3, unit);
            stmt.setBigDecimal(4, total);
            stmt.setInt(5, bookingId);
            stmt.setInt(6, guestId);
            return stmt.executeUpdate() == 1;
        } catch (SQLException e) {
            System.err.println("[RoomServiceDAO] Error adding customer order: " + e.getMessage());
            return false;
        }
    }

    @Override
    public List<RoomServiceOrder> findAll(String status) {
        // Enforce customer data isolation: customers are blocked from viewing all hotel orders
        if (UserSession.isCustomer()) {
            throw new SecurityException("Access Denied: Customers cannot view all hotel orders.");
        }

        boolean filtered = status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status);

        String sql = "SELECT s.id, s.booking_id, b.booking_reference, g.full_name, s.room_id, "
                   + "r.room_number, s.service_item, s.quantity, s.unit_price, s.total_price, "
                   + "s.status, s.requested_at, s.completed_at "
                   + "FROM room_service s "
                   + "JOIN bookings b ON s.booking_id = b.id "
                   + "JOIN guests g ON b.guest_id = g.id "
                   + "JOIN rooms r ON s.room_id = r.id "
                   + (filtered ? "WHERE s.status = ? " : "")
                   + "ORDER BY s.requested_at DESC, s.id DESC";

        List<RoomServiceOrder> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            if (filtered) {
                stmt.setString(1, status);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(new RoomServiceOrder(
                            rs.getInt("id"),
                            rs.getInt("booking_id"),
                            rs.getString("booking_reference"),
                            rs.getString("full_name"),
                            rs.getInt("room_id"),
                            rs.getString("room_number"),
                            rs.getString("service_item"),
                            rs.getInt("quantity"),
                            rs.getBigDecimal("unit_price"),
                            rs.getBigDecimal("total_price"),
                            rs.getString("status"),
                            rs.getTimestamp("requested_at"),
                            rs.getTimestamp("completed_at")));
                }
            }
        } catch (SQLException e) {
            System.err.println("[RoomServiceDAO] Error loading orders: " + e.getMessage());
        }
        return list;
    }

    @Override
    public List<RoomServiceOrder> findOrdersByGuestId(int guestId) {
        UserSession.enforceGuestAccess(guestId);

        String sql = "SELECT s.id, s.booking_id, b.booking_reference, g.full_name, s.room_id, "
                   + "r.room_number, s.service_item, s.quantity, s.unit_price, s.total_price, "
                   + "s.status, s.requested_at, s.completed_at "
                   + "FROM room_service s "
                   + "JOIN bookings b ON s.booking_id = b.id "
                   + "JOIN guests g ON b.guest_id = g.id "
                   + "JOIN rooms r ON s.room_id = r.id "
                   + "WHERE b.guest_id = ? "
                   + "ORDER BY s.requested_at DESC, s.id DESC";

        List<RoomServiceOrder> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, guestId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(new RoomServiceOrder(
                            rs.getInt("id"),
                            rs.getInt("booking_id"),
                            rs.getString("booking_reference"),
                            rs.getString("full_name"),
                            rs.getInt("room_id"),
                            rs.getString("room_number"),
                            rs.getString("service_item"),
                            rs.getInt("quantity"),
                            rs.getBigDecimal("unit_price"),
                            rs.getBigDecimal("total_price"),
                            rs.getString("status"),
                            rs.getTimestamp("requested_at"),
                            rs.getTimestamp("completed_at")));
                }
            }
        } catch (SQLException e) {
            System.err.println("[RoomServiceDAO] Error loading customer orders: " + e.getMessage());
        }
        return list;
    }

    @Override
    public boolean updateStatus(int orderId, String newStatus) {
        if (newStatus == null || !VALID_STATUSES.contains(newStatus)) {
            return false;
        }
        String sql = "UPDATE room_service SET status = ?, "
                   + "completed_at = CASE WHEN ? = 'COMPLETED' THEN NOW() ELSE NULL END "
                   + "WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, newStatus);
            stmt.setString(2, newStatus);
            stmt.setInt(3, orderId);
            return stmt.executeUpdate() == 1;
        } catch (SQLException e) {
            System.err.println("[RoomServiceDAO] Error updating order status: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean cancelCustomerOrder(int guestId, int orderId) {
        UserSession.enforceGuestAccess(guestId);

        String sql = "UPDATE room_service s "
                   + "JOIN bookings b ON s.booking_id = b.id "
                   + "SET s.status = 'CANCELLED' "
                   + "WHERE s.id = ? AND b.guest_id = ? AND s.status = 'REQUESTED'";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, orderId);
            stmt.setInt(2, guestId);
            return stmt.executeUpdate() == 1;
        } catch (SQLException e) {
            System.err.println("[RoomServiceDAO] Error cancelling customer order: " + e.getMessage());
            return false;
        }
    }

    @Override
    public int countByStatus(String status) {
        String sql = "SELECT COUNT(*) FROM room_service WHERE status = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            System.err.println("[RoomServiceDAO] Error counting orders: " + e.getMessage());
            return -1;
        }
    }
}
