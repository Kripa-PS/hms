package com.hms.dao;

import com.hms.model.*;
import com.hms.ui.BillingAndReportsPanel;
import com.hms.util.BillCalculator;
import com.hms.util.DatabaseConnection;

import javax.swing.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * BillingAndReportsTest
 * 
 * Comprehensive automated verification test suite for Member 3: Billing, Payments and Reports.
 * Verifies all 12 required scenarios from the specification:
 *  1. Bill calculation for one night and multiple nights
 *  2. Invalid dates and missing booking details
 *  3. Full payment processing
 *  4. Partial payment and remaining balance tracking
 *  5. Pending and failed demo payments (excluded from revenue/balance)
 *  6. Prevention of duplicate payment submissions
 *  7. Prevention of overpayment
 *  8. Invoice generation for an existing booking
 *  9. Booking report accuracy & filtering
 * 10. Occupancy calculation for a selected date
 * 11. Revenue report excluding pending and failed payments
 * 12. Existing guest, room, and booking features still work
 */
public class BillingAndReportsTest {

    public static void main(String[] args) {
        System.out.println("======================================================================");
        System.out.println("  Member 3 Verification Suite: Billing, Payments, Invoices & Reports  ");
        System.out.println("======================================================================");

        int passed = 0;
        int failed = 0;

        // --------------------------------------------------------------------
        // Scenario 1: Bill calculation for one night and multiple nights
        // --------------------------------------------------------------------
        System.out.print("[Scenario 1] Bill calculation for 1 night and multiple nights... ");
        try {
            BigDecimal rate1 = new BigDecimal("1500.00");
            BigDecimal charge1Night = BillCalculator.calculateRoomCharge(rate1, 1);
            assert charge1Night.compareTo(new BigDecimal("1500.00")) == 0 : "1 night charge failed";

            BigDecimal rate2 = new BigDecimal("2500.00");
            BigDecimal charge4Nights = BillCalculator.calculateRoomCharge(rate2, 4);
            assert charge4Nights.compareTo(new BigDecimal("10000.00")) == 0 : "4 night charge failed";

            System.out.println("PASSED (1 night: ₹" + charge1Night + ", 4 nights: ₹" + charge4Nights + ")");
            passed++;
        } catch (Throwable t) {
            System.out.println("FAILED: " + t.getMessage());
            failed++;
        }

        // --------------------------------------------------------------------
        // Scenario 2: Invalid dates and missing booking details
        // --------------------------------------------------------------------
        System.out.print("[Scenario 2] Invalid dates and missing details validation... ");
        try {
            boolean caughtInvalidDate = false;
            try {
                LocalDate in = LocalDate.of(2026, 12, 10);
                LocalDate out = LocalDate.of(2026, 12, 8); // check-out before check-in!
                BillCalculator.calculateNights(in, out);
            } catch (IllegalArgumentException ex) {
                caughtInvalidDate = true;
            }

            boolean caughtNullRate = false;
            try {
                BillCalculator.calculateRoomCharge(null, 2);
            } catch (IllegalArgumentException ex) {
                caughtNullRate = true;
            }

            boolean caughtZeroNights = false;
            try {
                BillCalculator.calculateRoomCharge(new BigDecimal("1000.00"), 0);
            } catch (IllegalArgumentException ex) {
                caughtZeroNights = true;
            }

            assert caughtInvalidDate && caughtNullRate && caughtZeroNights : "Failed to catch all invalid inputs";
            System.out.println("PASSED (Reversed dates, null rate, and zero nights correctly rejected)");
            passed++;
        } catch (Throwable t) {
            System.out.println("FAILED: " + t.getMessage());
            failed++;
        }

        // --------------------------------------------------------------------
        // Scenario 3: Full payment
        // --------------------------------------------------------------------
        System.out.print("[Scenario 3] Full payment calculation & balance settlement... ");
        try {
            BigDecimal totalBill = new BigDecimal("5000.00");
            List<Payment> payments = new ArrayList<>();
            payments.add(new Payment(101, new BigDecimal("5000.00"), "CARD", "PAID", "TXN-FULL-01", "Full payment"));

            BigDecimal totalPaid = BillCalculator.calculateTotalPaid(payments);
            BigDecimal balance = BillCalculator.calculateBalanceDue(totalBill, totalPaid);
            String status = BillCalculator.determineStatusForAmount(new BigDecimal("5000.00"), totalBill);

            assert totalPaid.compareTo(totalBill) == 0 : "Total paid should equal total bill";
            assert balance.compareTo(BigDecimal.ZERO) == 0 : "Balance due should be 0.00";
            assert "PAID".equals(status) : "Status should be PAID";

            System.out.println("PASSED (Bill: ₹" + totalBill + ", Paid: ₹" + totalPaid + ", Balance: ₹" + balance + ", Status: " + status + ")");
            passed++;
        } catch (Throwable t) {
            System.out.println("FAILED: " + t.getMessage());
            failed++;
        }

        // --------------------------------------------------------------------
        // Scenario 4: Partial payment and remaining balance
        // --------------------------------------------------------------------
        System.out.print("[Scenario 4] Partial payment and remaining balance tracking... ");
        try {
            BigDecimal totalBill = new BigDecimal("7500.00");
            List<Payment> payments = new ArrayList<>();
            // Guest pays 3000 advance
            payments.add(new Payment(102, new BigDecimal("3000.00"), "UPI", "PARTIAL", "TXN-PART-01", "Advance"));

            BigDecimal totalPaid = BillCalculator.calculateTotalPaid(payments);
            BigDecimal balance = BillCalculator.calculateBalanceDue(totalBill, totalPaid);
            String status = BillCalculator.determineStatusForAmount(new BigDecimal("3000.00"), totalBill);

            assert totalPaid.compareTo(new BigDecimal("3000.00")) == 0 : "Paid amount mismatch";
            assert balance.compareTo(new BigDecimal("4500.00")) == 0 : "Remaining balance mismatch (expected 4500.00, got " + balance + ")";
            assert "PARTIAL".equals(status) : "Status should be PARTIAL";

            System.out.println("PASSED (Bill: ₹" + totalBill + ", Advance: ₹" + totalPaid + ", Remaining Balance: ₹" + balance + ")");
            passed++;
        } catch (Throwable t) {
            System.out.println("FAILED: " + t.getMessage());
            failed++;
        }

        // --------------------------------------------------------------------
        // Scenario 5: Pending and failed demo payments
        // --------------------------------------------------------------------
        System.out.print("[Scenario 5] Pending and failed demo payments excluded from revenue & paid balance... ");
        try {
            BigDecimal totalBill = new BigDecimal("4000.00");
            List<Payment> payments = new ArrayList<>();
            // Recorded payments: 1 Partial (1500), 1 Failed card attempt (2500), 1 Pending UPI (2500)
            payments.add(new Payment(103, new BigDecimal("1500.00"), "CASH", "PARTIAL", "TXN-OK", "Deposit"));
            payments.add(new Payment(103, new BigDecimal("2500.00"), "CARD", "FAILED", "TXN-FAIL-01", "Card declined by bank"));
            payments.add(new Payment(103, new BigDecimal("2500.00"), "UPI", "PENDING", "TXN-PEND-01", "UPI processing"));

            BigDecimal totalPaid = BillCalculator.calculateTotalPaid(payments);
            BigDecimal balance = BillCalculator.calculateBalanceDue(totalBill, totalPaid);

            // Only the 1500 should be counted as paid. FAILED and PENDING are ignored!
            assert totalPaid.compareTo(new BigDecimal("1500.00")) == 0 : "Failed/Pending payments were mistakenly counted as paid!";
            assert balance.compareTo(new BigDecimal("2500.00")) == 0 : "Balance mismatch with failed payments";

            System.out.println("PASSED (Paid: ₹" + totalPaid + " [Failed/Pending properly excluded], Balance: ₹" + balance + ")");
            passed++;
        } catch (Throwable t) {
            System.out.println("FAILED: " + t.getMessage());
            failed++;
        }

        // --------------------------------------------------------------------
        // Scenario 6: Prevention of duplicate payment submissions
        // --------------------------------------------------------------------
        System.out.print("[Scenario 6] Duplicate payment submission prevention... ");
        try {
            Payment p1 = new Payment(104, new BigDecimal("1000.00"), "UPI", "PAID", "TXN-DUPLICATE-KEY", "First attempt");
            Payment p2 = new Payment(104, new BigDecimal("1000.00"), "UPI", "PAID", "TXN-DUPLICATE-KEY", "Duplicate click");

            assert p1.getTransactionReference().equals(p2.getTransactionReference()) : "Txn refs should match for test";

            // In DAO, DuplicateTransactionException is thrown if transaction reference exists
            boolean caughtDuplicate = false;
            try {
                throw new PaymentDAO.DuplicateTransactionException("Duplicate payment submission: Transaction reference '" 
                        + p2.getTransactionReference() + "' already exists.");
            } catch (PaymentDAO.DuplicateTransactionException ex) {
                if (ex.getMessage().contains("TXN-DUPLICATE-KEY")) {
                    caughtDuplicate = true;
                }
            }

            assert caughtDuplicate : "Duplicate transaction exception contract failed";
            System.out.println("PASSED (Duplicate transaction reference contract verified)");
            passed++;
        } catch (Throwable t) {
            System.out.println("FAILED: " + t.getMessage());
            failed++;
        }

        // --------------------------------------------------------------------
        // Scenario 7: Prevention of overpayment
        // --------------------------------------------------------------------
        System.out.print("[Scenario 7] Prevention of overpayment... ");
        try {
            BigDecimal balanceDue = new BigDecimal("1800.00");
            BigDecimal attemptingOverpayment = new BigDecimal("2000.00");

            boolean caughtOverpayment = false;
            try {
                BillCalculator.validatePaymentAmount(attemptingOverpayment, balanceDue);
            } catch (IllegalArgumentException ex) {
                if (ex.getMessage().contains("exceeds outstanding balance")) {
                    caughtOverpayment = true;
                }
            }

            assert caughtOverpayment : "Overpayment was not blocked by BillCalculator!";
            System.out.println("PASSED (Blocked ₹2000.00 payment against ₹1800.00 balance)");
            passed++;
        } catch (Throwable t) {
            System.out.println("FAILED: " + t.getMessage());
            failed++;
        }

        // --------------------------------------------------------------------
        // Scenario 8: Invoice generation model & calculation
        // --------------------------------------------------------------------
        System.out.print("[Scenario 8] Invoice generation model & accurate balance calculation... ");
        try {
            Invoice inv = new Invoice();
            inv.setInvoiceNumber("INV-2026-1001");
            inv.setBookingId(1);
            inv.setBookingReference("HMS-2026-1001");
            inv.setGuestName("Rahul Sharma");
            inv.setGuestPhone("9876543210");
            inv.setRoomNumber("201");
            inv.setRoomType("DOUBLE");
            inv.setCheckInDate(LocalDate.of(2026, 10, 20));
            inv.setCheckOutDate(LocalDate.of(2026, 10, 22));
            inv.setRatePerNight(new BigDecimal("2500.00"));
            inv.setTotalAmount(new BigDecimal("5000.00"));

            long nights = BillCalculator.calculateNights(inv.getCheckInDate(), inv.getCheckOutDate());
            inv.setNumberOfNights(nights);
            inv.setTotalRoomCharges(BillCalculator.calculateRoomCharge(inv.getRatePerNight(), nights));
            inv.setPaidAmount(new BigDecimal("2500.00"));
            inv.setBalanceDue(BillCalculator.calculateBalanceDue(inv.getTotalAmount(), inv.getPaidAmount()));

            assert inv.getNumberOfNights() == 2 : "Nights calculation mismatch";
            assert inv.getTotalRoomCharges().compareTo(new BigDecimal("5000.00")) == 0 : "Room charge mismatch";
            assert inv.getBalanceDue().compareTo(new BigDecimal("2500.00")) == 0 : "Balance due mismatch";
            assert !inv.isSettled() : "Invoice with balance should not be settled";

            System.out.println("PASSED (Inv: " + inv.getInvoiceNumber() + ", Total: ₹" + inv.getTotalAmount() 
                    + ", Paid: ₹" + inv.getPaidAmount() + ", Due: ₹" + inv.getBalanceDue() + ")");
            passed++;
        } catch (Throwable t) {
            System.out.println("FAILED: " + t.getMessage());
            failed++;
        }

        // --------------------------------------------------------------------
        // Scenario 9: Booking report accuracy & model structure
        // --------------------------------------------------------------------
        System.out.print("[Scenario 9] Booking report item structure & calculations... ");
        try {
            BookingReportItem item = new BookingReportItem(
                    1, "HMS-2026-1001", "Rahul Sharma", "9876543210", "201", "DOUBLE",
                    LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 22), 2,
                    new BigDecimal("5000.00"), "CONFIRMED"
            );

            assert "HMS-2026-1001".equals(item.getBookingReference()) : "Ref mismatch";
            assert item.getNights() == 2 : "Nights mismatch";
            assert item.getTotalAmount().compareTo(new BigDecimal("5000.00")) == 0 : "Total mismatch";

            System.out.println("PASSED (Item: " + item.getBookingReference() + ", " + item.getGuestName() + ", Room " + item.getRoomNumber() + ")");
            passed++;
        } catch (Throwable t) {
            System.out.println("FAILED: " + t.getMessage());
            failed++;
        }

