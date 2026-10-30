package com.hms.model;

import java.math.BigDecimal;

/**
 * Room Model (Minimal Reference Model)
 * 
 * Provides room details (room number, capacity, price per night, status)
 * needed by the Booking module to validate dates, guest limits, and compute charges.
 * 
 * Corresponds to the 'rooms' table in MySQL.
 */
public class Room {

    private int id;
    private String roomNumber;
    private String roomType;           // 'SINGLE', 'DOUBLE', 'DELUXE', 'SUITE'
    private int capacity;              // Max guests allowed
    private BigDecimal pricePerNight;  // Decimal price per night
    private String operationalStatus;  // 'AVAILABLE', 'OCCUPIED', 'MAINTENANCE'
    private String amenities;

    public Room() {}

    public Room(int id, String roomNumber, String roomType, int capacity, BigDecimal pricePerNight, String operationalStatus) {
        this.id = id;
        this.roomNumber = roomNumber;
        this.roomType = roomType;
        this.capacity = capacity;
        this.pricePerNight = pricePerNight;
        this.operationalStatus = operationalStatus;
    }

    public Room(String roomNumber, String roomType, int capacity, BigDecimal pricePerNight, String operationalStatus, String amenities) {
        this.roomNumber = roomNumber;
        this.roomType = roomType;
        this.capacity = capacity;
        this.pricePerNight = pricePerNight;
        this.operationalStatus = operationalStatus;
        this.amenities = amenities;
    }

    public boolean isOperationallyAvailable() {
        return "AVAILABLE".equalsIgnoreCase(operationalStatus);
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public BigDecimal getPricePerNight() {
        return pricePerNight;
    }

    public void setPricePerNight(BigDecimal pricePerNight) {
        this.pricePerNight = pricePerNight;
    }

    public String getOperationalStatus() {
        return operationalStatus;
    }

    public void setOperationalStatus(String operationalStatus) {
        this.operationalStatus = operationalStatus;
    }

    public String getAmenities() {
        return amenities;
    }

    public void setAmenities(String amenities) {
        this.amenities = amenities;
    }

    @Override
    public String toString() {
        return "Room " + roomNumber + " (" + roomType + ", Rs. " + pricePerNight + "/night)";
    }
}
