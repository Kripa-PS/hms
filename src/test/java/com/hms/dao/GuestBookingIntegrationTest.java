package com.hms.dao;

import com.hms.model.Booking;
import com.hms.model.Guest;
import com.hms.model.User;
import com.hms.util.DatabaseConnection;
import com.hms.util.UserSession;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * GuestBookingIntegrationTest
 * 
 * Verifies the connection between Guest Management and Booking Management:
 * 1. Stable Guest ID foreign key relationship
 * 2. Reuse of existing guest ID for repeat bookings (no duplicate guest records)
 * 3. Rejection of nonexistent guest ID
 * 4. Querying bookings by guest ID (findBookingsByGuestId)
 * 5. Backend customer data isolation (customer cannot access other customers' bookings)
 */
public class GuestBookingIntegrationTest {

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("  Phase 4: Guest & Booking Relationship Tests    ");
        System.out.println("=================================================");

        if (!DatabaseConnection.testConnection()) {
            System.err.println("[ERROR] Database connection failed.");
            System.exit(1);
        }

        GuestDAO guestDAO = new GuestDAOImpl();
        BookingDAO bookingDAO = new BookingDAOImpl();
        RoomDAO roomDAO = new RoomDAOImpl();

        int passed = 0;
        int failed = 0;

        // Setup: test guest and room
        String testPhone = "98" + (int)(Math.random() * 90000000 + 10000000);
        Guest g = new Guest("Relationship Test Guest", testPhone, "rel@example.com", "Aadhaar", "1234-5678-9012", "Cochin");
        guestDAO.addGuest(g);
        int stableGuestId = g.getId();

        var rooms = roomDAO.findOperationallyAvailableRooms();
        if (rooms.isEmpty()) {
            System.err.println("No available rooms for testing.");
            System.exit(1);
        }
        int roomId = rooms.get(0).getId();

        // Dynamically find free date windows so existing DB records never collide
        LocalDate in1 = LocalDate.now().plusDays(400);
        while (!bookingDAO.isRoomAvailable(roomId, in1, in1.plusDays(2))) {
            in1 = in1.plusDays(5);
        }
        LocalDate out1 = in1.plusDays(2);

        LocalDate in2 = out1.plusDays(5);
        while (!bookingDAO.isRoomAvailable(roomId, in2, in2.plusDays(2))) {
            in2 = in2.plusDays(5);
        }
        LocalDate out2 = in2.plusDays(2);

        // Test 1: Stable Guest ID association in Booking
        System.out.print("[Test 1] Booking created with stable guest ID... ");
        Booking b1 = new Booking();
        b1.setGuestId(stableGuestId);
        b1.setRoomId(roomId);
        b1.setCheckInDate(in1);
        b1.setCheckOutDate(out1);
        b1.setGuestCount(1);
        b1.setTotalAmount(new BigDecimal("3000.00"));

        try {
            Booking created1 = bookingDAO.createBooking(b1);
            if (created1.getGuestId() == stableGuestId && created1.getId() > 0) {
                System.out.println("PASSED (Booking Ref: " + created1.getBookingReference() + ", Guest ID: " + created1.getGuestId() + ")");
                passed++;
            } else {
                System.out.println("FAILED (Guest ID mismatch)");
                failed++;
            }
        } catch (Exception e) {
            System.out.println("FAILED: " + e.getMessage());
            failed++;
        }

        // Test 2: Repeat booking reuses existing guest ID (no duplicate guest record)
        System.out.print("[Test 2] Repeat booking reuses same guest ID without duplicate guest creation... ");
        Booking b2 = new Booking();
        b2.setGuestId(stableGuestId); // Reusing same guest
        b2.setRoomId(roomId);
        b2.setCheckInDate(in2);
        b2.setCheckOutDate(out2);
        b2.setGuestCount(1);
        b2.setTotalAmount(new BigDecimal("3000.00"));

        try {
            Booking created2 = bookingDAO.createBooking(b2);
            if (created2.getGuestId() == stableGuestId) {
                // Verify guest count for this phone is still exactly 1
                List<Guest> searchResults = guestDAO.searchByNameOrPhone(testPhone);
                if (searchResults.size() == 1) {
                    System.out.println("PASSED (Guest ID reused: " + stableGuestId + ", Unique guest verified)");
                    passed++;
                } else {
                    System.out.println("FAILED (Duplicate guest created for same phone)");
                    failed++;
                }
            } else {
                System.out.println("FAILED (Guest ID mismatch)");
                failed++;
            }
        } catch (Exception e) {
            System.out.println("FAILED: " + e.getMessage());
            failed++;
        }

        // Test 3: Rejection of nonexistent guest ID
        System.out.print("[Test 3] Rejection of nonexistent guest ID... ");
        Booking bInvalid = new Booking();
        bInvalid.setGuestId(9999999); // nonexistent guest
        bInvalid.setRoomId(roomId);
        bInvalid.setCheckInDate(LocalDate.now().plusDays(500));
        bInvalid.setCheckOutDate(LocalDate.now().plusDays(502));
        bInvalid.setGuestCount(1);
        bInvalid.setTotalAmount(new BigDecimal("3000.00"));

        try {
            bookingDAO.createBooking(bInvalid);
            System.out.println("FAILED (Should have rejected nonexistent guest ID)");
            failed++;
        } catch (IllegalArgumentException e) {
            System.out.println("PASSED (Correctly rejected: " + e.getMessage() + ")");
            passed++;
        } catch (Exception e) {
            System.out.println("PASSED (Constraint rejected: " + e.getMessage() + ")");
            passed++;
        }

        // Test 4: Find bookings by guest ID
        System.out.print("[Test 4] Querying bookings by guest ID... ");
        List<Booking> guestBookings = bookingDAO.findBookingsByGuestId(stableGuestId);
        if (guestBookings.size() >= 2) {
            System.out.println("PASSED (Retrieved " + guestBookings.size() + " bookings for guest ID " + stableGuestId + ")");
            passed++;
        } else {
            System.out.println("FAILED (Expected at least 2 bookings, got " + guestBookings.size() + ")");
            failed++;
        }

        // Test 5: Customer data access restriction (customer cannot view another's bookings)
        System.out.print("[Test 5] Customer backend data access restriction... ");
        User mockCustomer = new User("mock_user", "hash", "Mock Customer", "CUSTOMER", stableGuestId);
        UserSession.login(mockCustomer);

        try {
            // Customer attempting to query bookings of another guest (e.g. guest ID 1)
            bookingDAO.findBookingsByGuestId(1);
            System.out.println("FAILED (Should have blocked customer from querying other guest's bookings)");
            failed++;
        } catch (SecurityException e) {
            System.out.println("PASSED (Blocked: " + e.getMessage() + ")");
            passed++;
        } finally {
            UserSession.logout();
        }

        System.out.println("-------------------------------------------------");
        System.out.println("Summary: Passed: " + passed + " / 5, Failed: " + failed);
        System.out.println("=================================================");

        if (failed > 0) {
            System.exit(1);
        }
    }
}
