package com.hms.dao;

import com.hms.model.User;
import com.hms.util.BCrypt;
import com.hms.util.DatabaseConnection;
import com.hms.util.ValidationUtil;
import com.hms.util.ValidationUtil.ValidationResult;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * UserDAOImpl
 * 
 * JDBC Implementation of UserDAO.
 * Supports secure authentication with BCrypt, customer registration,
 * and profile management with database transactions.
 */
public class UserDAOImpl implements UserDAO {

    @Override
    public Optional<User> authenticate(String username, String plaintextPassword) {
        if (username == null || username.trim().isEmpty() || plaintextPassword == null) {
            return Optional.empty();
        }

        String sql = "SELECT id, username, password_hash, full_name, role, guest_id, active, created_at "
                   + "FROM users WHERE username = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    boolean active = rs.getBoolean("active");
                    if (!active) {
                        return Optional.empty(); // Inactive / disabled account
                    }

                    String passwordHash = rs.getString("password_hash");
                    if (BCrypt.checkpw(plaintextPassword, passwordHash)) {
                        int id = rs.getInt("id");
                        String user = rs.getString("username");
                        String fullName = rs.getString("full_name");
                        String role = rs.getString("role");
                        Integer guestId = rs.getObject("guest_id") != null ? rs.getInt("guest_id") : null;
                        Timestamp ts = rs.getTimestamp("created_at");
                        LocalDateTime createdAt = (ts != null) ? ts.toLocalDateTime() : LocalDateTime.now();

                        return Optional.of(new User(id, user, passwordHash, fullName, role, guestId, active, createdAt));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] Error during authentication: " + e.getMessage());
        }

