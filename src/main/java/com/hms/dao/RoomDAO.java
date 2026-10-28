package com.hms.dao;

import com.hms.model.Room;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * RoomDAO Interface
 *
 * Data-access contract for the 'rooms' table.
 * Read methods (findAll, findById, etc.) are used by the Booking module.
 * Write methods (addRoom, updateRoom, etc.) are used by the Room Management module.
 */
public interface RoomDAO {

    // -----------------------------------------------------------------------
    // READ METHODS (used by Booking module — do not change signatures)
    // -----------------------------------------------------------------------

    /** Returns every room ordered by room_number ASC. */
    List<Room> findAll();

    /** Returns rooms whose operational_status = 'AVAILABLE'. */
    List<Room> findOperationallyAvailableRooms();

    /**
     * Returns rooms that are AVAILABLE and have no overlapping confirmed/checked-in
     * booking AND no active hold for the given date range.
     */
    List<Room> findAvailableRoomsForDates(LocalDate checkIn, LocalDate checkOut);

    /** Finds a room by its primary-key ID. */
    Optional<Room> findById(int id);

    /** Finds a room by its unique room_number. */
    Optional<Room> findByRoomNumber(String roomNumber);

    // -----------------------------------------------------------------------
    // WRITE METHODS (used by Room Management module)
    // -----------------------------------------------------------------------

    /**
     * Inserts a new room into the database.
     *
     * @param room Room object (id field is ignored — DB auto-generates it)
     * @return true if the INSERT succeeded, false otherwise
     */
    boolean addRoom(Room room);

    /**
     * Updates an existing room identified by room.getId().
     *
     * @param room Room with updated field values; id must be a valid existing PK
     * @return true if the UPDATE affected exactly one row, false otherwise
     */
    boolean updateRoom(Room room);

    /**
     * Checks whether a room_number already exists in the database, optionally
     * excluding one room by ID (used during edits to allow keeping the same number).
     *
     * @param roomNumber the room number to check
     * @param excludeId  the ID of the room being edited (pass 0 for new rooms)
     * @return true if another room with that number exists
     */
    boolean roomNumberExists(String roomNumber, int excludeId);

    /**
     * Searches rooms using optional filters.
     * All parameters are optional — pass null or empty string to skip a filter.
     *
     * @param roomNumberKeyword partial room number (LIKE %keyword%)
     * @param roomType          exact ENUM value or null/empty
     * @param status            exact ENUM value or null/empty
     * @return matching rooms ordered by room_number ASC
     */
    List<Room> searchRooms(String roomNumberKeyword, String roomType, String status);

    /**
     * Toggles a room's operational_status between AVAILABLE and MAINTENANCE.
     * Will NOT change status if it is currently OCCUPIED (booking module owns that).
     *
     * @param roomId      primary key of the room
     * @param maintenance true → set to MAINTENANCE, false → set to AVAILABLE
     * @return true if the UPDATE was applied, false if blocked (e.g. room is OCCUPIED)
     */
    boolean setMaintenanceStatus(int roomId, boolean maintenance);
}
