package com.hms.model;

/**
 * StayOption
 *
 * One entry of the "which booking is this order for?" drop-down in the Room Service screen.
 * Shows as: Room 201 - Rahul Sharma (HMS-2026-1001)
 */
public class StayOption {

    private final int bookingId;
    private final int roomId;
    private final String label;

    public StayOption(int bookingId, int roomId, String roomNumber, String guestName, String bookingReference) {
        this.bookingId = bookingId;
        this.roomId = roomId;
        this.label = "Room " + roomNumber + " - " + guestName + " (" + bookingReference + ")";
    }

    public int getBookingId() { return bookingId; }
    public int getRoomId() { return roomId; }

    @Override
    public String toString() { return label; }
}
