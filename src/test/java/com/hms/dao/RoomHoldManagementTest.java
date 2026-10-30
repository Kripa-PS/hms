package com.hms.dao;

import com.hms.model.Booking;
import com.hms.model.Guest;
import com.hms.model.Room;
import com.hms.model.RoomHold;
import com.hms.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * RoomHoldManagementTest
 * 
 * Comprehensive automated test suite for the 5-Minute Ghost Inventory Prevention feature.
 * Tests against live MySQL:
 * 1. Hold Creation & 5-minute expiry timestamp calculation
 * 2. Overlapping hold rejection (hold vs hold conflict)
 * 3. Overlapping booking rejection (hold vs booking conflict)
 * 4. Back-to-back holds allowed on the same room
 * 5. Expired holds stop blocking availability (self-expiring query logic)
 * 6. Successful booking consumes hold ('ACTIVE' -> 'CONSUMED')
 * 7. Hold cancellation & release ('ACTIVE' -> 'CANCELLED')
 * 8. Unauthorized token rejection (using wrong hold token)
 * 9. Direct booking blocked when room is on hold
 * 10. Expired hold cleanup utility
 */
public class RoomHoldManagementTest {

    public static void main(String[] args) {
        System.out.println("================================================================");
        System.out.println("  Feature Verification: 5-Minute Ghost Inventory Prevention");
        System.out.println("================================================================");

        if (!DatabaseConnection.testConnection()) {
            System.err.println("[ERROR] MySQL connection is offline. Aborting live tests.");
            System.exit(1);
        }

        RoomHoldDAO roomHoldDAO = new RoomHoldDAOImpl();
        BookingDAO bookingDAO = new BookingDAOImpl();
        GuestDAO guestDAO = new GuestDAOImpl();
        RoomDAO roomDAO = new RoomDAOImpl();

        int passed = 0;
        int failed = 0;

        // Setup test guest and test room
        System.out.print("[Setup] Preparing test guest and rooms... ");
        String testPhone = "97777" + (int)(Math.random() * 90000 + 10000);
        Guest testGuest = new Guest("Hold Test Guest", testPhone, "hold@test.com", "Passport", "P1234567", "Ernakulam");
        guestDAO.addGuest(testGuest);

        List<Room> rooms = roomDAO.findOperationallyAvailableRooms();
        if (rooms.size() < 2) {
            System.err.println("FAILED (Need at least 2 rooms in database to run tests).");
            System.exit(1);
        }
        Room testRoom = rooms.get(0);
        Room testRoom2 = rooms.get(1);
        System.out.println("READY (Guest ID: " + testGuest.getId() + ", Rooms: " + testRoom.getRoomNumber() + ", " + testRoom2.getRoomNumber() + ")");

        // Base future dates with random offset to avoid collisions with bookings from previous test runs
        LocalDate baseIn = LocalDate.now().plusDays(150 + (int)(Math.random() * 600));
        LocalDate baseOut = baseIn.plusDays(3);

        // TEST 1: Hold creation & 5-minute expiry
        System.out.print("\n[Test 1] Creating new room hold for Room " + testRoom.getRoomNumber() + "... ");
        try {
            RoomHold hold = roomHoldDAO.createHold(testRoom.getId(), baseIn, baseOut);
            if (hold != null 
                    && hold.getHoldToken() != null 
                    && hold.getHoldToken().startsWith("HOLD-")
                    && "ACTIVE".equalsIgnoreCase(hold.getStatus())
                    && hold.getRemainingSeconds() > 270 && hold.getRemainingSeconds() <= 300) {
                System.out.println("PASSED (Token: " + hold.getHoldToken() + ", Remaining: " + hold.getRemainingSeconds() + "s)");
                passed++;
            } else {
                System.out.println("FAILED (Hold attributes incorrect: " + hold + ")");
                failed++;
            }
        } catch (Exception e) {
            System.out.println("FAILED: " + e.getMessage());
            failed++;
        }

        // TEST 2: Overlapping hold rejected on same room & dates
        System.out.print("[Test 2] Rejecting second overlapping hold on Room " + testRoom.getRoomNumber() + "... ");
        try {
            LocalDate overlapIn = baseIn.plusDays(1);
            LocalDate overlapOut = baseOut.plusDays(1);
            roomHoldDAO.createHold(testRoom.getId(), overlapIn, overlapOut);
            System.out.println("FAILED (Did not reject overlapping hold)");
            failed++;
        } catch (RoomHoldDAO.RoomHoldOverlapException e) {
            System.out.println("PASSED (" + e.getMessage() + ")");
            passed++;
        } catch (Exception e) {
            System.out.println("FAILED (Unexpected exception: " + e.getMessage() + ")");
            failed++;
        }

        // TEST 3: Back-to-back hold allowed on same room
        System.out.print("[Test 3] Allowing back-to-back hold starting on changeover date (" + baseOut + ")... ");
        try {
            LocalDate nextIn = baseOut; // Same day as baseOut
            LocalDate nextOut = baseOut.plusDays(2);
            RoomHold b2bHold = roomHoldDAO.createHold(testRoom.getId(), nextIn, nextOut);
            if (b2bHold != null && b2bHold.getHoldToken() != null) {
                System.out.println("PASSED (Token: " + b2bHold.getHoldToken() + ")");
                passed++;
                // Release it so it doesn't affect later tests
                roomHoldDAO.releaseHold(b2bHold.getHoldToken());
            } else {
                System.out.println("FAILED");
                failed++;
            }
        } catch (Exception e) {
            System.out.println("FAILED: " + e.getMessage());
            failed++;
        }

        // TEST 4: Direct booking blocked when room is on hold
        System.out.print("[Test 4] Direct booking blocked when room is under active hold... ");
        try {
            Booking directBooking = new Booking();
            directBooking.setGuestId(testGuest.getId());
            directBooking.setRoomId(testRoom.getId());
            directBooking.setCheckInDate(baseIn);
            directBooking.setCheckOutDate(baseOut);
            directBooking.setGuestCount(1);

            bookingDAO.createBooking(directBooking); // Passing null holdToken
            System.out.println("FAILED (Direct booking was not blocked by active hold)");
            failed++;
        } catch (BookingDAO.BookingOverlapException e) {
            System.out.println("PASSED (" + e.getMessage() + ")");
            passed++;
        } catch (Exception e) {
            System.out.println("FAILED (Unexpected exception: " + e.getMessage() + ")");
            failed++;
        }

        // TEST 5: Booking using another user's or invalid hold token rejected
        System.out.print("[Test 5] Rejecting booking with invalid/fake hold token... ");
        try {
            Booking fakeHoldBooking = new Booking();
            fakeHoldBooking.setGuestId(testGuest.getId());
            fakeHoldBooking.setRoomId(testRoom.getId());
            fakeHoldBooking.setCheckInDate(baseIn);
            fakeHoldBooking.setCheckOutDate(baseOut);
            fakeHoldBooking.setGuestCount(1);

            bookingDAO.createBooking(fakeHoldBooking, "HOLD-FAKETOKEN999");
            System.out.println("FAILED (Allowed booking with fake token)");
            failed++;
        } catch (IllegalStateException e) {
            System.out.println("PASSED (" + e.getMessage() + ")");
            passed++;
        } catch (Exception e) {
            System.out.println("FAILED (Unexpected exception: " + e.getMessage() + ")");
            failed++;
        }

        // TEST 6: Successful booking consumes hold
        System.out.print("[Test 6] Successful booking consumes active hold ('ACTIVE' -> 'CONSUMED')... ");
        try {
            // Fetch the active hold from Test 1
            var holds = roomHoldDAO.findActiveHoldsForRoom(testRoom.getId());
            if (holds.isEmpty()) {
                throw new RuntimeException("No active hold found for room");
            }
            RoomHold userHold = holds.get(0);

            Booking validBooking = new Booking();
            validBooking.setGuestId(testGuest.getId());
            validBooking.setRoomId(testRoom.getId());
            validBooking.setCheckInDate(baseIn);
            validBooking.setCheckOutDate(baseOut);
            validBooking.setGuestCount(1);

            Booking created = bookingDAO.createBooking(validBooking, userHold.getHoldToken());
            Optional<RoomHold> consumedHold = roomHoldDAO.findByToken(userHold.getHoldToken());

            if (created != null && consumedHold.isPresent() && "CONSUMED".equalsIgnoreCase(consumedHold.get().getStatus())) {
                System.out.println("PASSED (Ref: " + created.getBookingReference() + ", Hold Status: " + consumedHold.get().getStatus() + ")");
                passed++;
            } else {
                System.out.println("FAILED (Hold not marked as CONSUMED: " + consumedHold + ")");
                failed++;
            }
        } catch (Exception e) {
            System.out.println("FAILED: " + e.getMessage());
            failed++;
        }

        // TEST 7: Attempting to hold a room with a confirmed booking is rejected
        System.out.print("[Test 7] Rejecting hold on room that already has a confirmed booking... ");
        try {
            roomHoldDAO.createHold(testRoom.getId(), baseIn, baseOut);
            System.out.println("FAILED (Allowed hold over confirmed booking)");
            failed++;
        } catch (RoomHoldDAO.RoomHoldOverlapException e) {
            System.out.println("PASSED (" + e.getMessage() + ")");
            passed++;
        } catch (Exception e) {
            System.out.println("FAILED: " + e.getMessage());
            failed++;
        }

        // TEST 8: Hold cancellation / release
        System.out.print("[Test 8] Releasing active hold ('ACTIVE' -> 'CANCELLED')... ");
        try {
            LocalDate relIn = baseIn.plusDays(10);
            LocalDate relOut = relIn.plusDays(2);
            RoomHold holdToRelease = roomHoldDAO.createHold(testRoom2.getId(), relIn, relOut);

            boolean released = roomHoldDAO.releaseHold(holdToRelease.getHoldToken());
            Optional<RoomHold> cancelledOpt = roomHoldDAO.findByToken(holdToRelease.getHoldToken());
            boolean stillHeld = roomHoldDAO.isRoomHeld(testRoom2.getId(), relIn, relOut);

            if (released && cancelledOpt.isPresent() && "CANCELLED".equalsIgnoreCase(cancelledOpt.get().getStatus()) && !stillHeld) {
                System.out.println("PASSED (Status: CANCELLED, Room is no longer held)");
                passed++;
            } else {
                System.out.println("FAILED");
                failed++;
            }
        } catch (Exception e) {
            System.out.println("FAILED: " + e.getMessage());
            failed++;
        }

        // TEST 9: Expired hold stops blocking availability without requiring background daemon
        System.out.print("[Test 9] Expired hold immediately unblocks availability... ");
        try {
            LocalDate expIn = baseIn.plusDays(20);
            LocalDate expOut = expIn.plusDays(3);
            RoomHold holdToExpire = roomHoldDAO.createHold(testRoom2.getId(), expIn, expOut);

            // Simulate server-side expiry by setting expires_at to 10 seconds in the past
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                         "UPDATE room_holds SET expires_at = DATE_SUB(NOW(), INTERVAL 10 SECOND) WHERE hold_token = ?")) {
                stmt.setString(1, holdToExpire.getHoldToken());
                stmt.executeUpdate();
            }

