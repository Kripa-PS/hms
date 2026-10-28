package com.hms.dao;

import com.hms.model.Room;
import com.hms.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * RoomDAOImpl
 *
 * JDBC implementation of RoomDAO.
 * Uses PreparedStatements for all queries.
 * Existing read methods are unchanged (Booking module depends on them).
 * New write/search methods support the Room Management module.
 */
public class RoomDAOImpl implements RoomDAO {

    // -----------------------------------------------------------------------
    // READ METHODS (unchanged — Booking module depends on these)
    // -----------------------------------------------------------------------

    @Override
    public List<Room> findAll() {
        List<Room> list = new ArrayList<>();
        String sql = "SELECT id, room_number, room_type, capacity, price_per_night, "
                   + "operational_status, amenities FROM rooms ORDER BY room_number ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("[RoomDAO] Error finding all rooms: " + e.getMessage());
        }
        return list;
    }

    @Override
    public List<Room> findOperationallyAvailableRooms() {
        List<Room> list = new ArrayList<>();
        String sql = "SELECT id, room_number, room_type, capacity, price_per_night, "
                   + "operational_status, amenities "
                   + "FROM rooms WHERE operational_status = 'AVAILABLE' ORDER BY room_number ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("[RoomDAO] Error finding operational rooms: " + e.getMessage());
        }
        return list;
    }

    @Override
    public List<Room> findAvailableRoomsForDates(LocalDate checkIn, LocalDate checkOut) {
        List<Room> list = new ArrayList<>();
        String sql = "SELECT r.id, r.room_number, r.room_type, r.capacity, "
                   + "r.price_per_night, r.operational_status, r.amenities "
                   + "FROM rooms r "
                   + "WHERE r.operational_status = 'AVAILABLE' "
                   + "  AND r.id NOT IN ( "
                   + "      SELECT b.room_id FROM bookings b "
                   + "      WHERE b.status IN ('CONFIRMED', 'CHECKED_IN') "
                   + "        AND b.check_in_date < ? AND b.check_out_date > ? "
                   + "  ) "
                   + "  AND r.id NOT IN ( "
                   + "      SELECT h.room_id FROM room_holds h "
                   + "      WHERE h.status = 'ACTIVE' AND h.expires_at > NOW() "
                   + "        AND h.check_in_date < ? AND h.check_out_date > ? "
                   + "  ) "
                   + "ORDER BY r.room_number ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, java.sql.Date.valueOf(checkOut));
            stmt.setDate(2, java.sql.Date.valueOf(checkIn));
            stmt.setDate(3, java.sql.Date.valueOf(checkOut));
            stmt.setDate(4, java.sql.Date.valueOf(checkIn));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[RoomDAO] Error finding available rooms for dates: " + e.getMessage());
        }
        return list;
    }

    @Override
    public Optional<Room> findById(int id) {
        String sql = "SELECT id, room_number, room_type, capacity, price_per_night, "
                   + "operational_status, amenities FROM rooms WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[RoomDAO] Error finding room by id: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public Optional<Room> findByRoomNumber(String roomNumber) {
        String sql = "SELECT id, room_number, room_type, capacity, price_per_night, "
                   + "operational_status, amenities FROM rooms WHERE room_number = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, roomNumber);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[RoomDAO] Error finding room by number: " + e.getMessage());
        }
        return Optional.empty();
    }

    // -----------------------------------------------------------------------
    // WRITE METHODS (Room Management module)
    // -----------------------------------------------------------------------

    @Override
    public boolean addRoom(Room room) {
        String sql = "INSERT INTO rooms (room_number, room_type, capacity, price_per_night, "
                   + "amenities, operational_status) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, room.getRoomNumber().trim().toUpperCase());
            stmt.setString(2, room.getRoomType());
            stmt.setInt(3, room.getCapacity());
            stmt.setBigDecimal(4, room.getPricePerNight());
            stmt.setString(5, room.getAmenities() != null ? room.getAmenities().trim() : "WiFi, TV, AC");
            stmt.setString(6, room.getOperationalStatus());

            return stmt.executeUpdate() == 1;

        } catch (SQLException e) {
            System.err.println("[RoomDAO] Error adding room: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean updateRoom(Room room) {
        String sql = "UPDATE rooms SET room_number = ?, room_type = ?, capacity = ?, "
                   + "price_per_night = ?, amenities = ?, operational_status = ? "
                   + "WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, room.getRoomNumber().trim().toUpperCase());
            stmt.setString(2, room.getRoomType());
            stmt.setInt(3, room.getCapacity());
            stmt.setBigDecimal(4, room.getPricePerNight());
            stmt.setString(5, room.getAmenities() != null ? room.getAmenities().trim() : "WiFi, TV, AC");
            stmt.setString(6, room.getOperationalStatus());
            stmt.setInt(7, room.getId());

            return stmt.executeUpdate() == 1;

        } catch (SQLException e) {
            System.err.println("[RoomDAO] Error updating room: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean roomNumberExists(String roomNumber, int excludeId) {
        // excludeId == 0 means we are adding a new room (exclude nothing)
        String sql = excludeId == 0
                ? "SELECT COUNT(*) FROM rooms WHERE room_number = ?"
                : "SELECT COUNT(*) FROM rooms WHERE room_number = ? AND id <> ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, roomNumber.trim().toUpperCase());
            if (excludeId != 0) {
                stmt.setInt(2, excludeId);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("[RoomDAO] Error checking room number existence: " + e.getMessage());
        }
        return false;
    }

    @Override
    public List<Room> searchRooms(String roomNumberKeyword, String roomType, String status) {
        List<Room> list = new ArrayList<>();

        // Build query dynamically based on which filters are provided
        StringBuilder sql = new StringBuilder(
                "SELECT id, room_number, room_type, capacity, price_per_night, "
              + "operational_status, amenities FROM rooms WHERE 1=1");

        boolean hasKeyword = roomNumberKeyword != null && !roomNumberKeyword.isBlank();
        boolean hasType    = roomType  != null && !roomType.isBlank()  && !roomType.equals("ALL");
        boolean hasStatus  = status    != null && !status.isBlank()    && !status.equals("ALL");

        if (hasKeyword) sql.append(" AND room_number LIKE ?");
        if (hasType)    sql.append(" AND room_type = ?");
        if (hasStatus)  sql.append(" AND operational_status = ?");
        sql.append(" ORDER BY room_number ASC");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            int idx = 1;
            if (hasKeyword) stmt.setString(idx++, "%" + roomNumberKeyword.trim() + "%");
            if (hasType)    stmt.setString(idx++, roomType);
            if (hasStatus)  stmt.setString(idx++, status);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[RoomDAO] Error searching rooms: " + e.getMessage());
        }
        return list;
    }

    @Override
    public boolean setMaintenanceStatus(int roomId, boolean maintenance) {
        // Only transition AVAILABLE → MAINTENANCE or MAINTENANCE → AVAILABLE.
        // OCCUPIED rooms are managed by the Booking module and must not be touched here.
        String expectedCurrent = maintenance ? "AVAILABLE"    : "MAINTENANCE";
        String newStatus       = maintenance ? "MAINTENANCE"  : "AVAILABLE";

        String sql = "UPDATE rooms SET operational_status = ? "
                   + "WHERE id = ? AND operational_status = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, newStatus);
            stmt.setInt(2, roomId);
            stmt.setString(3, expectedCurrent);

            int rows = stmt.executeUpdate();
            if (rows == 0) {
                System.out.println("[RoomDAO] setMaintenanceStatus: No update applied. "
                    + "Room " + roomId + " may be OCCUPIED or already in target state.");
            }
            return rows == 1;

        } catch (SQLException e) {
            System.err.println("[RoomDAO] Error setting maintenance status: " + e.getMessage());
            return false;
        }
    }

    // -----------------------------------------------------------------------
    // PRIVATE HELPER
    // -----------------------------------------------------------------------

    /** Maps a ResultSet row to a Room object. */
    private Room mapRow(ResultSet rs) throws SQLException {
        Room r = new Room(
                rs.getInt("id"),
                rs.getString("room_number"),
                rs.getString("room_type"),
                rs.getInt("capacity"),
                rs.getBigDecimal("price_per_night"),
                rs.getString("operational_status")
        );
        r.setAmenities(rs.getString("amenities"));
        return r;
    }
}
