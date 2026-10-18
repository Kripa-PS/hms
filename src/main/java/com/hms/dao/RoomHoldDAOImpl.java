package com.hms.dao;

import com.hms.model.RoomHold;
import com.hms.util.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * RoomHoldDAOImpl
 * 
 * JDBC Implementation of RoomHoldDAO for 5-minute Ghost Inventory Prevention.
 * 
 * Features:
 * - Pessimistic row-locking (SELECT ... FOR UPDATE) on rooms table during hold creation
 * - Interval overlap condition: existingCheckIn < requestedCheckOut AND existingCheckOut > requestedCheckIn
 * - Server-side 5-minute expiry calculation via DATE_ADD(NOW(), INTERVAL 5 MINUTE)
 * - Self-expiring queries checking (expires_at > NOW()) to ensure expired holds stop blocking
 *   availability immediately without requiring background daemons
 * - Safe release and cancellation handling
 */
public class RoomHoldDAOImpl implements RoomHoldDAO {

    @Override
    public RoomHold createHold(int roomId, LocalDate checkIn, LocalDate checkOut) {
        // 1. Validation
        if (checkIn == null || checkOut == null) {
            throw new InvalidHoldDateException("Check-in and check-out dates are required.");
        }
        if (!checkOut.isAfter(checkIn)) {
            throw new InvalidHoldDateException("Check-out date (" + checkOut 
                    + ") must be after check-in date (" + checkIn + ").");
        }
        if (checkIn.isBefore(LocalDate.now())) {
            throw new InvalidHoldDateException("Check-in date (" + checkIn + ") cannot be in the past.");
        }

        // 2. Transaction with row lock on rooms table
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);

            try {
                // Step A: Lock and verify room operational status
                String lockSql = "SELECT id, room_number, operational_status FROM rooms WHERE id = ? FOR UPDATE";
                String roomNumber;
                try (PreparedStatement lockStmt = conn.prepareStatement(lockSql)) {
                    lockStmt.setInt(1, roomId);
                    try (ResultSet rs = lockStmt.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            throw new IllegalArgumentException("Room ID " + roomId + " does not exist.");
                        }
                        if (!"AVAILABLE".equalsIgnoreCase(rs.getString("operational_status"))) {
                            conn.rollback();
                            throw new IllegalStateException("Room " + rs.getString("room_number") 
                                    + " is currently under maintenance or not available.");
                        }
                        roomNumber = rs.getString("room_number");
                    }
                }

                // Step B: Check overlapping confirmed or checked-in bookings
                String bookingOverlapSql = "SELECT COUNT(*) FROM bookings "
                                         + "WHERE room_id = ? "
                                         + "  AND status IN ('CONFIRMED', 'CHECKED_IN') "
                                         + "  AND check_in_date < ? "
                                         + "  AND check_out_date > ?";
                try (PreparedStatement bStmt = conn.prepareStatement(bookingOverlapSql)) {
                    bStmt.setInt(1, roomId);
                    bStmt.setDate(2, java.sql.Date.valueOf(checkOut));
                    bStmt.setDate(3, java.sql.Date.valueOf(checkIn));
                    try (ResultSet rs = bStmt.executeQuery()) {
                        if (rs.next() && rs.getInt(1) > 0) {
                            conn.rollback();
                            throw new RoomHoldOverlapException("Room " + roomNumber 
                                    + " is already booked for the selected dates (" + checkIn + " to " + checkOut + ").");
                        }
                    }
                }

                // Step C: Check overlapping active, unexpired holds
                String holdOverlapSql = "SELECT COUNT(*) FROM room_holds "
                                      + "WHERE room_id = ? "
                                      + "  AND status = 'ACTIVE' "
                                      + "  AND expires_at > NOW() "
                                      + "  AND check_in_date < ? "
                                      + "  AND check_out_date > ?";
                try (PreparedStatement hStmt = conn.prepareStatement(holdOverlapSql)) {
                    hStmt.setInt(1, roomId);
                    hStmt.setDate(2, java.sql.Date.valueOf(checkOut));
                    hStmt.setDate(3, java.sql.Date.valueOf(checkIn));
                    try (ResultSet rs = hStmt.executeQuery()) {
                        if (rs.next() && rs.getInt(1) > 0) {
                            conn.rollback();
                            throw new RoomHoldOverlapException("Room " + roomNumber 
                                    + " is currently on temporary hold by another guest for the selected dates.");
                        }
                    }
                }

                // Step D: Insert 5-minute hold record
                String token = "HOLD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                String insertSql = "INSERT INTO room_holds (hold_token, room_id, check_in_date, check_out_date, expires_at, status) "
                                 + "VALUES (?, ?, ?, ?, DATE_ADD(NOW(), INTERVAL 5 MINUTE), 'ACTIVE')";
                int generatedId;
                try (PreparedStatement insertStmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                    insertStmt.setString(1, token);
                    insertStmt.setInt(2, roomId);
                    insertStmt.setDate(3, java.sql.Date.valueOf(checkIn));
                    insertStmt.setDate(4, java.sql.Date.valueOf(checkOut));
                    insertStmt.executeUpdate();

                    try (ResultSet keys = insertStmt.getGeneratedKeys()) {
                        if (keys.next()) {
                            generatedId = keys.getInt(1);
                        } else {
                            conn.rollback();
                            throw new SQLException("Failed to retrieve generated hold ID.");
                        }
                    }
                }