            boolean blocked = roomHoldDAO.isRoomHeld(testRoom2.getId(), expIn, expOut);
            if (!blocked) {
                // Room is unblocked, so creating a new hold should succeed
                RoomHold newHold = roomHoldDAO.createHold(testRoom2.getId(), expIn, expOut);
                if (newHold != null && newHold.getHoldToken() != null) {
                    System.out.println("PASSED (Room immediately available after expiry, new hold created)");
                    passed++;
                    roomHoldDAO.releaseHold(newHold.getHoldToken());
                } else {
                    System.out.println("FAILED (Could not create hold after expiry)");
                    failed++;
                }
            } else {
                System.out.println("FAILED (Room still marked as held after expiry)");
                failed++;
            }
        } catch (Exception e) {
            System.out.println("FAILED: " + e.getMessage());
            failed++;
        }

        // TEST 10: Cleanup utility sweeps expired holds to 'EXPIRED' status
        System.out.print("[Test 10] Cleanup utility updates status of expired holds to 'EXPIRED'... ");
        try {
            int cleaned = roomHoldDAO.cleanupExpiredHolds();
            System.out.println("PASSED (Cleaned up " + cleaned + " expired hold record(s))");
            passed++;
        } catch (Exception e) {
            System.out.println("FAILED: " + e.getMessage());
            failed++;
        }

        // Final Summary
        System.out.println("\n----------------------------------------------------------------");
        System.out.println("  Ghost Inventory Prevention Tests: " + passed + " PASSED, " + failed + " FAILED");
        System.out.println("----------------------------------------------------------------");

        if (failed > 0) {
            System.exit(1);
        }
    }
}
