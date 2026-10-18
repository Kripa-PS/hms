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
 * JDBC Implementation of RoomDAO.
 * Uses PreparedStatements for all queries.
 */
public class RoomDAOImpl implements RoomDAO {

    @Override
    public List<Room> findAll() {
        List<Room> list = new ArrayList<>();
        String sql = "SELECT id, room_number, room_type, capacity, price_per_night, operational_status, amenities "
                   + "FROM rooms ORDER BY room_number ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                list.add(mapResultSetToRoom(rs));
            }
        } catch (SQLException e) {
            System.err.println("[RoomDAO] Error finding all rooms: " + e.getMessage());
        }
        return list;
    }

    @Override
    public List<Room> findOperationallyAvailableRooms() {
        List<Room> list = new ArrayList<>();
        String sql = "SELECT id, room_number, room_type, capacity, price_per_night, operational_status, amenities "
                   + "FROM rooms WHERE operational_status = 'AVAILABLE' ORDER BY room_number ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                list.add(mapResultSetToRoom(rs));
            }
        } catch (SQLException e) {
            System.err.println("[RoomDAO] Error finding operational rooms: " + e.getMessage());
        }
        return list;
    }

    @Override
    public List<Room> findAvailableRoomsForDates(LocalDate checkIn, LocalDate checkOut) {
        List<Room> list = new ArrayList<>();
        String sql = "SELECT r.id, r.room_number, r.room_type, r.capacity, r.price_per_night, r.operational_status, r.amenities "
                   + "FROM rooms r "
                   + "WHERE r.operational_status = 'AVAILABLE' "
                   + "  AND r.id NOT IN ( "
                   + "      SELECT b.room_id "
                   + "      FROM bookings b "
                   + "      WHERE b.status IN ('CONFIRMED', 'CHECKED_IN') "
                   + "        AND b.check_in_date < ? "
                   + "        AND b.check_out_date > ? "
                   + "  ) "
                   + "  AND r.id NOT IN ( "
                   + "      SELECT h.room_id "
                   + "      FROM room_holds h "
                   + "      WHERE h.status = 'ACTIVE' "
                   + "        AND h.expires_at > NOW() "
                   + "        AND h.check_in_date < ? "
                   + "        AND h.check_out_date > ? "
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
                    list.add(mapResultSetToRoom(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[RoomDAO] Error finding available rooms for dates: " + e.getMessage());
        }
        return list;
    }

    @Override
    public Optional<Room> findById(int id) {
        String sql = "SELECT id, room_number, room_type, capacity, price_per_night, operational_status, amenities "
                   + "FROM rooms WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToRoom(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[RoomDAO] Error finding room by id: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public Optional<Room> findByRoomNumber(String roomNumber) {
        String sql = "SELECT id, room_number, room_type, capacity, price_per_night, operational_status, amenities "
                   + "FROM rooms WHERE room_number = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, roomNumber);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToRoom(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[RoomDAO] Error finding room by number: " + e.getMessage());
        }
        return Optional.empty();
    }

    private Room mapResultSetToRoom(ResultSet rs) throws SQLException {
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