        return Optional.empty();
    }

    @Override
    public User registerCustomer(String username, String plaintextPassword, String fullName, 
                                 String phone, String email, String idProofType, 
                                 String idProofNumber, String address) {
        // 1. Validations
        if (username == null || username.trim().length() < 3) {
            throw new IllegalArgumentException("Username must be at least 3 characters long.");
        }
        if (plaintextPassword == null || plaintextPassword.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters long.");
        }
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new IllegalArgumentException("Full name is required.");
        }

        ValidationResult phoneResult = ValidationUtil.validatePhone(phone);
        if (!phoneResult.isValid()) {
            throw new IllegalArgumentException(phoneResult.getMessage());
        }

        ValidationResult emailResult = ValidationUtil.validateEmail(email, true);
        if (!emailResult.isValid()) {
            throw new IllegalArgumentException(emailResult.getMessage());
        }

        ValidationResult idResult = ValidationUtil.validateIdProof(idProofType, idProofNumber, false);
        if (!idResult.isValid()) {
            throw new IllegalArgumentException(idResult.getMessage());
        }

        String cleanUsername = username.trim();
        String cleanName = fullName.trim();
        String cleanPhone = phone.trim().replace("-", "").replace(" ", "").replace("+91", "");
        String cleanEmail = email.trim();
        String cleanDocType = (idProofType != null && !idProofType.isBlank()) ? idProofType.trim() : "Aadhaar";
        String cleanDocNumber = (idProofNumber != null) ? idProofNumber.trim() : "";
        String cleanAddress = (address != null) ? address.trim() : "";

        // 2. Transaction
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);

            try {
                // Check if username is already taken
                String checkUserSql = "SELECT id FROM users WHERE username = ?";
                try (PreparedStatement checkUserStmt = conn.prepareStatement(checkUserSql)) {
                    checkUserStmt.setString(1, cleanUsername);
                    try (ResultSet rs = checkUserStmt.executeQuery()) {
                        if (rs.next()) {
                            conn.rollback();
                            throw new DuplicateUsernameException("Username '" + cleanUsername + "' is already taken. Please choose another.");
                        }
                    }
                }

                // Check if a guest record with this phone number already exists
                int guestId = -1;
                String checkGuestSql = "SELECT id FROM guests WHERE phone = ?";
                try (PreparedStatement checkGuestStmt = conn.prepareStatement(checkGuestSql)) {
                    checkGuestStmt.setString(1, cleanPhone);
                    try (ResultSet rs = checkGuestStmt.executeQuery()) {
                        if (rs.next()) {
                            guestId = rs.getInt("id");
                        }
                    }
                }

                if (guestId > 0) {
                    // Check if an online account is already linked to this guest
                    String checkLinkedSql = "SELECT id FROM users WHERE guest_id = ?";
                    try (PreparedStatement checkLinkedStmt = conn.prepareStatement(checkLinkedSql)) {
                        checkLinkedStmt.setInt(1, guestId);
                        try (ResultSet rs = checkLinkedStmt.executeQuery()) {
                            if (rs.next()) {
                                conn.rollback();
                                throw new DuplicateCustomerException("An account already exists for phone number " + cleanPhone + ". Please log in instead.");
                            }
                        }
                    }

                    // Existing guest found without account: update guest record with latest email/details
                    String updateGuestSql = "UPDATE guests SET full_name = ?, email = ?, id_proof_type = ?, id_proof_number = ?, address = ?, updated_at = NOW() WHERE id = ?";
                    try (PreparedStatement updateStmt = conn.prepareStatement(updateGuestSql)) {
                        updateStmt.setString(1, cleanName);
                        updateStmt.setString(2, cleanEmail);
                        updateStmt.setString(3, cleanDocType);
                        updateStmt.setString(4, cleanDocNumber);
                        updateStmt.setString(5, cleanAddress);
                        updateStmt.setInt(6, guestId);
                        updateStmt.executeUpdate();
                    }
                } else {
                    // Create brand new guest record
                    String insertGuestSql = "INSERT INTO guests (full_name, phone, email, id_proof_type, id_proof_number, address) VALUES (?, ?, ?, ?, ?, ?)";
                    try (PreparedStatement insertGuestStmt = conn.prepareStatement(insertGuestSql, Statement.RETURN_GENERATED_KEYS)) {
                        insertGuestStmt.setString(1, cleanName);
                        insertGuestStmt.setString(2, cleanPhone);
                        insertGuestStmt.setString(3, cleanEmail);
                        insertGuestStmt.setString(4, cleanDocType);
                        insertGuestStmt.setString(5, cleanDocNumber);
                        insertGuestStmt.setString(6, cleanAddress);
                        insertGuestStmt.executeUpdate();

                        try (ResultSet gk = insertGuestStmt.getGeneratedKeys()) {
                            if (gk.next()) {
                                guestId = gk.getInt(1);
                            } else {
                                conn.rollback();
                                throw new RuntimeException("Failed to generate guest ID.");
                            }
                        }
                    }
                }

                // Hash password securely with BCrypt
                String passwordHash = BCrypt.hashpw(plaintextPassword, BCrypt.gensalt(10));

                // Insert into users
                String insertUserSql = "INSERT INTO users (username, password_hash, full_name, role, guest_id, active) VALUES (?, ?, ?, 'CUSTOMER', ?, TRUE)";
                int userId = -1;
                try (PreparedStatement insertUserStmt = conn.prepareStatement(insertUserSql, Statement.RETURN_GENERATED_KEYS)) {
                    insertUserStmt.setString(1, cleanUsername);
                    insertUserStmt.setString(2, passwordHash);
                    insertUserStmt.setString(3, cleanName);
                    insertUserStmt.setInt(4, guestId);
                    insertUserStmt.executeUpdate();

                    try (ResultSet gk = insertUserStmt.getGeneratedKeys()) {
                        if (gk.next()) {
                            userId = gk.getInt(1);
                        }
                    }
                }

                conn.commit();

                return new User(userId, cleanUsername, passwordHash, cleanName, "CUSTOMER", guestId, true, LocalDateTime.now());

            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                if (e instanceof DuplicateUsernameException || e instanceof DuplicateCustomerException) {
                    throw (RuntimeException) e;
                }
                throw new RuntimeException("Error during customer registration: " + e.getMessage(), e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<User> findById(int id) {
        String sql = "SELECT id, username, password_hash, full_name, role, guest_id, active, created_at FROM users WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Timestamp ts = rs.getTimestamp("created_at");
                    LocalDateTime createdAt = (ts != null) ? ts.toLocalDateTime() : LocalDateTime.now();
                    Integer guestId = rs.getObject("guest_id") != null ? rs.getInt("guest_id") : null;
                    return Optional.of(new User(rs.getInt("id"), rs.getString("username"), rs.getString("password_hash"),
                            rs.getString("full_name"), rs.getString("role"), guestId, rs.getBoolean("active"), createdAt));
                }
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] Error findById: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public Optional<User> findByUsername(String username) {
        String sql = "SELECT id, username, password_hash, full_name, role, guest_id, active, created_at FROM users WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Timestamp ts = rs.getTimestamp("created_at");
                    LocalDateTime createdAt = (ts != null) ? ts.toLocalDateTime() : LocalDateTime.now();
                    Integer guestId = rs.getObject("guest_id") != null ? rs.getInt("guest_id") : null;
                    return Optional.of(new User(rs.getInt("id"), rs.getString("username"), rs.getString("password_hash"),
                            rs.getString("full_name"), rs.getString("role"), guestId, rs.getBoolean("active"), createdAt));
                }
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] Error findByUsername: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public boolean updateCustomerProfile(int userId, String fullName, String phone, String email, 
                                         String address, String idProofType, String idProofNumber) {
        ValidationResult emailRes = ValidationUtil.validateEmail(email, true);
        if (!emailRes.isValid()) throw new IllegalArgumentException(emailRes.getMessage());
        ValidationResult phoneRes = ValidationUtil.validatePhone(phone);
        if (!phoneRes.isValid()) throw new IllegalArgumentException(phoneRes.getMessage());
        ValidationResult idRes = ValidationUtil.validateIdProof(idProofType, idProofNumber, false);
        if (!idRes.isValid()) throw new IllegalArgumentException(idRes.getMessage());

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Get user's guestId
                Integer guestId = null;
                String getUserSql = "SELECT guest_id FROM users WHERE id = ?";
                try (PreparedStatement getUserStmt = conn.prepareStatement(getUserSql)) {
                    getUserStmt.setInt(1, userId);
                    try (ResultSet rs = getUserStmt.executeQuery()) {
                        if (rs.next() && rs.getObject("guest_id") != null) {
                            guestId = rs.getInt("guest_id");
                        }
                    }
                }

                // Update users table
                String updateUserSql = "UPDATE users SET full_name = ? WHERE id = ?";
                try (PreparedStatement uuStmt = conn.prepareStatement(updateUserSql)) {
                    uuStmt.setString(1, fullName.trim());
                    uuStmt.setInt(2, userId);
                    uuStmt.executeUpdate();
                }

                // Update guests table
                if (guestId != null) {
                    String updateGuestSql = "UPDATE guests SET full_name = ?, phone = ?, email = ?, address = ?, id_proof_type = ?, id_proof_number = ?, updated_at = NOW() WHERE id = ?";
                    try (PreparedStatement ugStmt = conn.prepareStatement(updateGuestSql)) {
                        ugStmt.setString(1, fullName.trim());
                        ugStmt.setString(2, phone.trim().replace("-", "").replace(" ", "").replace("+91", ""));
                        ugStmt.setString(3, email.trim());
                        ugStmt.setString(4, address != null ? address.trim() : "");
                        ugStmt.setString(5, idProofType != null ? idProofType.trim() : "Aadhaar");
                        ugStmt.setString(6, idProofNumber != null ? idProofNumber.trim() : "");
                        ugStmt.setInt(7, guestId);
                        ugStmt.executeUpdate();
                    }
                }

                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] Error updateCustomerProfile: " + e.getMessage());
            return false;
        }
    }
}
