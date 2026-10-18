package com.hms.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class ModelTest {
    public static void main(String[] args) {
        System.out.println("[ModelTest] Starting model verification...");

        // 1. Test Guest
        Guest g = new Guest("Rahul Sharma", "9876543210", "rahul@example.com", "Aadhaar", "1234-5678-9012", "Kochi");
        assert g.isValid() : "Guest should be valid";
        assert "Rahul Sharma".equals(g.getFullName()) : "Name mismatch";
        System.out.println("  ✓ Guest model verified: " + g);

        // 2. Test Room
        Room r = new Room(1, "101", "SINGLE", 1, new BigDecimal("1500.00"), "AVAILABLE");
        assert r.isOperationallyAvailable() : "Room should be available";
        assert r.getCapacity() == 1 : "Capacity mismatch";
        System.out.println("  ✓ Room model verified: " + r);

        // 3. Test RoomHold
        LocalDateTime fiveMinsLater = LocalDateTime.now().plusMinutes(5);
        RoomHold hold = new RoomHold("test-token-123", 1, LocalDate.now(), LocalDate.now().plusDays(2), fiveMinsLater);
        assert hold.isValid() : "Hold should be active and valid";
        assert !hold.isExpired() : "Hold should not be expired yet";
        assert hold.getRemainingSeconds() > 280 : "Remaining seconds should be close to 300";
        System.out.println("  ✓ RoomHold model verified: " + hold);

        // 4. Test Booking
        LocalDate inDate = LocalDate.now().plusDays(1);
        LocalDate outDate = LocalDate.now().plusDays(4); // 3 nights
        Booking b = new Booking("HMS-2026-9999", 1, 1, inDate, outDate, 1, new BigDecimal("4500.00"));
        assert b.areDatesValid() : "Dates should be valid";
        assert b.getNumberOfNights() == 3 : "Nights calculation should be 3, got: " + b.getNumberOfNights();
        assert b.isActive() : "Booking status should be active";
        System.out.println("  ✓ Booking model verified: " + b + " (Nights: " + b.getNumberOfNights() + ")");

        System.out.println("[ModelTest] ALL MODEL TESTS PASSED SUCCESSFULLY!");
    }
}
