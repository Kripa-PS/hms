package com.hms.dao;

import com.hms.model.Booking;
import com.hms.util.DatabaseConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * BookingDAOImpl
 * 
 * JDBC Implementation of BookingDAO.
 * Features:
 * - Interval overlap condition: existingCheckIn < requestedCheckOut AND existingCheckOut > requestedCheckIn
 * - Back-to-back booking support (same-day changeover is allowed)
 * - Row-level locking (SELECT ... FOR UPDATE) inside database transaction to prevent concurrent double-bookings
 * - Auto-rollback on any conflict or error
 * - Joined queries to retrieve guest name, phone, and room number for UI display
 */
public class BookingDAOImpl implements BookingDAO {

    private final Random random = new Random();

    @Override
    public boolean isRoomAvailable(int roomId, LocalDate checkIn, LocalDate checkOut) {
        return isRoomAvailable(roomId, checkIn, checkOut, null);
    }

    @Override
    public boolean isRoomAvailable(int roomId, LocalDate checkIn, LocalDate checkOut, String currentHoldToken) {
        if (checkIn == null || checkOut == null || !checkOut.isAfter(checkIn)) {
            return false;
        }

        String sql = "SELECT COUNT(*) FROM rooms r WHERE r.id = ? AND r.operational_status = 'AVAILABLE'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, roomId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next() || rs.getInt(1) == 0) {
                    return false; // Room does not exist or is under maintenance
                }
            }

            // Overlap check query for confirmed / checked-in bookings
            String overlapSql = "SELECT COUNT(*) FROM bookings b "
                              + "WHERE b.room_id = ? "
                              + "  AND b.status IN ('CONFIRMED', 'CHECKED_IN') "
                              + "  AND b.check_in_date < ? "
                              + "  AND b.check_out_date > ?";

            try (PreparedStatement overlapStmt = conn.prepareStatement(overlapSql)) {
                overlapStmt.setInt(1, roomId);
                overlapStmt.setDate(2, java.sql.Date.valueOf(checkOut));
                overlapStmt.setDate(3, java.sql.Date.valueOf(checkIn));

                try (ResultSet rs = overlapStmt.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        return false; // Conflicting booking exists
                    }
                }
            }

            // Check overlapping active, unexpired holds (excluding currentHoldToken if provided)
            StringBuilder holdSql = new StringBuilder(
                    "SELECT COUNT(*) FROM room_holds h "
                  + "WHERE h.room_id = ? "
                  + "  AND h.status = 'ACTIVE' "
                  + "  AND h.expires_at > NOW() "
                  + "  AND h.check_in_date < ? "
                  + "  AND h.check_out_date > ?");

            if (currentHoldToken != null && !currentHoldToken.isBlank()) {
                holdSql.append(" AND h.hold_token != ?");
            }

            try (PreparedStatement holdStmt = conn.prepareStatement(holdSql.toString())) {
                holdStmt.setInt(1, roomId);
                holdStmt.setDate(2, java.sql.Date.valueOf(checkOut));
                holdStmt.setDate(3, java.sql.Date.valueOf(checkIn));
                if (currentHoldToken != null && !currentHoldToken.isBlank()) {
                    holdStmt.setString(4, currentHoldToken.trim());
                }

                try (ResultSet rs = holdStmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1) == 0;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[BookingDAO] Error checking room availability: " + e.getMessage());
        }
        return false;
    }

    @Override
    public Booking createBooking(Booking booking) {
        return createBooking(booking, null);
    }

    @Override
    public Booking createBooking(Booking booking, String holdToken) {
        // 1. Input validations
        if (booking.getCheckInDate() == null || booking.getCheckOutDate() == null) {
            throw new InvalidBookingDateException("Check-in and check-out dates are required.");
        }
        if (!booking.getCheckOutDate().isAfter(booking.getCheckInDate())) {
            throw new InvalidBookingDateException("Check-out date (" + booking.getCheckOutDate() 
                    + ") must be after check-in date (" + booking.getCheckInDate() + ").");
        }
        if (booking.getCheckInDate().isBefore(LocalDate.now())) {
            throw new InvalidBookingDateException("Check-in date (" + booking.getCheckInDate() 
                    + ") cannot be in the past.");
        }
        if (booking.getGuestCount() < 1) {
            throw new IllegalArgumentException("Guest count must be at least 1.");
        }

        // 2. Transaction with pessimistic row-locking on Room
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false); // Begin transaction

            try {
                // Step A: Lock room row with SELECT ... FOR UPDATE
                String lockRoomSql = "SELECT id, room_number, price_per_night, capacity, operational_status "
                                   + "FROM rooms WHERE id = ? FOR UPDATE";
                String roomNumber;
                BigDecimal pricePerNight;
                int roomCapacity;

                try (PreparedStatement lockStmt = conn.prepareStatement(lockRoomSql)) {
                    lockStmt.setInt(1, booking.getRoomId());
                    try (ResultSet rs = lockStmt.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            throw new IllegalArgumentException("Room ID " + booking.getRoomId() + " does not exist.");
                        }
                        if (!"AVAILABLE".equalsIgnoreCase(rs.getString("operational_status"))) {
                            conn.rollback();
                            throw new IllegalStateException("Room " + rs.getString("room_number") 
                                    + " is currently under maintenance or not available.");
                        }
                        roomNumber = rs.getString("room_number");
                        pricePerNight = rs.getBigDecimal("price_per_night");
                        roomCapacity = rs.getInt("capacity");
                    }
                }

                // Step B: Validate capacity against room limits
                if (booking.getGuestCount() > roomCapacity) {
                    conn.rollback();
                    throw new IllegalArgumentException("Guest count (" + booking.getGuestCount() 
                            + ") exceeds room capacity (" + roomCapacity + ").");
                }

                // Step C: Verify guest exists
                String guestSql = "SELECT full_name FROM guests WHERE id = ?";
                try (PreparedStatement guestStmt = conn.prepareStatement(guestSql)) {
                    guestStmt.setInt(1, booking.getGuestId());
                    try (ResultSet rs = guestStmt.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            throw new IllegalArgumentException("Guest ID " + booking.getGuestId() + " does not exist.");
                        }
                    }
                }

                // Step D: Re-check booking overlap while holding the lock
                String overlapSql = "SELECT COUNT(*) FROM bookings "
                                  + "WHERE room_id = ? "
                                  + "  AND status IN ('CONFIRMED', 'CHECKED_IN') "
                                  + "  AND check_in_date < ? "
                                  + "  AND check_out_date > ?";
                try (PreparedStatement overlapStmt = conn.prepareStatement(overlapSql)) {
                    overlapStmt.setInt(1, booking.getRoomId());
                    overlapStmt.setDate(2, java.sql.Date.valueOf(booking.getCheckOutDate()));
                    overlapStmt.setDate(3, java.sql.Date.valueOf(booking.getCheckInDate()));

                    try (ResultSet rs = overlapStmt.executeQuery()) {
                        if (rs.next() && rs.getInt(1) > 0) {
                            conn.rollback();
                            throw new BookingOverlapException("Room " + roomNumber + " is already booked for the selected dates ("
                                    + booking.getCheckInDate() + " to " + booking.getCheckOutDate() + ").");
                        }
                    }
                }

                // Step D2: Check hold conflicts and validate hold ownership
                if (holdToken != null && !holdToken.isBlank()) {
                    // User supplied a holdToken: verify it is active, unexpired, and matches room + dates
                    String verifyHoldSql = "SELECT id, room_id, check_in_date, check_out_date, expires_at, status "
                                         + "FROM room_holds WHERE hold_token = ? FOR UPDATE";
                    try (PreparedStatement vhStmt = conn.prepareStatement(verifyHoldSql)) {
                        vhStmt.setString(1, holdToken.trim());
                        try (ResultSet rs = vhStmt.executeQuery()) {
                            if (!rs.next()) {
                                conn.rollback();
                                throw new IllegalStateException("Hold token '" + holdToken + "' was not found.");
                            }
                            String holdStatus = rs.getString("status");
                            LocalDateTime expiresAt = rs.getObject("expires_at", LocalDateTime.class);
                            int holdRoomId = rs.getInt("room_id");
                            LocalDate holdIn = rs.getObject("check_in_date", LocalDate.class);
                            LocalDate holdOut = rs.getObject("check_out_date", LocalDate.class);

                            if (!"ACTIVE".equalsIgnoreCase(holdStatus)) {
                                conn.rollback();
                                throw new IllegalStateException("This room hold is no longer active (Status: " + holdStatus + ").");
                            }
                            if (expiresAt != null && expiresAt.isBefore(LocalDateTime.now())) {
                                conn.rollback();
                                throw new IllegalStateException("Your 5-minute room hold has expired. Please check availability again.");
                            }
                            if (holdRoomId != booking.getRoomId() 
                                    || !holdIn.equals(booking.getCheckInDate()) 
                                    || !holdOut.equals(booking.getCheckOutDate())) {
                                conn.rollback();
                                throw new IllegalArgumentException("Hold details do not match the requested booking room or dates.");
                            }
                        }
                    }

                    // Check that no other active hold conflicts
                    String otherHoldSql = "SELECT COUNT(*) FROM room_holds "
                                        + "WHERE room_id = ? "
                                        + "  AND status = 'ACTIVE' "
                                        + "  AND expires_at > NOW() "
                                        + "  AND hold_token != ? "
                                        + "  AND check_in_date < ? "
                                        + "  AND check_out_date > ?";
                    try (PreparedStatement ohStmt = conn.prepareStatement(otherHoldSql)) {
                        ohStmt.setInt(1, booking.getRoomId());
                        ohStmt.setString(2, holdToken.trim());
                        ohStmt.setDate(3, java.sql.Date.valueOf(booking.getCheckOutDate()));
                        ohStmt.setDate(4, java.sql.Date.valueOf(booking.getCheckInDate()));
                        try (ResultSet rs = ohStmt.executeQuery()) {
                            if (rs.next() && rs.getInt(1) > 0) {
                                conn.rollback();
                                throw new BookingOverlapException("Room " + roomNumber 
                                        + " is currently on hold by another guest for the selected dates.");
                            }
                        }
                    }

                } else {
                    // Direct booking (no holdToken): ensure no other active, unexpired hold conflicts
                    String directHoldCheckSql = "SELECT COUNT(*) FROM room_holds "
                                              + "WHERE room_id = ? "
                                              + "  AND status = 'ACTIVE' "
                                              + "  AND expires_at > NOW() "
                                              + "  AND check_in_date < ? "
                                              + "  AND check_out_date > ?";
                    try (PreparedStatement dhStmt = conn.prepareStatement(directHoldCheckSql)) {
                        dhStmt.setInt(1, booking.getRoomId());
                        dhStmt.setDate(2, java.sql.Date.valueOf(booking.getCheckOutDate()));
                        dhStmt.setDate(3, java.sql.Date.valueOf(booking.getCheckInDate()));
                        try (ResultSet rs = dhStmt.executeQuery()) {
                            if (rs.next() && rs.getInt(1) > 0) {
                                conn.rollback();
                                throw new BookingOverlapException("Room " + roomNumber 
                                        + " is currently on temporary hold by another guest for the selected dates.");
                            }
                        }
                    }
                }

                // Step E: Compute total amount (nights * price_per_night)
                long nights = booking.getNumberOfNights();
                BigDecimal calculatedTotal = pricePerNight.multiply(BigDecimal.valueOf(nights));
                if (booking.getTotalAmount() == null || booking.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
                    booking.setTotalAmount(calculatedTotal);
                }

                // Step F: Generate unique reference if missing
                if (booking.getBookingReference() == null || booking.getBookingReference().isBlank()) {
                    booking.setBookingReference(generateBookingReference(conn));
                }

                // Step G: Insert booking
                String insertSql = "INSERT INTO bookings (booking_reference, guest_id, room_id, check_in_date, check_out_date, guest_count, status, total_amount) "
                                 + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement insertStmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                    insertStmt.setString(1, booking.getBookingReference());
                    insertStmt.setInt(2, booking.getGuestId());
                    insertStmt.setInt(3, booking.getRoomId());
                    insertStmt.setDate(4, java.sql.Date.valueOf(booking.getCheckInDate()));
                    insertStmt.setDate(5, java.sql.Date.valueOf(booking.getCheckOutDate()));
                    insertStmt.setInt(6, booking.getGuestCount());
                    insertStmt.setString(7, (booking.getStatus() != null) ? booking.getStatus() : "CONFIRMED");
                    insertStmt.setBigDecimal(8, booking.getTotalAmount());

                    insertStmt.executeUpdate();
                    try (ResultSet generatedKeys = insertStmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            booking.setId(generatedKeys.getInt(1));
                        }
                    }
                }

                // Step H: Consume the hold if one was used
                if (holdToken != null && !holdToken.isBlank()) {
                    String consumeSql = "UPDATE room_holds SET status = 'CONSUMED' WHERE hold_token = ?";
                    try (PreparedStatement consumeStmt = conn.prepareStatement(consumeSql)) {
                        consumeStmt.setString(1, holdToken.trim());
                        consumeStmt.executeUpdate();
                    }
                }

                conn.commit(); // Commit transaction atomically
                return booking;

            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                }
                throw new RuntimeException("Database error creating booking: " + e.getMessage(), e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database connection error: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean cancelBooking(int bookingId) {
        String sql = "UPDATE bookings SET status = 'CANCELLED', cancelled_at = CURRENT_TIMESTAMP, "
                   + "updated_at = CURRENT_TIMESTAMP WHERE id = ? AND status != 'CANCELLED'";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, bookingId);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[BookingDAO] Error cancelling booking: " + e.getMessage());
            throw new RuntimeException("Error cancelling booking: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Booking> findAllBookings() {
        List<Booking> list = new ArrayList<>();
        String sql = "SELECT b.id, b.booking_reference, b.guest_id, b.room_id, b.check_in_date, b.check_out_date, "
                   + "       b.guest_count, b.status, b.total_amount, b.created_at, b.updated_at, b.cancelled_at, "
                   + "       g.full_name AS guest_name, g.phone AS guest_phone, "
                   + "       r.room_number, r.room_type "
                   + "FROM bookings b "
                   + "JOIN guests g ON b.guest_id = g.id "
                   + "JOIN rooms r ON b.room_id = r.id "
                   + "ORDER BY b.id DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                list.add(mapResultSetToBooking(rs));
            }
        } catch (SQLException e) {
            System.err.println("[BookingDAO] Error finding all bookings: " + e.getMessage());
        }
        return list;
    }

    @Override
    public Optional<Booking> findById(int id) {
        String sql = "SELECT b.id, b.booking_reference, b.guest_id, b.room_id, b.check_in_date, b.check_out_date, "
                   + "       b.guest_count, b.status, b.total_amount, b.created_at, b.updated_at, b.cancelled_at, "
                   + "       g.full_name AS guest_name, g.phone AS guest_phone, "
                   + "       r.room_number, r.room_type "
                   + "FROM bookings b "
                   + "JOIN guests g ON b.guest_id = g.id "
                   + "JOIN rooms r ON b.room_id = r.id "
                   + "WHERE b.id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToBooking(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[BookingDAO] Error finding booking by id: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public Optional<Booking> findByReference(String bookingReference) {
        String sql = "SELECT b.id, b.booking_reference, b.guest_id, b.room_id, b.check_in_date, b.check_out_date, "
                   + "       b.guest_count, b.status, b.total_amount, b.created_at, b.updated_at, b.cancelled_at, "
                   + "       g.full_name AS guest_name, g.phone AS guest_phone, "
                   + "       r.room_number, r.room_type "
                   + "FROM bookings b "
                   + "JOIN guests g ON b.guest_id = g.id "
                   + "JOIN rooms r ON b.room_id = r.id "
                   + "WHERE b.booking_reference = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, bookingReference.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToBooking(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[BookingDAO] Error finding booking by reference: " + e.getMessage());
        }
        return Optional.empty();
    }

    private Booking mapResultSetToBooking(ResultSet rs) throws SQLException {
        Timestamp createdTs = rs.getTimestamp("created_at");
        Timestamp updatedTs = rs.getTimestamp("updated_at");
        Timestamp cancelledTs = rs.getTimestamp("cancelled_at");

        LocalDateTime createdAt = (createdTs != null) ? createdTs.toLocalDateTime() : null;
        LocalDateTime updatedAt = (updatedTs != null) ? updatedTs.toLocalDateTime() : null;
        LocalDateTime cancelledAt = (cancelledTs != null) ? cancelledTs.toLocalDateTime() : null;

        Booking b = new Booking(
                rs.getInt("id"),
                rs.getString("booking_reference"),
                rs.getInt("guest_id"),
                rs.getInt("room_id"),
                rs.getDate("check_in_date").toLocalDate(),
                rs.getDate("check_out_date").toLocalDate(),
                rs.getInt("guest_count"),
                rs.getString("status"),
                rs.getBigDecimal("total_amount"),
                createdAt,
                updatedAt,
                cancelledAt
        );

        b.setGuestName(rs.getString("guest_name"));
        b.setGuestPhone(rs.getString("guest_phone"));
        b.setRoomNumber(rs.getString("room_number"));
        b.setRoomType(rs.getString("room_type"));

        return b;
    }

    private String generateBookingReference(Connection conn) throws SQLException {
        String ref;
        int attempts = 0;
        do {
            int num = 1000 + random.nextInt(9000);
            ref = "HMS-2026-" + num;
            attempts++;
            if (attempts > 50) {
                ref = "HMS-2026-" + System.currentTimeMillis() % 100000;
                break;
            }
        } while (referenceExists(conn, ref));
        return ref;
    }

    private boolean referenceExists(Connection conn, String ref) throws SQLException {
        String sql = "SELECT COUNT(*) FROM bookings WHERE booking_reference = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, ref);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }
}
