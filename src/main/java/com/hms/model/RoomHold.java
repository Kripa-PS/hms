package com.hms.model;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * RoomHold Model (5-Minute Ghost-Inventory Prevention)
 * 
 * Represents a temporary 5-minute hold on a room to prevent two users
 * from booking the same room for overlapping dates simultaneously.
 * 
 * Corresponds to the 'room_holds' table in MySQL.
 */
public class RoomHold {

    private int id;
    private String holdToken;          // Unique UUID token
    private int roomId;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private LocalDateTime expiresAt;   // Server-side expiry time (created_at + 5 minutes)
    private String status;             // 'ACTIVE', 'CONSUMED', 'CANCELLED', 'EXPIRED'
    private LocalDateTime createdAt;

    // Additional display field
    private String roomNumber;

    public RoomHold() {}

    public RoomHold(String holdToken, int roomId, LocalDate checkInDate, LocalDate checkOutDate, LocalDateTime expiresAt) {
        this.holdToken = holdToken;
        this.roomId = roomId;
        this.checkInDate = checkInDate;
        this.checkOutDate = checkOutDate;
        this.expiresAt = expiresAt;
        this.status = "ACTIVE";
        this.createdAt = LocalDateTime.now();
    }

    public RoomHold(int id, String holdToken, int roomId, LocalDate checkInDate, 
                    LocalDate checkOutDate, LocalDateTime expiresAt, String status, LocalDateTime createdAt) {
        this.id = id;
        this.holdToken = holdToken;
        this.roomId = roomId;
        this.checkInDate = checkInDate;
        this.checkOutDate = checkOutDate;
        this.expiresAt = expiresAt;
        this.status = status;
        this.createdAt = createdAt;
    }

    /**
     * Checks if this hold has expired based on current server time.
     */
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Checks if the hold is still valid and unconsumed.
     */
    public boolean isValid() {
        return "ACTIVE".equalsIgnoreCase(status) && !isExpired();
    }

    /**
     * Computes remaining seconds for the 5-minute UI countdown timer.
     * Returns 0 if already expired.
     */
    public long getRemainingSeconds() {
        if (expiresAt == null) return 0;
        long secs = Duration.between(LocalDateTime.now(), expiresAt).getSeconds();
        return Math.max(0, secs);
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getHoldToken() {
        return holdToken;
    }

    public void setHoldToken(String holdToken) {
        this.holdToken = holdToken;
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

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    @Override
    public String toString() {
        return "Hold[" + holdToken + ", Room " + roomId + ", Status=" + status + ", Remaining=" + getRemainingSeconds() + "s]";
    }
}
