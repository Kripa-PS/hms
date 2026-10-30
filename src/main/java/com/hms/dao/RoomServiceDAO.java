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
     * Finds active orderable stays for a specific customer guest ID.
     * Enforces customer data isolation.
     */
    List<StayOption> findActiveStaysForGuest(int guestId);

    /**
     * Adds a new order with status REQUESTED. The room is taken from the booking, and
     * total_price = unitPrice x quantity is calculated here with BigDecimal.
     *
     * @return true if the INSERT succeeded (false for bad input, an unsuitable booking, or a database error)
     */
    boolean addOrder(int bookingId, String serviceItem, int quantity, BigDecimal unitPrice);

    /**
     * Adds a new order placed by an authenticated customer.
     * Strictly verifies in the database that bookingId belongs to guestId and is CONFIRMED or CHECKED_IN.
     * Combines item and optional special instructions safely.
     *
     * @return true if order was created, false otherwise.
     */
    boolean addCustomerOrder(int guestId, int bookingId, String serviceItem, int quantity, BigDecimal unitPrice, String specialInstructions);

    /**
     * Returns orders (newest first) joined with booking reference, guest name and room number.
     * Restricted to staff/admin callers.
     *
     * @param status REQUESTED, IN_PROGRESS, COMPLETED or CANCELLED; pass null or "ALL" for every order
     */
    List<RoomServiceOrder> findAll(String status);

    /**
     * Returns room service orders belonging to a specific customer guest ID.
     * Enforces customer data isolation in the backend.
     */
    List<RoomServiceOrder> findOrdersByGuestId(int guestId);

    /**
     * Changes an order's status. completed_at is set when the new status is COMPLETED.
     *
     * @return true if exactly one row was updated
     */
    boolean updateStatus(int orderId, String newStatus);

    /**
     * Allows a customer to cancel their own order if and only if it belongs to their active booking
     * and its current status is 'REQUESTED'.
     */
    boolean cancelCustomerOrder(int guestId, int orderId);

    /** Number of orders with the given status, or -1 if the database could not be read. */
    int countByStatus(String status);
}