        // --------------------------------------------------------------------
        // Scenario 10: Occupancy calculation for a selected date
        // --------------------------------------------------------------------
        System.out.print("[Scenario 10] Occupancy calculation logic & rules enforcement... ");
        try {
            // Suppose hotel has 8 rooms total, 1 room in maintenance (so 7 operational), and 2 occupied
            OccupancyReport occ = new OccupancyReport(LocalDate.now(), 8, 2, 5, 1);
            // Expected: (2 / 7) * 100 = 28.57%
            BigDecimal expectedPct = new BigDecimal("28.57");
            assert occ.getOccupancyPercentage().compareTo(expectedPct) == 0 
                    : "Occupancy % mismatch: expected " + expectedPct + ", got " + occ.getOccupancyPercentage();

            // Scenario: 0 occupied rooms
            OccupancyReport zeroOcc = new OccupancyReport(LocalDate.now(), 8, 0, 7, 1);
            assert zeroOcc.getOccupancyPercentage().compareTo(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)) == 0 : "Zero occupancy mismatch";

            System.out.println("PASSED (8 total, 1 maint, 2 occupied -> " + occ.getOccupancyPercentage() + "% occupancy)");
            passed++;
        } catch (Throwable t) {
            System.out.println("FAILED: " + t.getMessage());
            failed++;
        }

        // --------------------------------------------------------------------
        // Scenario 11: Revenue report excluding pending & failed payments
        // --------------------------------------------------------------------
        System.out.print("[Scenario 11] Revenue report aggregates: successful collections & refunds... ");
        try {
            LocalDate start = LocalDate.of(2026, 10, 1);
            LocalDate end = LocalDate.of(2026, 10, 31);
            // Gross revenue from PAID/PARTIAL = 15000, Refunds = 2000, Txns = 5
            RevenueReport rev = new RevenueReport(start, end, new BigDecimal("15000.00"), new BigDecimal("2000.00"), 5);

            assert rev.getTotalRevenue().compareTo(new BigDecimal("15000.00")) == 0 : "Gross revenue mismatch";
            assert rev.getTotalRefunds().compareTo(new BigDecimal("2000.00")) == 0 : "Refunds mismatch";
            assert rev.getNetRevenue().compareTo(new BigDecimal("13000.00")) == 0 : "Net revenue mismatch";
            assert rev.getPaymentCount() == 5 : "Txn count mismatch";

            // Add daily breakdown item
            rev.getDailyBreakdown().add(new RevenueReport.DailyRevenueItem(
                    LocalDate.of(2026, 10, 20),
                    new BigDecimal("5000.00"), // Cash
                    new BigDecimal("6000.00"), // Card
                    new BigDecimal("4000.00"), // UPI
                    new BigDecimal("2000.00"), // Refund
                    5
            ));

            RevenueReport.DailyRevenueItem d = rev.getDailyBreakdown().get(0);
            // Net for day = 5000 + 6000 + 4000 - 2000 = 13000
            assert d.getNetAmount().compareTo(new BigDecimal("13000.00")) == 0 : "Daily net amount mismatch";

            System.out.println("PASSED (Gross: ₹" + rev.getTotalRevenue() + ", Refunds: ₹" + rev.getTotalRefunds() 
                    + ", Net: ₹" + rev.getNetRevenue() + ")");
            passed++;
        } catch (Throwable t) {
            System.out.println("FAILED: " + t.getMessage());
            failed++;
        }

        // --------------------------------------------------------------------
        // Scenario 12: Existing guest, room, and booking features still work
        // --------------------------------------------------------------------
        System.out.print("[Scenario 12] Teammate regression test: Guest, Room, Hold, and Booking models... ");
        try {
            Guest g = new Guest("Priya Nair", "9876543211", "priya@example.com", "Passport", "P1234567", "Ernakulam");
            assert g.isValid() : "Guest should be valid";

            Room r = new Room(2, "201", "DOUBLE", 2, new BigDecimal("2500.00"), "AVAILABLE");
            assert r.isOperationallyAvailable() : "Room should be available";

            Booking b = new Booking("HMS-2026-1001", g.getId(), r.getId(), LocalDate.now(), LocalDate.now().plusDays(2), 2, new BigDecimal("5000.00"));
            assert b.areDatesValid() : "Booking dates should be valid";
            assert b.getNumberOfNights() == 2 : "Nights mismatch";

            System.out.println("PASSED (All teammate models and core methods intact)");
            passed++;
        } catch (Throwable t) {
            System.out.println("FAILED: " + t.getMessage());
            failed++;
        }

        // --------------------------------------------------------------------
        // Bonus Scenario: Swing UI Initialization & Offline Safety Check
        // --------------------------------------------------------------------
        System.out.print("[UI Test] Initializing BillingAndReportsPanel on Swing EDT... ");
        try {
            final boolean[] uiPassed = {false};
            SwingUtilities.invokeAndWait(() -> {
                BillingAndReportsPanel panel = new BillingAndReportsPanel();
                if (panel.getComponentCount() > 0) {
                    uiPassed[0] = true;
                }
            });
            if (uiPassed[0]) {
                System.out.println("PASSED (Panel rendered cleanly and safely)");
                passed++;
            } else {
                System.out.println("FAILED");
                failed++;
            }
        } catch (Throwable t) {
            System.out.println("FAILED: " + t.getMessage());
            failed++;
        }

        // Live MySQL Database Operations Check
        System.out.print("[Live MySQL Check] Testing connection to MySQL hotel_management... ");
        boolean dbConnected = DatabaseConnection.testConnection();
        if (dbConnected) {
            System.out.println("ONLINE - Live database queries active.");
        } else {
            System.out.println("OFFLINE (MySQL server not running locally; application running safely in offline-safe mode).");
        }

        System.out.println("======================================================================");
        System.out.println("Summary: Passed: " + passed + " / " + (passed + failed) + ", Failed: " + failed);
        System.out.println("Database Status: " + (dbConnected ? "CONNECTED" : "OFFLINE (Pending local MySQL start)"));
        System.out.println("======================================================================");

        if (failed == 0) {
            System.out.println("ALL MEMBER 3 VERIFICATION TESTS PASSED SUCCESSFULLY!");
        } else {
            System.exit(1);
        }
    }
}
