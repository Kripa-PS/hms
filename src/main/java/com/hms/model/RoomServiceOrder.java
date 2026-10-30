package com.hms.model;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * RoomServiceOrder
 *
 * One room-service request (row of the 'room_service' table), joined with its
 * booking, guest and room so it can be shown in a table.
 * Status values: REQUESTED, IN_PROGRESS, COMPLETED, CANCELLED.
 */
public class RoomServiceOrder {

    private final int id;
    private final int bookingId;
    private final String bookingReference;
    private final String guestName;
    private final int roomId;
    private final String roomNumber;
    private final String serviceItem;
    private final int quantity;
    private final BigDecimal unitPrice;
    private final BigDecimal totalPrice;
    private final String status;
    private final Timestamp requestedAt;
    private final Timestamp completedAt;

    public RoomServiceOrder(int id, int bookingId, String bookingReference, String guestName,
                            int roomId, String roomNumber, String serviceItem, int quantity,
                            BigDecimal unitPrice, BigDecimal totalPrice, String status,
                            Timestamp requestedAt, Timestamp completedAt) {
        this.id = id;
        this.bookingId = bookingId;
        this.bookingReference = bookingReference;
        this.guestName = guestName;
        this.roomId = roomId;
        this.roomNumber = roomNumber;
        this.serviceItem = serviceItem;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.totalPrice = totalPrice;
        this.status = status;
        this.requestedAt = requestedAt;
        this.completedAt = completedAt;
    }

    public int getId() { return id; }
    public int getBookingId() { return bookingId; }
    public String getBookingReference() { return bookingReference; }
    public String getGuestName() { return guestName; }
    public int getRoomId() { return roomId; }
    public String getRoomNumber() { return roomNumber; }
    public String getServiceItem() { return serviceItem; }
    public int getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public BigDecimal getTotalPrice() { return totalPrice; }
    public String getStatus() { return status; }
    public Timestamp getRequestedAt() { return requestedAt; }
    public Timestamp getCompletedAt() { return completedAt; }
}
