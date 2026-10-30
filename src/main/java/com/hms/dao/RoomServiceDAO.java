package com.hms.dao;

import com.hms.model.RoomServiceOrder;
import com.hms.model.StayOption;

import java.math.BigDecimal;
import java.util.List;

/**
 * RoomServiceDAO Interface
 *
 * Data-access contract for the 'room_service' table (Member 4: Services).
 * Every order belongs to a booking (and therefore a room and a guest).
 */
public interface RoomServiceDAO {

    /** Bookings that can receive room service: status CONFIRMED or CHECKED_IN, ordered by room number. */
    List<StayOption> findOrderableStays();

    /**
     * Adds a new order with status REQUESTED. The room is taken from the booking, and
     * total_price = unitPrice x quantity is calculated here with BigDecimal.
     *
     * @return true if the INSERT succeeded (false for bad input, an unsuitable booking, or a database error)
     */
    boolean addOrder(int bookingId, String serviceItem, int quantity, BigDecimal unitPrice);

    /**
     * Returns orders (newest first) joined with booking reference, guest name and room number.
     *
     * @param status REQUESTED, IN_PROGRESS, COMPLETED or CANCELLED; pass null or "ALL" for every order
     */
    List<RoomServiceOrder> findAll(String status);

    /**
     * Changes an order's status. completed_at is set when the new status is COMPLETED.
     *
     * @return true if exactly one row was updated
     */
    boolean updateStatus(int orderId, String newStatus);

    /** Number of orders with the given status, or -1 if the database could not be read. */
    int countByStatus(String status);
}
