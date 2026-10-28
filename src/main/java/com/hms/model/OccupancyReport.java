package com.hms.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * OccupancyReport
 * 
 * Represents daily hotel room occupancy statistics for a selected date.
 * Enforces business rules:
 * - Total rooms = available + occupied + maintenance
 * - Occupancy % = (Occupied Rooms / Operational Rooms) * 100
 * - Does not count cancelled or failed bookings as occupied.
 */
public class OccupancyReport {

    private LocalDate targetDate;
    private int totalRooms;
    private int occupiedRooms;
    private int availableRooms;
    private int maintenanceRooms;
    private BigDecimal occupancyPercentage;
    private List<OccupiedRoomDetail> occupiedDetails;

    public OccupancyReport() {
        this.targetDate = LocalDate.now();
        this.occupancyPercentage = BigDecimal.ZERO;
        this.occupiedDetails = new ArrayList<>();
    }

    public OccupancyReport(LocalDate targetDate, int totalRooms, int occupiedRooms, 
                           int availableRooms, int maintenanceRooms) {
        this.targetDate = targetDate;
        this.totalRooms = totalRooms;
        this.occupiedRooms = occupiedRooms;
        this.availableRooms = availableRooms;
        this.maintenanceRooms = maintenanceRooms;
        this.occupiedDetails = new ArrayList<>();
        calculateOccupancyRate();
    }

    public void calculateOccupancyRate() {
        int operationalRooms = totalRooms - maintenanceRooms;
        if (operationalRooms > 0 && occupiedRooms > 0) {
            this.occupancyPercentage = BigDecimal.valueOf(occupiedRooms)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(operationalRooms), 2, RoundingMode.HALF_UP);
        } else {
            this.occupancyPercentage = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
    }

    public LocalDate getTargetDate() {
        return targetDate;
    }

    public void setTargetDate(LocalDate targetDate) {
        this.targetDate = targetDate;
    }

    public int getTotalRooms() {
        return totalRooms;
    }

    public void setTotalRooms(int totalRooms) {
        this.totalRooms = totalRooms;
    }

    public int getOccupiedRooms() {
        return occupiedRooms;
    }

    public void setOccupiedRooms(int occupiedRooms) {
        this.occupiedRooms = occupiedRooms;
    }

    public int getAvailableRooms() {
        return availableRooms;
    }

    public void setAvailableRooms(int availableRooms) {
        this.availableRooms = availableRooms;
    }

    public int getMaintenanceRooms() {
        return maintenanceRooms;
    }

    public void setMaintenanceRooms(int maintenanceRooms) {
        this.maintenanceRooms = maintenanceRooms;
    }

    public BigDecimal getOccupancyPercentage() {
        return occupancyPercentage;
    }

    public void setOccupancyPercentage(BigDecimal occupancyPercentage) {
        this.occupancyPercentage = occupancyPercentage;
    }

    public List<OccupiedRoomDetail> getOccupiedDetails() {
        return occupiedDetails;
    }

    public void setOccupiedDetails(List<OccupiedRoomDetail> occupiedDetails) {
        this.occupiedDetails = occupiedDetails;
    }

    /**
     * Inner class representing details of a room occupied on this date.
     */
    public static class OccupiedRoomDetail {
        private String roomNumber;
        private String roomType;
        private String bookingReference;
        private String guestName;
        private LocalDate checkInDate;
        private LocalDate checkOutDate;

        public OccupiedRoomDetail() {}

        public OccupiedRoomDetail(String roomNumber, String roomType, String bookingReference, 
                                  String guestName, LocalDate checkInDate, LocalDate checkOutDate) {
            this.roomNumber = roomNumber;
            this.roomType = roomType;
            this.bookingReference = bookingReference;
            this.guestName = guestName;
            this.checkInDate = checkInDate;
            this.checkOutDate = checkOutDate;
        }

        public String getRoomNumber() {
            return roomNumber;
        }

        public String getRoomType() {
            return roomType;
        }

        public String getBookingReference() {
            return bookingReference;
        }

        public String getGuestName() {
            return guestName;
        }

        public LocalDate getCheckInDate() {
            return checkInDate;
        }

        public LocalDate getCheckOutDate() {
            return checkOutDate;
        }
    }
}
