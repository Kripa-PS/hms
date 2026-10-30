package com.hms.dao;

import com.hms.model.Guest;
import com.hms.util.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * GuestDAOImpl
 * 
 * JDBC Implementation of GuestDAO.
 * Uses PreparedStatements for all database operations to prevent SQL Injection.
 * Handles MySQL error code 1062 (Duplicate Key) gracefully.
 */
public class GuestDAOImpl implements GuestDAO {

    private static final int MYSQL_DUPLICATE_KEY_ERROR = 1062;

    @Override
    public boolean addGuest(Guest guest) {
        if (guest == null) {
            throw new IllegalArgumentException("Guest cannot be null.");
        }

        // Check duplicate phone first to provide immediate domain exception
        if (guest.getPhone() != null && !guest.getPhone().isBlank()) {
            if (findByPhone(guest.getPhone().trim()).isPresent()) {
                throw new DuplicatePhoneException("A guest with phone '" + guest.getPhone().trim() + "' is already registered.");
            }
        }

        guest.validate();

        String sql = "INSERT INTO guests (full_name, phone, email, id_proof_type, id_proof_number, address) "
                   + "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, guest.getFullName());
            stmt.setString(2, guest.getPhone());
            stmt.setString(3, guest.getEmail());
            stmt.setString(4, guest.getIdProofType());
            stmt.setString(5, guest.getIdProofNumber());
            stmt.setString(6, guest.getAddress());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        guest.setId(generatedKeys.getInt(1));
                    }
                }
                return true;
            }
            return false;

        } catch (SQLException e) {
            handleSqlException(e, guest.getPhone());
            return false;
        }
    }

    @Override
    public Optional<Guest> findById(int id) {
        String sql = "SELECT id, full_name, phone, email, id_proof_type, id_proof_number, address, created_at, updated_at "
                   + "FROM guests WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToGuest(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[GuestDAO] Error finding guest by id: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public Optional<Guest> findByPhone(String phone) {
        String sql = "SELECT id, full_name, phone, email, id_proof_type, id_proof_number, address, created_at, updated_at "
                   + "FROM guests WHERE phone = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, phone);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToGuest(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[GuestDAO] Error finding guest by phone: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public List<Guest> searchByNameOrPhone(String query) {
        List<Guest> list = new ArrayList<>();
        String sql = "SELECT id, full_name, phone, email, id_proof_type, id_proof_number, address, created_at, updated_at "
                   + "FROM guests WHERE full_name LIKE ? OR phone LIKE ? ORDER BY id DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            String pattern = "%" + query.trim() + "%";
            stmt.setString(1, pattern);
            stmt.setString(2, pattern);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToGuest(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[GuestDAO] Error searching guests: " + e.getMessage());
        }
        return list;
    }

    @Override
    public List<Guest> findAll() {
        List<Guest> list = new ArrayList<>();
        String sql = "SELECT id, full_name, phone, email, id_proof_type, id_proof_number, address, created_at, updated_at "
                   + "FROM guests ORDER BY id DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                list.add(mapResultSetToGuest(rs));
            }
        } catch (SQLException e) {
            System.err.println("[GuestDAO] Error retrieving all guests: " + e.getMessage());
        }
        return list;
    }

    @Override
    public boolean updateGuest(Guest guest) {
        if (guest == null) {
            throw new IllegalArgumentException("Guest cannot be null.");
        }
        guest.validate();

        String sql = "UPDATE guests SET full_name = ?, phone = ?, email = ?, id_proof_type = ?, "
                   + "id_proof_number = ?, address = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, guest.getFullName());
            stmt.setString(2, guest.getPhone());
            stmt.setString(3, guest.getEmail());
            stmt.setString(4, guest.getIdProofType());
            stmt.setString(5, guest.getIdProofNumber());
            stmt.setString(6, guest.getAddress());
            stmt.setInt(7, guest.getId());

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            handleSqlException(e, guest.getPhone());
            return false;
        }
    }

    /**
     * Maps a current ResultSet row into a Guest object.
     */
    private Guest mapResultSetToGuest(ResultSet rs) throws SQLException {
        Timestamp createdTs = rs.getTimestamp("created_at");
        Timestamp updatedTs = rs.getTimestamp("updated_at");

        LocalDateTime createdAt = (createdTs != null) ? createdTs.toLocalDateTime() : null;
        LocalDateTime updatedAt = (updatedTs != null) ? updatedTs.toLocalDateTime() : null;

        return new Guest(
                rs.getInt("id"),
                rs.getString("full_name"),
                rs.getString("phone"),
                rs.getString("email"),
                rs.getString("id_proof_type"),
                rs.getString("id_proof_number"),
                rs.getString("address"),
                createdAt,
                updatedAt
        );
    }

    /**
     * Analyzes SQL exception to identify duplicate phone constraint violation.
     */
    private void handleSqlException(SQLException e, String phone) {
        if (e.getErrorCode() == MYSQL_DUPLICATE_KEY_ERROR || "23000".equals(e.getSQLState())) {
            throw new DuplicatePhoneException("A guest with phone number '" + phone + "' is already registered in the system.");
        }
        System.err.println("[GuestDAO] Database Error [" + e.getErrorCode() + "]: " + e.getMessage());
        throw new RuntimeException("Database error: " + e.getMessage(), e);
    }
}
