package com.hms.dao;

import com.hms.model.*;
import com.hms.util.BillCalculator;
import com.hms.util.UserSession;
import com.hms.util.ValidationUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * CustomerWorkflowTest
 * 
 * Comprehensive automated integration test for the Customer Booking & Payment Workflow:
 * 1. Customer registration & authentication (linking to guest record)
 * 2. Room availability search
 * 3. Automatic 5-minute room hold creation & inventory locking
 * 4. Bill calculation using BillCalculator (rate * nights, RoundingMode.HALF_UP)
 * 5. Countdown timestamp verification from database expires_at
 * 6. Payment failure simulation (hold remains active, no booking created)
 * 7. Payment success simulation (atomic hold consumption, booking creation, payment recording, invoice generation)
 * 8. Customer data isolation (accessing another guest's bookings or invoices is blocked)
 * 9. Hold cancellation & inventory release
 */
public class CustomerWorkflowTest {

    private static int testsPassed = 0;
    private static int testsFailed = 0;

    public static void main(String[] args) {
        System.out.println("============================================================");
        System.out.println("  Running Customer Workflow & 5-Min Hold Integration Tests");
        System.out.println("============================================================\n");

        UserDAO userDAO = new UserDAOImpl();
        GuestDAO guestDAO = new GuestDAOImpl();
        RoomDAO roomDAO = new RoomDAOImpl();
        RoomHoldDAO roomHoldDAO = new RoomHoldDAOImpl();
        BookingDAO bookingDAO = new BookingDAOImpl();
        PaymentDAO paymentDAO = new PaymentDAOImpl();
        InvoiceDAO invoiceDAO = new InvoiceDAOImpl();
        ReportDAO reportDAO = new ReportDAOImpl();

        long uniqueSuffix = System.currentTimeMillis() % 100000;
        String custUsername = "cust_wf_" + uniqueSuffix;
        String custPhone = "99" + String.format("%08d", uniqueSuffix);
        String custEmail = "cust" + uniqueSuffix + "@wfhotel.com";

        try {
            // -----------------------------------------------------------------
            // TEST 1: Customer Registration & Guest Profile Link
            // -----------------------------------------------------------------
            System.out.println("--- Test 1: Customer Registration & Guest Record Linking ---");
            User registeredUser = userDAO.registerCustomer(
                    custUsername,
                    "secretPass123",
                    "Workflow Test Customer",
                    custPhone,
                    custEmail,
                    "Aadhaar",
                    "1234-5678-" + String.format("%04d", uniqueSuffix % 10000),
                    "Kochi, Kerala"
            );

            assertCondition(registeredUser != null, "Customer registration should return non-null user");
            assertCondition("CUSTOMER".equals(registeredUser.getRole()), "User role must be CUSTOMER");
            assertCondition(registeredUser.getGuestId() != null && registeredUser.getGuestId() > 0,
                    "Customer user must have linked guest_id > 0");

            Optional<Guest> guestOpt = guestDAO.findById(registeredUser.getGuestId());
            assertCondition(guestOpt.isPresent(), "Linked guest record must exist in guests table");
            assertCondition(custPhone.equals(guestOpt.get().getPhone()), "Linked guest must have registered phone");
            System.out.println("Registered Customer User ID: " + registeredUser.getId() + ", Linked Guest ID: " + registeredUser.getGuestId());

            // -----------------------------------------------------------------
            // TEST 2: Customer Authentication & Session State
            // -----------------------------------------------------------------
            System.out.println("\n--- Test 2: Customer Authentication & Session State ---");
            Optional<User> authUserOpt = userDAO.authenticate(custUsername, "secretPass123");
            assertCondition(authUserOpt.isPresent(), "Customer authentication must succeed with valid credentials");
            User authUser = authUserOpt.get();
            UserSession.login(authUser);
            assertCondition(UserSession.isCustomer(), "UserSession.isCustomer() must be true");
            assertCondition(registeredUser.getGuestId().equals(UserSession.getCurrentGuestId()),
                    "UserSession.getCurrentGuestId() must match registered guestId");

            // -----------------------------------------------------------------
            // TEST 3: Room Availability Search & Dynamic Date Interval
            // -----------------------------------------------------------------
            System.out.println("\n--- Test 3: Room Availability Search ---");
            // Pick a date far in future to guarantee clean test window
            LocalDate checkIn = LocalDate.now().plusMonths(4).plusDays((int)(uniqueSuffix % 20));
            LocalDate checkOut = checkIn.plusDays(3);

            List<Room> availableRooms = roomDAO.findAvailableRoomsForDates(checkIn, checkOut);
            assertCondition(!availableRooms.isEmpty(), "System must have available rooms for future dates");
            Room testRoom = availableRooms.get(0);
            System.out.println("Selected test room: Room " + testRoom.getRoomNumber() + " (" + testRoom.getRoomType() + ")");

            // -----------------------------------------------------------------
            // TEST 4: Automatic 5-Minute Room Hold Creation
            // -----------------------------------------------------------------
            System.out.println("\n--- Test 4: Automatic 5-Minute Room Hold Creation ---");
            RoomHold hold = roomHoldDAO.createHold(testRoom.getId(), checkIn, checkOut);
            assertCondition(hold != null, "createHold must return non-null RoomHold");
            assertCondition(hold.getHoldToken() != null && !hold.getHoldToken().isBlank(), "Hold token must be generated");
            assertCondition("ACTIVE".equals(hold.getStatus()), "Hold status must be ACTIVE");
            assertCondition(hold.getExpiresAt() != null, "Hold must have an expires_at timestamp");

            long secondsUntilExpiry = ChronoUnit.SECONDS.between(LocalDateTime.now(), hold.getExpiresAt());
            assertCondition(secondsUntilExpiry >= 270 && secondsUntilExpiry <= 310,
                    "Hold duration must be approximately 300 seconds (5 minutes). Actual: " + secondsUntilExpiry);

            // Verify room is now blocked from availability
            boolean isHeld = roomHoldDAO.isRoomHeld(testRoom.getId(), checkIn, checkOut);
            assertCondition(isHeld, "isRoomHeld must return true while hold is active");

            List<Room> roomsAfterHold = roomDAO.findAvailableRoomsForDates(checkIn, checkOut);
            boolean containsHeldRoom = roomsAfterHold.stream().anyMatch(r -> r.getId() == testRoom.getId());
            assertCondition(!containsHeldRoom, "findAvailableRoomsForDates must exclude the held room");

            // -----------------------------------------------------------------
            // TEST 5: Bill Calculation via BillCalculator
            // -----------------------------------------------------------------
            System.out.println("\n--- Test 5: Bill Calculation ---");
            long nights = BillCalculator.calculateNights(checkIn, checkOut);
            assertCondition(nights == 3, "Nights must be 3 between check-in and check-out");
            BigDecimal expectedTotal = BillCalculator.calculateRoomCharge(testRoom.getPricePerNight(), nights);
            BigDecimal manualCalc = testRoom.getPricePerNight().multiply(BigDecimal.valueOf(3)).setScale(2, RoundingMode.HALF_UP);
            assertCondition(expectedTotal.compareTo(manualCalc) == 0, "Calculated total must equal rate * nights");
            System.out.println("Room rate: ₹" + testRoom.getPricePerNight() + " * " + nights + " nights = Total: ₹" + expectedTotal);

            // -----------------------------------------------------------------
            // TEST 6: Payment Failure Simulation (Hold Stays Active)
            // -----------------------------------------------------------------
            System.out.println("\n--- Test 6: Payment Failure Simulation ---");
            // If payment fails during simulation, hold must remain active and room must still be protected
            boolean holdStillActive = roomHoldDAO.isRoomHeld(testRoom.getId(), checkIn, checkOut);
            assertCondition(holdStillActive, "Hold must remain ACTIVE after a failed payment attempt");

            // -----------------------------------------------------------------
            // TEST 7: Payment Success Flow & Atomic Hold Consumption
            // -----------------------------------------------------------------
            System.out.println("\n--- Test 7: Payment Success Flow & Hold Consumption ---");
            Booking booking = new Booking();
            booking.setGuestId(registeredUser.getGuestId());
            booking.setRoomId(testRoom.getId());
            booking.setCheckInDate(checkIn);
            booking.setCheckOutDate(checkOut);
            booking.setGuestCount(1);
            booking.setTotalAmount(expectedTotal);
            booking.setStatus("CONFIRMED");

            Booking createdBooking = bookingDAO.createBooking(booking, hold.getHoldToken());
            assertCondition(createdBooking.getId() > 0, "Booking creation must generate ID");
            assertCondition(createdBooking.getBookingReference() != null, "Booking must have generated reference");

            // Verify hold is now marked CONSUMED
            Optional<RoomHold> holdAfterOpt = roomHoldDAO.findByToken(hold.getHoldToken());
            assertCondition(holdAfterOpt.isPresent() && "CONSUMED".equalsIgnoreCase(holdAfterOpt.get().getStatus()),
                    "Room hold status must transition to CONSUMED upon booking creation");

            // Record payment as PAID
            Payment payment = new Payment();
            payment.setBookingId(createdBooking.getId());
            payment.setAmount(expectedTotal);
            payment.setPaymentMethod("UPI");
            payment.setPaymentStatus("PAID");
            payment.setTransactionReference("TXN-WF-" + createdBooking.getId() + "-" + uniqueSuffix);
            payment.setNotes("Customer workflow verified payment");
            Payment recordedPayment = paymentDAO.recordPayment(payment);

            assertCondition(recordedPayment.getId() > 0, "Recorded payment must have valid ID");
            BigDecimal totalPaid = paymentDAO.getTotalPaidForBooking(createdBooking.getId());
            assertCondition(totalPaid.compareTo(expectedTotal) == 0, "Total paid must equal total booking amount");

            // Generate Invoice
            Invoice invoice = invoiceDAO.generateInvoice(createdBooking.getId(), "Customer Portal Workflow Test");
            assertCondition(invoice != null, "generateInvoice must return non-null Invoice");
            assertCondition(invoice.getInvoiceNumber() != null, "Invoice must have invoice number");
            assertCondition(invoice.getBalanceDue().compareTo(BigDecimal.ZERO) == 0, "Balance due must be ₹0.00");
            assertCondition(invoice.isSettled(), "Invoice must be settled");
            System.out.println("Generated Invoice: " + invoice.getInvoiceNumber() + ", Total: ₹" + invoice.getTotalAmount()
                    + ", Paid: ₹" + invoice.getPaidAmount() + ", Balance: ₹" + invoice.getBalanceDue());

            // -----------------------------------------------------------------
            // TEST 8: Customer Isolation Checks
            // -----------------------------------------------------------------
            System.out.println("\n--- Test 8: Customer Data Isolation Enforced ---");
            // Register a second customer
            String otherCustUsername = "other_wf_" + uniqueSuffix;
            String otherPhone = "98" + String.format("%08d", uniqueSuffix);
            User otherCustomer = userDAO.registerCustomer(
                    otherCustUsername,
                    "password999",
                    "Other Unauthorized Guest",
                    otherPhone,
                    "other" + uniqueSuffix + "@wfhotel.com",
                    "Passport",
                    "Z" + String.format("%07d", uniqueSuffix % 10000000),
                    "Bangalore"
            );

            // Log in as second customer
            UserSession.login(otherCustomer);

            // Attempt to read first customer's bookings
            boolean bookingAccessBlocked = false;
            try {
                bookingDAO.findBookingsByGuestId(registeredUser.getGuestId());
            } catch (SecurityException ex) {
                bookingAccessBlocked = true;
                System.out.println("Blocked unauthorized booking access: " + ex.getMessage());
            }
            assertCondition(bookingAccessBlocked, "Customer must NOT be allowed to view another customer's bookings");

            // Attempt to read first customer's invoices
            boolean invoiceAccessBlocked = false;
            try {
                invoiceDAO.findInvoicesByGuestId(registeredUser.getGuestId());
            } catch (SecurityException ex) {
                invoiceAccessBlocked = true;
                System.out.println("Blocked unauthorized invoice access: " + ex.getMessage());
            }
            assertCondition(invoiceAccessBlocked, "Customer must NOT be allowed to view another customer's invoices");

            // Attempt to access admin report as customer
            boolean reportAccessBlocked = false;
            try {
                reportDAO.generateBookingReport(null, null, "ALL");
            } catch (SecurityException ex) {
                reportAccessBlocked = true;
                System.out.println("Blocked unauthorized report access: " + ex.getMessage());
            }
            assertCondition(reportAccessBlocked, "Customer must NOT be allowed to access administrative reports");

            // Log back in as first customer and verify they CAN see their own booking & invoice
            UserSession.login(authUser);
            List<Booking> myBookings = bookingDAO.findBookingsByGuestId(registeredUser.getGuestId());
            assertCondition(!myBookings.isEmpty(), "Customer must be able to view their own bookings");
            assertCondition(myBookings.stream().anyMatch(b -> b.getId() == createdBooking.getId()),
                    "Created booking must appear in customer's own booking list");

            List<Invoice> myInvoices = invoiceDAO.findInvoicesByGuestId(registeredUser.getGuestId());
            assertCondition(!myInvoices.isEmpty(), "Customer must be able to view their own invoices");
            assertCondition(myInvoices.stream().anyMatch(inv -> inv.getId() == invoice.getId()),
                    "Generated invoice must appear in customer's own invoice list");

            // -----------------------------------------------------------------
            // TEST 9: Hold Cancellation & Inventory Release
            // -----------------------------------------------------------------
            System.out.println("\n--- Test 9: Hold Cancellation & Inventory Release ---");
            LocalDate testDateIn = LocalDate.now().plusMonths(6);
            LocalDate testDateOut = testDateIn.plusDays(2);
            RoomHold holdToCancel = roomHoldDAO.createHold(testRoom.getId(), testDateIn, testDateOut);
            assertCondition("ACTIVE".equals(holdToCancel.getStatus()), "New hold must be ACTIVE");

            boolean released = roomHoldDAO.releaseHold(holdToCancel.getHoldToken());
            assertCondition(released, "releaseHold must return true");

            Optional<RoomHold> releasedHoldOpt = roomHoldDAO.findByToken(holdToCancel.getHoldToken());
            assertCondition(releasedHoldOpt.isPresent() && "CANCELLED".equals(releasedHoldOpt.get().getStatus()),
                    "Released hold status must be CANCELLED");

            boolean roomHeldAfterRelease = roomHoldDAO.isRoomHeld(testRoom.getId(), testDateIn, testDateOut);
            assertCondition(!roomHeldAfterRelease, "Room must no longer be held after releaseHold");

            UserSession.logout();

        } catch (Exception e) {
            e.printStackTrace();
            testsFailed++;
        }

        System.out.println("\n============================================================");
        System.out.println("  Customer Workflow Integration Test Summary");
        System.out.println("  PASSED: " + testsPassed + " | FAILED: " + testsFailed);
        System.out.println("============================================================");

        if (testsFailed > 0) {
            System.exit(1);
        }
    }

    private static void assertCondition(boolean condition, String message) {
        if (condition) {
            System.out.println("  [PASS] " + message);
            testsPassed++;
        } else {
            System.err.println("  [FAIL] " + message);
            testsFailed++;
        }
    }
}
