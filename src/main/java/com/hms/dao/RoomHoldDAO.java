package com.hms.dao;

import com.hms.model.RoomHold;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * RoomHoldDAO Interface
 * 
 * Manages the lifecycle of temporary 5-minute room holds to prevent
 * "ghost inventory" (race conditions and double bookings).
 */
public interface RoomHoldDAO {

    /**
     * Creates a temporary 5-minute hold on a room for the specified dates.
     * Uses pessimistic row-locking (SELECT ... FOR UPDATE) inside a transaction.
     * 
     * @param roomId Target room ID
     * @param checkIn Check-in date
     * @param checkOut Check-out date
     * @return Created RoomHold object with unique holdToken and expiration timestamp.
     * @throws RoomHoldOverlapException if the room has an active hold or confirmed booking overlapping the dates.
     * @throws InvalidHoldDateException if check-in or check-out dates are invalid or in the past.
     * @throws IllegalArgumentException if the room does not exist or is under maintenance.
     */
    RoomHold createHold(int roomId, LocalDate checkIn, LocalDate checkOut);

    /**
     * Releases an active hold voluntarily (e.g. user cancels or clears reservation form).
     * Transitions status from 'ACTIVE' to 'CANCELLED'.
     * 
     * @param holdToken The unique hold token
     * @return true if successfully released, false otherwise.
     */
    boolean releaseHold(String holdToken);

    /**
     * Finds a hold record by its unique token.
     * 
     * @param holdToken The unique UUID token
     * @return Optional containing the RoomHold if found.
     */
    Optional<RoomHold> findByToken(String holdToken);

    /**
     * Checks if a room has any active, unexpired hold for the given date range.
     * Overlap condition: existingCheckIn < requestedCheckOut AND existingCheckOut > requestedCheckIn
     * 
     * @param roomId Target room ID
     * @param checkIn Requested check-in date
     * @param checkOut Requested check-out date
     * @return true if an active, unexpired hold overlaps with these dates.
     */
    boolean isRoomHeld(int roomId, LocalDate checkIn, LocalDate checkOut);

    /**
     * Finds all currently active, unexpired holds for a room.
     * 
     * @param roomId Target room ID
     * @return List of active RoomHold records.
     */
    List<RoomHold> findActiveHoldsForRoom(int roomId);

    /**
     * Sweeps and marks expired active holds as 'EXPIRED'.
     * Note: Queries also independently check (expires_at > NOW()) so availability is never blocked
     * even if cleanup has not yet run.
     * 
     * @return Number of holds transitioned to EXPIRED.
     */
    int cleanupExpiredHolds();

    /**
     * Exception thrown when an overlapping active hold or confirmed booking conflicts with the requested room hold.
     */
    class RoomHoldOverlapException extends RuntimeException {
        public RoomHoldOverlapException(String message) {
            super(message);
        }
    }

    /**
     * Exception thrown when dates provided for a hold are invalid.
     */
    class InvalidHoldDateException extends RuntimeException {
        public InvalidHoldDateException(String message) {
            super(message);
        }
    }
}
