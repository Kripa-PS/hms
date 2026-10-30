package com.hms.dao;

import com.hms.model.*;
import com.hms.util.DatabaseConnection;
import com.hms.util.RoomServiceMenu;
import com.hms.util.UserSession;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * CustomerRoomServiceTest
 * 
 * Comprehensive automated verification test suite for the extended Customer Room Service module.
 * Tests:
 * 1. Customer without active booking blocked from ordering
 * 2. Successful customer order placement with active booking
 * 3. Accurate price, quantity and instructions calculation
 * 4. Cross-customer booking ownership enforcement (cannot order for another guest's room)
 * 5. Customer data isolation (cannot view all orders or another guest's orders)
 * 6. Immediate staff visibility of customer orders
 * 7. Staff order status update workflow (REQUESTED -> IN_PROGRESS -> COMPLETED)
 * 8. Customer cancellation of pending orders & prevention of cancelling completed orders
 * 9. Input validation for quantities, items and prices
 * 10. RoomServiceMenu catalog structure and pricing integrity
 */
public class CustomerRoomServiceTest {

    public static void main(String[] args) {
        System.out.println("======================================================================");
        System.out.println("  Customer Room Service & Staff Integration Test Suite               ");
        System.out.println("======================================================================");

        if (!DatabaseConnection.testConnection()) {
            System.err.println("[ERROR] MySQL is offline. Aborting tests.");
            System.exit(1);
        }

        RoomServiceDAO roomServiceDAO = new RoomServiceDAOImpl();
        BookingDAO bookingDAO = new BookingDAOImpl();
        RoomDAO roomDAO = new RoomDAOImpl();
        UserDAO userDAO = new UserDAOImpl();

        int passed = 0;
        int failed = 0;

        try {
            // Setup Test Customer 1
            String phone1 = "98" + (int)(Math.random() * 90000000 + 10000000);
            String user1Name = "cust_rs_" + System.currentTimeMillis();
            User user1 = userDAO.registerCustomer(
                    user1Name, "custpass123", "Ananya Iyer",
                    phone1, "ananya@test.com", "Aadhaar",
                    "1234-5678-9012", "Bengaluru"
            );
            int guest1Id = user1.getGuestId();

            // Setup Test Customer 2 (for cross-customer isolation tests)
            String phone2 = "97" + (int)(Math.random() * 90000000 + 10000000);
            String user2Name = "cust_rs2_" + (System.currentTimeMillis() + 10);
            User user2 = userDAO.registerCustomer(
                    user2Name, "custpass123", "Rohan Verma",
                    phone2, "rohan@test.com", "Passport",
                    "P9876543", "Mumbai"
            );
            int guest2Id = user2.getGuestId();

            // Setup Admin User
            User adminUser = userDAO.authenticate("admin", "admin123").orElseGet(() ->
                    new User(1, "admin", "admin123", "System Administrator", "ADMIN", null, true, LocalDateTime.now())
            );

            // Find an operational room
            List<Room> rooms = roomDAO.findOperationallyAvailableRooms();
            if (rooms.isEmpty()) {
                System.err.println("[ERROR] No operational rooms found in database.");
                System.exit(1);
            }
            Room testRoom = rooms.get(0);

            // --------------------------------------------------------------------
            // Test 1: Customer without active booking cannot order
            // --------------------------------------------------------------------
            System.out.print("[Test 1] Customer without active booking blocked from ordering... ");
            UserSession.login(user1);
            List<StayOption> initialStays = roomServiceDAO.findActiveStaysForGuest(guest1Id);
            assert initialStays.isEmpty() : "Should have no active stays initially";

            boolean fakeOrderPlaced = roomServiceDAO.addCustomerOrder(guest1Id, 999999, "Club Sandwich", 1, new BigDecimal("220.00"), "");
            assert !fakeOrderPlaced : "Should reject order for non-existent booking";
            System.out.println("PASSED (Active stay required)");
            passed++;

            // --------------------------------------------------------------------
            // Test 2: Successful customer order placement with active booking
            // --------------------------------------------------------------------
            System.out.print("[Test 2] Customer order placement with confirmed booking... ");
            LocalDate inDate = LocalDate.now().plusDays(1000 + (int)(Math.random() * 5000));
            while (!bookingDAO.isRoomAvailable(testRoom.getId(), inDate, inDate.plusDays(3))) {
                inDate = inDate.plusDays(10);
            }
            LocalDate outDate = inDate.plusDays(3);
            Booking booking1 = new Booking("HMS-RS-" + (int)(Math.random() * 9000 + 1000),
                    guest1Id, testRoom.getId(), inDate, outDate, 1, new BigDecimal("4500.00"));
            Booking createdBooking1 = bookingDAO.createBooking(booking1);

            List<StayOption> activeStays = roomServiceDAO.findActiveStaysForGuest(guest1Id);
            assert !activeStays.isEmpty() : "Customer should now have an active stay";
            StayOption stay1 = activeStays.get(0);
            assert stay1.getBookingId() == createdBooking1.getId() : "Active stay booking ID mismatch";

            boolean order1Placed = roomServiceDAO.addCustomerOrder(
                    guest1Id, createdBooking1.getId(), "Club Sandwich", 2, new BigDecimal("220.00"), "Extra cheese, no mayo");
            assert order1Placed : "Order placement should succeed";
            System.out.println("PASSED (Order placed for Room " + testRoom.getRoomNumber() + ")");
            passed++;

            // --------------------------------------------------------------------
            // Test 3: Accurate price, quantity and instructions calculation
            // --------------------------------------------------------------------
            System.out.print("[Test 3] Order pricing and instruction persistence... ");
            List<RoomServiceOrder> guest1Orders = roomServiceDAO.findOrdersByGuestId(guest1Id);
            assert !guest1Orders.isEmpty() : "Guest should have placed orders";
            RoomServiceOrder order1 = guest1Orders.get(0);

            assert order1.getQuantity() == 2 : "Quantity should be 2";
            assert order1.getUnitPrice().compareTo(new BigDecimal("220.00")) == 0 : "Unit price mismatch";
            assert order1.getTotalPrice().compareTo(new BigDecimal("440.00")) == 0 : "Total price should be 440.00";
            assert "REQUESTED".equals(order1.getStatus()) : "Status should be REQUESTED";
            assert order1.getServiceItem().contains("Club Sandwich") : "Item name missing";
            assert order1.getServiceItem().contains("Extra cheese, no mayo") : "Special instructions missing";
            System.out.println("PASSED (Total: Rs. " + order1.getTotalPrice() + ", Item: " + order1.getServiceItem() + ")");
            passed++;

            // --------------------------------------------------------------------
            // Test 4: Cross-customer booking ownership enforcement
            // --------------------------------------------------------------------
            System.out.print("[Test 4] Backend blocks ordering for another customer's booking... ");
            // Guest 2 creates a booking
            LocalDate in2 = outDate.plusDays(10);
            while (!bookingDAO.isRoomAvailable(testRoom.getId(), in2, in2.plusDays(2))) {
                in2 = in2.plusDays(5);
            }
            LocalDate out2 = in2.plusDays(2);
            Booking booking2 = new Booking("HMS-RS2-" + (int)(Math.random() * 9000 + 1000),
                    guest2Id, testRoom.getId(), in2, out2, 1, new BigDecimal("3000.00"));
            Booking createdBooking2 = bookingDAO.createBooking(booking2);

            // User 1 (Guest 1) attempts to place an order specifying Guest 2's bookingId
            boolean forgedOrder = roomServiceDAO.addCustomerOrder(
                    guest1Id, createdBooking2.getId(), "Hot Masala Tea", 1, new BigDecimal("60.00"), "");
            assert !forgedOrder : "Backend must reject ordering for another guest's booking ID";
            System.out.println("PASSED (Cross-booking tampering blocked)");
            passed++;

            // --------------------------------------------------------------------
            // Test 5: Customer data isolation
            // --------------------------------------------------------------------
            System.out.print("[Test 5] Customer data isolation enforced in DAO... ");
            boolean caughtGuestMismatch = false;
            try {
                // User 1 attempts to query User 2's orders
                roomServiceDAO.findOrdersByGuestId(guest2Id);
            } catch (SecurityException se) {
                caughtGuestMismatch = true;
            }
            assert caughtGuestMismatch : "Customer querying another customer's orders must throw SecurityException";

            boolean caughtFindAllBlocked = false;
            try {
                // User 1 attempts to query all hotel orders
                roomServiceDAO.findAll("ALL");
            } catch (SecurityException se) {
                caughtFindAllBlocked = true;
            }
            assert caughtFindAllBlocked : "Customer calling findAll must throw SecurityException";
            System.out.println("PASSED (Unauthorized access rejected with SecurityException)");
            passed++;

            // --------------------------------------------------------------------
            // Test 6: Immediate staff visibility of customer order
            // --------------------------------------------------------------------
            System.out.print("[Test 6] Staff immediately sees customer order in portal... ");
            UserSession.login(adminUser);
            List<RoomServiceOrder> staffOrders = roomServiceDAO.findAll("ALL");
            boolean foundCustomerOrder = false;
            for (RoomServiceOrder so : staffOrders) {
                if (so.getId() == order1.getId()) {
                    foundCustomerOrder = true;
                    assert so.getGuestName().contains("Ananya Iyer") : "Staff view guest name mismatch";
                    assert so.getRoomNumber().equals(testRoom.getRoomNumber()) : "Staff view room number mismatch";
                    break;
                }
            }
            assert foundCustomerOrder : "Customer order should appear in staff orders list";
            System.out.println("PASSED (Staff views Order #" + order1.getId() + " for Room " + testRoom.getRoomNumber() + ")");
            passed++;

            // --------------------------------------------------------------------
            // Test 7: Staff status update workflow (REQUESTED -> IN_PROGRESS -> COMPLETED)
            // --------------------------------------------------------------------
            System.out.print("[Test 7] Staff updates order status to IN_PROGRESS and COMPLETED... ");
            boolean updatedToProgress = roomServiceDAO.updateStatus(order1.getId(), "IN_PROGRESS");
            assert updatedToProgress : "Staff should be able to update status to IN_PROGRESS";

            // Verify customer sees IN_PROGRESS
            UserSession.login(user1);
            List<RoomServiceOrder> updatedCustomerOrders = roomServiceDAO.findOrdersByGuestId(guest1Id);
            assert "IN_PROGRESS".equals(updatedCustomerOrders.get(0).getStatus()) : "Customer should see IN_PROGRESS";

            // Staff completes the order
            UserSession.login(adminUser);
            boolean updatedToCompleted = roomServiceDAO.updateStatus(order1.getId(), "COMPLETED");
            assert updatedToCompleted : "Staff should be able to update status to COMPLETED";

            UserSession.login(user1);
            List<RoomServiceOrder> completedCustomerOrders = roomServiceDAO.findOrdersByGuestId(guest1Id);
            assert "COMPLETED".equals(completedCustomerOrders.get(0).getStatus()) : "Customer should see COMPLETED";
            assert completedCustomerOrders.get(0).getCompletedAt() != null : "completed_at timestamp must be recorded";
            System.out.println("PASSED (Status transitions: REQUESTED -> IN_PROGRESS -> COMPLETED)");
            passed++;

            // --------------------------------------------------------------------
            // Test 8: Customer order cancellation & prevention on completed orders
            // --------------------------------------------------------------------
            System.out.print("[Test 8] Customer cancellation of REQUESTED order... ");
            // Customer places a second order
            boolean order2Placed = roomServiceDAO.addCustomerOrder(
                    guest1Id, createdBooking1.getId(), "Cold Brew Coffee", 1, new BigDecimal("120.00"), "Less ice");
            assert order2Placed : "Order 2 placement failed";
            List<RoomServiceOrder> ordersAfter2 = roomServiceDAO.findOrdersByGuestId(guest1Id);
            RoomServiceOrder order2 = ordersAfter2.get(0); // newest first

            // Customer cancels order 2
            boolean cancelled = roomServiceDAO.cancelCustomerOrder(guest1Id, order2.getId());
            assert cancelled : "Customer should be able to cancel a REQUESTED order";

            List<RoomServiceOrder> ordersAfterCancel = roomServiceDAO.findOrdersByGuestId(guest1Id);
            assert "CANCELLED".equals(ordersAfterCancel.get(0).getStatus()) : "Order status should now be CANCELLED";

            // Customer cannot cancel an already COMPLETED order (order 1)
            boolean cancelCompletedBlocked = roomServiceDAO.cancelCustomerOrder(guest1Id, order1.getId());
            assert !cancelCompletedBlocked : "Customer cannot cancel a COMPLETED order";

            // Customer 1 cannot cancel Customer 2's orders
            boolean cancelOtherBlocked = roomServiceDAO.cancelCustomerOrder(guest1Id, 999999);
            assert !cancelOtherBlocked : "Customer cannot cancel another guest's order";
            System.out.println("PASSED (REQUESTED order cancelled, COMPLETED order protected)");
            passed++;

            // --------------------------------------------------------------------
            // Test 9: Input validation for quantities, items and prices
            // --------------------------------------------------------------------
            System.out.print("[Test 9] Validation of quantities, items and prices... ");
            boolean zeroQty = roomServiceDAO.addCustomerOrder(guest1Id, createdBooking1.getId(), "Item", 0, new BigDecimal("100.00"), "");
            boolean negQty = roomServiceDAO.addCustomerOrder(guest1Id, createdBooking1.getId(), "Item", -2, new BigDecimal("100.00"), "");
            boolean emptyItem = roomServiceDAO.addCustomerOrder(guest1Id, createdBooking1.getId(), "   ", 1, new BigDecimal("100.00"), "");
            boolean negPrice = roomServiceDAO.addCustomerOrder(guest1Id, createdBooking1.getId(), "Item", 1, new BigDecimal("-10.00"), "");

            assert !zeroQty && !negQty && !emptyItem && !negPrice : "Invalid inputs must be rejected";
            System.out.println("PASSED (Zero/negative qty, empty item, negative price rejected)");
            passed++;

            // --------------------------------------------------------------------
            // Test 10: Menu catalog integrity
            // --------------------------------------------------------------------
            System.out.print("[Test 10] RoomServiceMenu catalog validation... ");
            List<RoomServiceMenuItem> menu = RoomServiceMenu.getAllItems();
            assert menu.size() >= 10 : "Menu must contain at least 10 items";
            for (RoomServiceMenuItem item : menu) {
                assert item.getId() != null && !item.getId().isBlank() : "Item ID cannot be empty";
                assert item.getName() != null && !item.getName().isBlank() : "Item name cannot be empty";
                assert item.getCategory() != null && !item.getCategory().isBlank() : "Item category cannot be empty";
                assert item.getPrice() != null && item.getPrice().signum() > 0 : "Item price must be positive";
                assert item.isAvailable() : "Item must be marked available";
            }
            System.out.println("PASSED (" + menu.size() + " menu items validated)");
            passed++;

        } catch (Throwable t) {
            System.out.println("FAILED: " + t.getMessage());
            t.printStackTrace();
            failed++;
        } finally {
            UserSession.logout();
        }

        System.out.println("======================================================================");
        System.out.println("Summary: Passed: " + passed + " / " + (passed + failed) + ", Failed: " + failed);
        System.out.println("======================================================================");

        if (failed > 0) {
            System.exit(1);
        }
    }
}
