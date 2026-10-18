package com.hms.dao;

import com.hms.model.Booking;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * BookingDAO Interface
 * 
 * Defines data access and reservation transaction operations for Booking Management.
 */
public interface BookingDAO {

    /**
     * Checks if a room is available for the given date range.
     * Enforces the interval condition:
     * existingCheckIn < requestedCheckOut AND existingCheckOut > requestedCheckIn
     * 
     * @param roomId Target room ID
     * @param checkIn Requested check-in date
     * @param checkOut Requested check-out date
     * @return true if no active booking overlaps and room is operational, false otherwise.
     */
    boolean isRoomAvailable(int roomId, LocalDate checkIn, LocalDate checkOut);

    /**
     * Checks if a room is available for the given date range, optionally ignoring
     * the user's own active holdToken.
     * 
     * @param roomId Target room ID
     * @param checkIn Requested check-in date
     * @param checkOut Requested check-out date
     * @param currentHoldToken Optional token of the hold currently owned by this user (null if none)
     * @return true if no active booking or other user's active hold overlaps, false otherwise.
     */
    boolean isRoomAvailable(int roomId, LocalDate checkIn, LocalDate checkOut, String currentHoldToken);

    /**
     * Creates a confirmed booking inside a safe database transaction.
     * Uses row-level locking (SELECT ... FOR UPDATE) to prevent concurrent double-bookings.
     * 
     * @param booking Booking object with guestId, roomId, checkInDate, checkOutDate, guestCount.
     * @return The created Booking object with assigned database ID and reference code.
     * @throws BookingOverlapException if the room is already booked for overlapping dates.
     * @throws IllegalArgumentException if dates or guest counts are invalid.
     * @throws RuntimeException for general database errors.
     */
    Booking createBooking(Booking booking);

    /**
     * Creates a confirmed booking inside a safe database transaction, optionally consuming a holdToken.
     * Uses row-level locking (SELECT ... FOR UPDATE) on the room row.
     * If holdToken is provided, validates that the hold belongs to the user, is active, matches room and dates,
     * and transitions its status to 'CONSUMED' upon successful booking.
     * If holdToken is null (direct booking), verifies no other active unexpired hold blocks the room.
     * 
     * @param booking Booking object
     * @param holdToken Optional hold token (null for direct booking)
     * @return The created Booking object
     * @throws BookingOverlapException if the room is booked or held by another guest.
     */
    Booking createBooking(Booking booking, String holdToken);

    /**
     * Cancels an existing booking, setting status to 'CANCELLED' and cancelled_at to NOW().
     * This immediately frees up the room dates for other guests.
     * 
     * @param bookingId ID of the booking to cancel.
     * @return true if successfully cancelled, false otherwise.
     */
    boolean cancelBooking(int bookingId);

    /**
     * Retrieves all bookings with joined guest name, phone, and room number for UI table display.
     */
    List<Booking> findAllBookings();

    /**
     * Finds a booking by its primary key ID.
     */
    Optional<Booking> findById(int id);

    /**
     * Finds a booking by its unique booking reference (e.g. HMS-2026-1001).
     */
    Optional<Booking> findByReference(String bookingReference);

    /**
     * Thrown when an overlapping booking conflicts with the requested room and dates.
     */
    class BookingOverlapException extends RuntimeException {
        public BookingOverlapException(String message) {
            super(message);
        }
    }

    /**
     * Thrown when check-in or check-out dates fail validation.
     */
    class InvalidBookingDateException extends RuntimeException {
        public InvalidBookingDateException(String message) {
            super(message);
        }
    }
}