                // Step E: Fetch exact inserted timestamps
                LocalDateTime expiresAt = null;
                LocalDateTime createdAt = null;
                String fetchSql = "SELECT expires_at, created_at FROM room_holds WHERE id = ?";
                try (PreparedStatement fetchStmt = conn.prepareStatement(fetchSql)) {
                    fetchStmt.setInt(1, generatedId);
                    try (ResultSet rs = fetchStmt.executeQuery()) {
                        if (rs.next()) {
                            expiresAt = rs.getObject("expires_at", LocalDateTime.class);
                            createdAt = rs.getObject("created_at", LocalDateTime.class);
                        }
                    }
                }

                conn.commit();

                RoomHold hold = new RoomHold(generatedId, token, roomId, checkIn, checkOut, expiresAt, "ACTIVE", createdAt);
                hold.setRoomNumber(roomNumber);
                return hold;

            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                }
                throw new RuntimeException("Database error creating room hold: " + e.getMessage(), e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Connection error during room hold creation: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean releaseHold(String holdToken) {
        if (holdToken == null || holdToken.isBlank()) {
            return false;
        }

        String sql = "UPDATE room_holds SET status = 'CANCELLED' WHERE hold_token = ? AND status = 'ACTIVE'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, holdToken.trim());
            int updated = stmt.executeUpdate();
            return updated > 0;
        } catch (SQLException e) {
            System.err.println("[RoomHoldDAO] Error releasing hold: " + e.getMessage());
            return false;
        }
    }

    @Override
    public Optional<RoomHold> findByToken(String holdToken) {
        if (holdToken == null || holdToken.isBlank()) {
            return Optional.empty();
        }

        String sql = "SELECT h.id, h.hold_token, h.room_id, h.check_in_date, h.check_out_date, "
                   + "       h.expires_at, h.status, h.created_at, r.room_number "
                   + "FROM room_holds h "
                   + "JOIN rooms r ON h.room_id = r.id "
                   + "WHERE h.hold_token = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, holdToken.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToHold(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[RoomHoldDAO] Error finding hold by token: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public boolean isRoomHeld(int roomId, LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null || checkOut == null || !checkOut.isAfter(checkIn)) {
            return false;
        }

        String sql = "SELECT COUNT(*) FROM room_holds "
                   + "WHERE room_id = ? "
                   + "  AND status = 'ACTIVE' "
                   + "  AND expires_at > NOW() "
                   + "  AND check_in_date < ? "
                   + "  AND check_out_date > ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, roomId);
            stmt.setDate(2, java.sql.Date.valueOf(checkOut));
            stmt.setDate(3, java.sql.Date.valueOf(checkIn));

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("[RoomHoldDAO] Error checking if room is held: " + e.getMessage());
        }
        return false;
    }

    @Override
    public List<RoomHold> findActiveHoldsForRoom(int roomId) {
        List<RoomHold> list = new ArrayList<>();
        String sql = "SELECT h.id, h.hold_token, h.room_id, h.check_in_date, h.check_out_date, "
                   + "       h.expires_at, h.status, h.created_at, r.room_number "
                   + "FROM room_holds h "
                   + "JOIN rooms r ON h.room_id = r.id "
                   + "WHERE h.room_id = ? "
                   + "  AND h.status = 'ACTIVE' "
                   + "  AND h.expires_at > NOW() "
                   + "ORDER BY h.check_in_date ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, roomId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToHold(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[RoomHoldDAO] Error finding active holds: " + e.getMessage());
        }
        return list;
    }

    @Override
    public int cleanupExpiredHolds() {
        String sql = "UPDATE room_holds SET status = 'EXPIRED' WHERE status = 'ACTIVE' AND expires_at <= NOW()";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            return stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[RoomHoldDAO] Error during cleanup: " + e.getMessage());
            return 0;
        }
    }

    private RoomHold mapResultSetToHold(ResultSet rs) throws SQLException {
        LocalDateTime expiresAt = rs.getObject("expires_at", LocalDateTime.class);
        LocalDateTime createdAt = rs.getObject("created_at", LocalDateTime.class);
        LocalDate in = rs.getObject("check_in_date", LocalDate.class);
        LocalDate out = rs.getObject("check_out_date", LocalDate.class);

        RoomHold hold = new RoomHold(
                rs.getInt("id"),
                rs.getString("hold_token"),
                rs.getInt("room_id"),
                in,
                out,
                expiresAt,
                rs.getString("status"),
                createdAt
        );
        hold.setRoomNumber(rs.getString("room_number"));
        return hold;
    }
}
