package com.hms.dao;

import com.hms.model.Booking;
import com.hms.model.Guest;
import com.hms.model.Room;
import com.hms.util.DatabaseConnection;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * BookingManagementTest
 * 
 * Comprehensive automated test suite for Booking Management (Stage 5).
 * Tests against live MySQL:
 * 1. Valid booking creation & reference generation
 * 2. Invalid dates rejection (checkOut <= checkIn)
 * 3. Past dates rejection (checkIn < today)
 * 4. Overlapping booking rejection (existingCheckIn < requestedCheckOut AND existingCheckOut > requestedCheckIn)
 * 5. Back-to-back booking allowed (checkIn == existingCheckOut)
 * 6. Room capacity enforcement (guestCount > capacity rejected)
 * 7. Cancellation & date release
 * 8. Persistence verification
 */
public class BookingManagementTest {

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("  Stage 5: Booking Management Verification Tests");
        System.out.println("=================================================");

        // Pre-check: Database connectivity
        if (!DatabaseConnection.testConnection()) {
            System.err.println("[ERROR] MySQL connection is offline. Aborting live tests.");
            System.exit(1);
        }

        BookingDAO bookingDAO = new BookingDAOImpl();
        GuestDAO guestDAO = new GuestDAOImpl();
        RoomDAO roomDAO = new RoomDAOImpl();

        int passed = 0;
        int failed = 0;

        // Setup test guest and test room
        System.out.print("[Setup] Preparing test guest and room... ");
        String testPhone = "98888" + (int)(Math.random() * 90000 + 10000);
        Guest testGuest = new Guest("Test Guest " + System.currentTimeMillis(), testPhone, "test@hotel.com", "Aadhaar", "1234", "Kochi");
        guestDAO.addGuest(testGuest);

        List<Room> rooms = roomDAO.findOperationallyAvailableRooms();
        if (rooms.isEmpty()) {
            System.err.println("FAILED (No available rooms in database to run tests against).");
            System.exit(1);
        }
        Room testRoom = rooms.get(0); // e.g. Room 101 or 102
        System.out.println("READY (Guest ID: " + testGuest.getId() + ", Room: " + testRoom.getRoomNumber() + ", Capacity: " + testRoom.getCapacity() + ")");

        // Test 1: Valid Booking Creation
        System.out.print("[Test 1] Valid Booking Creation... ");
        int offset = 30 + (int)(Math.random() * 500);
        LocalDate in1 = LocalDate.now().plusDays(offset);
        LocalDate out1 = in1.plusDays(3); // 3 nights
        Booking b1 = new Booking();
        b1.setGuestId(testGuest.getId());
        b1.setRoomId(testRoom.getId());
        b1.setCheckInDate(in1);
        b1.setCheckOutDate(out1);
        b1.setGuestCount(1);

        try {
            Booking created = bookingDAO.createBooking(b1);
            if (created.getId() > 0 && created.getBookingReference() != null && created.getBookingReference().startsWith("HMS-2026-")) {
                System.out.println("PASSED (Ref: " + created.getBookingReference() + ", Total: ₹" + created.getTotalAmount() + ")");
                passed++;
            } else {
                System.out.println("FAILED (Missing ID or Reference)");
                failed++;
            }
        } catch (Exception e) {
            System.out.println("FAILED: " + e.getMessage());
            failed++;
        }

        // Test 2: Invalid Date Rejection (checkOut <= checkIn)
        System.out.print("[Test 2] Invalid Date Rejection (Check-out before Check-in)... ");
        try {
            Booking bInvalid = new Booking();
            bInvalid.setGuestId(testGuest.getId());
            bInvalid.setRoomId(testRoom.getId());
            bInvalid.setCheckInDate(LocalDate.now().plusDays(5));
            bInvalid.setCheckOutDate(LocalDate.now().plusDays(3)); // Invalid!
            bInvalid.setGuestCount(1);

            bookingDAO.createBooking(bInvalid);
            System.out.println("FAILED (Should have thrown InvalidBookingDateException)");
            failed++;
        } catch (BookingDAO.InvalidBookingDateException e) {
            System.out.println("PASSED (Correctly rejected: " + e.getMessage() + ")");
            passed++;
        } catch (Exception e) {
            System.out.println("FAILED (Unexpected exception: " + e + ")");
            failed++;
        }

        // Test 3: Past Date Rejection
        System.out.print("[Test 3] Past Date Rejection (Check-in in the past)... ");
        try {
            Booking bPast = new Booking();
            bPast.setGuestId(testGuest.getId());
            bPast.setRoomId(testRoom.getId());
            bPast.setCheckInDate(LocalDate.now().minusDays(2)); // In the past!
            bPast.setCheckOutDate(LocalDate.now().plusDays(2));
            bPast.setGuestCount(1);

            bookingDAO.createBooking(bPast);
            System.out.println("FAILED (Should have thrown InvalidBookingDateException)");
            failed++;
        } catch (BookingDAO.InvalidBookingDateException e) {
            System.out.println("PASSED (Correctly rejected: " + e.getMessage() + ")");
            passed++;
        } catch (Exception e) {
            System.out.println("FAILED (Unexpected exception: " + e + ")");
            failed++;
        }

