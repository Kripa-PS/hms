package com.hms.dao;

import com.hms.model.Room;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * RoomDAO Interface
 * 
 * Minimal room data access interface needed by the Booking module.
 * Provides room queries without taking over or replacing Teammate A's module.
 */
public interface RoomDAO {

    /**
     * Finds all rooms in the system.
     */
    List<Room> findAll();

    /**
     * Finds all rooms with operational_status = 'AVAILABLE' (not under maintenance).
     */
    List<Room> findOperationallyAvailableRooms();

    /**
     * Finds rooms that are BOTH operationally available AND not booked for the given date range.
     * Enforces the overlap check:
     * NOT EXISTS (booking WHERE status IN ('CONFIRMED', 'CHECKED_IN')
     *             AND check_in_date < checkOut AND check_out_date > checkIn)
     */
    List<Room> findAvailableRoomsForDates(LocalDate checkIn, LocalDate checkOut);

    /**
     * Finds a room by its primary key ID.
     */
    Optional<Room> findById(int id);

    /**
     * Finds a room by its unique room number.
     */
    Optional<Room> findByRoomNumber(String roomNumber);
}
