package com.hms.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Booking Model
 * 
 * Represents a hotel room reservation or guest stay.
 * Corresponds to the 'bookings' table in MySQL.
 * Demonstrates OOP Encapsulation and business logic methods.
 */
public class Booking {

    private int id;
    private String bookingReference;   // e.g. "HMS-2026-1001"
    private int guestId;
    private int roomId;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private int guestCount;
    private String status;             // 'CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT', 'CANCELLED'
    private BigDecimal totalAmount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime cancelledAt;

    // Joined / Convenient UI Display fields
    private String guestName;
    private String guestPhone;
    private String roomNumber;
    private String roomType;

    public Booking() {
        this.status = "CONFIRMED";
        this.guestCount = 1;
    }

    public Booking(String bookingReference, int guestId, int roomId, LocalDate checkInDate, 
                   LocalDate checkOutDate, int guestCount, BigDecimal totalAmount) {
        this.bookingReference = bookingReference;
        this.guestId = guestId;
        this.roomId = roomId;
        this.checkInDate = checkInDate;
        this.checkOutDate = checkOutDate;
        this.guestCount = guestCount;
        this.status = "CONFIRMED";
        this.totalAmount = totalAmount;
    }

    public Booking(int id, String bookingReference, int guestId, int roomId, LocalDate checkInDate, 
                   LocalDate checkOutDate, int guestCount, String status, BigDecimal totalAmount, 
                   LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime cancelledAt) {
        this.id = id;
        this.bookingReference = bookingReference;
        this.guestId = guestId;
        this.roomId = roomId;
        this.checkInDate = checkInDate;
        this.checkOutDate = checkOutDate;
        this.guestCount = guestCount;
        this.status = status;
        this.totalAmount = totalAmount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.cancelledAt = cancelledAt;
    }

    /**
     * Calculates the number of nights stayed between checkInDate and checkOutDate.
     * Minimum 1 night.
     */
    public long getNumberOfNights() {
        if (checkInDate == null || checkOutDate == null) {
            return 0;
        }
        long nights = ChronoUnit.DAYS.between(checkInDate, checkOutDate);
        return Math.max(1, nights);
    }

    /**
     * Validates date consistency: check-out must be strictly after check-in.
     */
    public boolean areDatesValid() {
        return checkInDate != null && checkOutDate != null && checkOutDate.isAfter(checkInDate);
    }

    /**
     * Checks if this booking is currently active (blocking dates).
     * Cancelled bookings do not block dates.
     */
    public boolean isActive() {
        return "CONFIRMED".equalsIgnoreCase(status) || "CHECKED_IN".equalsIgnoreCase(status);
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getBookingReference() {
        return bookingReference;
    }

    public void setBookingReference(String bookingReference) {
        this.bookingReference = bookingReference;
    }

    public int getGuestId() {
        return guestId;
    }

    public void setGuestId(int guestId) {
        this.guestId = guestId;
    }

    public int getRoomId() {
        return roomId;
    }

    public void setRoomId(int roomId) {
        this.roomId = roomId;
    }

    public LocalDate getCheckInDate() {
        return checkInDate;
    }

    public void setCheckInDate(LocalDate checkInDate) {
        this.checkInDate = checkInDate;
    }

    public LocalDate getCheckOutDate() {
        return checkOutDate;
    }

    public void setCheckOutDate(LocalDate checkOutDate) {
        this.checkOutDate = checkOutDate;
    }

    public int getGuestCount() {
        return guestCount;
    }

    public void setGuestCount(int guestCount) {
        this.guestCount = guestCount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public String getGuestName() {
        return guestName;
    }

    public void setGuestName(String guestName) {
        this.guestName = guestName;
    }

    public String getGuestPhone() {
        return guestPhone;
    }

    public void setGuestPhone(String guestPhone) {
        this.guestPhone = guestPhone;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public String getRoomType() {
        return roomType;
    }

    public void setRoomType(String roomType) {
        this.roomType = roomType;
    }

    @Override
    public String toString() {
        return "Booking[" + bookingReference + ", Room " + roomNumber + ", Guest: " + guestName + ", " + checkInDate + " to " + checkOutDate + ", " + status + "]";
    }
}