        // Test 4: Overlapping Booking Rejection
        System.out.print("[Test 4] Overlapping Booking Rejection... ");
        try {
            // b1 is booked for in1 to out1. We attempt in1 + 1 to out1 + 1 (overlap!)
            Booking bOverlap = new Booking();
            bOverlap.setGuestId(testGuest.getId());
            bOverlap.setRoomId(testRoom.getId());
            bOverlap.setCheckInDate(in1.plusDays(1));
            bOverlap.setCheckOutDate(out1.plusDays(1));
            bOverlap.setGuestCount(1);

            bookingDAO.createBooking(bOverlap);
            System.out.println("FAILED (Overlapping booking was not blocked!)");
            failed++;
        } catch (BookingDAO.BookingOverlapException e) {
            System.out.println("PASSED (Correctly blocked overlap: " + e.getMessage() + ")");
            passed++;
        } catch (Exception e) {
            System.out.println("FAILED (Unexpected exception: " + e + ")");
            failed++;
        }

        // Test 5: Back-to-Back Booking Allowed (Check-in on check-out date of previous booking)
        System.out.print("[Test 5] Back-to-Back Booking (Check-in = Previous Check-out)... ");
        try {
            // b1 ends on out1. bBack starts on out1 and ends on out1 + 2.
            Booking bBack = new Booking();
            bBack.setGuestId(testGuest.getId());
            bBack.setRoomId(testRoom.getId());
            bBack.setCheckInDate(out1); // Same as out1!
            bBack.setCheckOutDate(out1.plusDays(2));
            bBack.setGuestCount(1);

            Booking createdBack = bookingDAO.createBooking(bBack);
            if (createdBack.getId() > 0) {
                System.out.println("PASSED (Back-to-back allowed, Ref: " + createdBack.getBookingReference() + ")");
                passed++;
            } else {
                System.out.println("FAILED");
                failed++;
            }
        } catch (Exception e) {
            System.out.println("FAILED: " + e.getMessage());
            failed++;
        }

        // Test 6: Room Capacity Enforcement
        System.out.print("[Test 6] Room Capacity Enforcement... ");
        try {
            Booking bOverCap = new Booking();
            bOverCap.setGuestId(testGuest.getId());
            bOverCap.setRoomId(testRoom.getId());
            bOverCap.setCheckInDate(LocalDate.now().plusDays(40));
            bOverCap.setCheckOutDate(LocalDate.now().plusDays(42));
            bOverCap.setGuestCount(testRoom.getCapacity() + 5); // Exceeds capacity!

            bookingDAO.createBooking(bOverCap);
            System.out.println("FAILED (Over-capacity was not blocked!)");
            failed++;
        } catch (IllegalArgumentException e) {
            System.out.println("PASSED (Blocked over-capacity: " + e.getMessage() + ")");
            passed++;
        } catch (Exception e) {
            System.out.println("FAILED (Unexpected exception: " + e + ")");
            failed++;
        }

        // Test 7: Cancellation & Availability Release
        System.out.print("[Test 7] Cancellation & Availability Release... ");
        try {
            boolean cancelled = bookingDAO.cancelBooking(b1.getId());
            if (!cancelled) {
                System.out.println("FAILED to cancel b1");
                failed++;
            } else {
                // b1 (+30 to +33) is now CANCELLED. We should be able to book those dates again!
                Booking bRebook = new Booking();
                bRebook.setGuestId(testGuest.getId());
                bRebook.setRoomId(testRoom.getId());
                bRebook.setCheckInDate(in1);
                bRebook.setCheckOutDate(out1);
                bRebook.setGuestCount(1);

                Booking createdRebook = bookingDAO.createBooking(bRebook);
                if (createdRebook.getId() > 0) {
                    System.out.println("PASSED (Dates successfully released and re-booked: " + createdRebook.getBookingReference() + ")");
                    passed++;
                } else {
                    System.out.println("FAILED rebook");
                    failed++;
                }
            }
        } catch (Exception e) {
            System.out.println("FAILED: " + e.getMessage());
            failed++;
        }

        // Test 8: Persistence & Lookup Verification
        System.out.print("[Test 8] Database Persistence & Reference Lookup... ");
        try {
            Optional<Booking> lookup = bookingDAO.findById(b1.getId());
            if (lookup.isPresent() && "CANCELLED".equals(lookup.get().getStatus())) {
                System.out.println("PASSED (Record verified in MySQL with status 'CANCELLED')");
                passed++;
            } else {
                System.out.println("FAILED (Record mismatch)");
                failed++;
            }
        } catch (Exception e) {
            System.out.println("FAILED: " + e.getMessage());
            failed++;
        }

        System.out.println("-------------------------------------------------");
        System.out.println("Summary: Passed: " + passed + " / " + (passed + failed) + ", Failed: " + failed);
        System.out.println("=================================================");

        if (failed == 0) {
            System.out.println("ALL STAGE 5 BOOKING MANAGEMENT TESTS PASSED!");
        } else {
            System.exit(1);
        }
    }
}
