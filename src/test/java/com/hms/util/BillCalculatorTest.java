package com.hms.util;

import com.hms.model.Payment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * BillCalculatorTest
 * 
 * Verifies room charge calculation, monetary rounding, date validation,
 * balance tracking, and overpayment prevention.
 */
public class BillCalculatorTest {

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("  Testing BillCalculator: Business & Math Logic  ");
        System.out.println("=================================================");

        int passed = 0;
        int failed = 0;

        // Test 1: One-Night Calculation
        System.out.print("[Test 1] 1-Night Calculation (1500 * 1 = 1500.00)... ");
        BigDecimal rate = new BigDecimal("1500.00");
        BigDecimal oneNight = BillCalculator.calculateRoomCharge(rate, 1);
        if (oneNight.compareTo(new BigDecimal("1500.00")) == 0) {
            System.out.println("PASSED (₹" + oneNight + ")");
            passed++;
        } else {
            System.out.println("FAILED (" + oneNight + ")");
            failed++;
        }

        // Test 2: Multiple Nights Calculation
        System.out.print("[Test 2] Multi-Night Calculation (2500.50 * 3 = 7501.50)... ");
        BigDecimal multiRate = new BigDecimal("2500.50");
        BigDecimal multiNights = BillCalculator.calculateRoomCharge(multiRate, 3);
        if (multiNights.compareTo(new BigDecimal("7501.50")) == 0) {
            System.out.println("PASSED (₹" + multiNights + ")");
            passed++;
        } else {
            System.out.println("FAILED (" + multiNights + ")");
            failed++;
        }

        // Test 3: Date Validation: checkOut > checkIn
        System.out.print("[Test 3] Date Validation: Nights between 2026-11-01 and 2026-11-05 = 4... ");
        LocalDate in = LocalDate.of(2026, 11, 1);
        LocalDate out = LocalDate.of(2026, 11, 5);
        long nights = BillCalculator.calculateNights(in, out);
        if (nights == 4) {
            System.out.println("PASSED (" + nights + " nights)");
            passed++;
        } else {
            System.out.println("FAILED (" + nights + ")");
            failed++;
        }

        // Test 4: Invalid Dates (checkOut <= checkIn)
        System.out.print("[Test 4] Invalid Dates Rejection (checkOut <= checkIn)... ");
        try {
            BillCalculator.calculateNights(out, in); // Reversed!
            System.out.println("FAILED (Should have thrown IllegalArgumentException)");
            failed++;
        } catch (IllegalArgumentException e) {
            System.out.println("PASSED (" + e.getMessage() + ")");
            passed++;
        }

        // Test 5: Paid Amount with Partial & Full Payments
        System.out.print("[Test 5] Paid Amount Calculation (Partial + Paid)... ");
        List<Payment> payments = new ArrayList<>();
        payments.add(new Payment(1, new BigDecimal("2000.00"), "UPI", "PARTIAL", "TXN-01", "Advance"));
        payments.add(new Payment(1, new BigDecimal("1500.00"), "CARD", "PAID", "TXN-02", "Settlement"));
        BigDecimal totalPaid = BillCalculator.calculateTotalPaid(payments);
        if (totalPaid.compareTo(new BigDecimal("3500.00")) == 0) {
            System.out.println("PASSED (₹" + totalPaid + ")");
            passed++;
        } else {
            System.out.println("FAILED (" + totalPaid + ")");
            failed++;
        }

        // Test 6: Exclusion of PENDING and FAILED payments
        System.out.print("[Test 6] Exclusion of PENDING & FAILED payments from revenue... ");
        payments.add(new Payment(1, new BigDecimal("5000.00"), "CARD", "FAILED", "TXN-FAIL", "Card Declined"));
        payments.add(new Payment(1, new BigDecimal("1000.00"), "UPI", "PENDING", "TXN-PEND", "Processing"));
        BigDecimal paidWithPendingFailed = BillCalculator.calculateTotalPaid(payments);
        if (paidWithPendingFailed.compareTo(new BigDecimal("3500.00")) == 0) {
            System.out.println("PASSED (Still ₹" + paidWithPendingFailed + ", correctly excluded)");
            passed++;
        } else {
            System.out.println("FAILED (" + paidWithPendingFailed + ")");
            failed++;
        }

        // Test 7: Refund handling
        System.out.print("[Test 7] Refund handling (3500 - 500 refund = 3000)... ");
        payments.add(new Payment(1, new BigDecimal("500.00"), "CASH", "REFUNDED", "TXN-REF", "Cancellation refund"));
        BigDecimal paidAfterRefund = BillCalculator.calculateTotalPaid(payments);
        if (paidAfterRefund.compareTo(new BigDecimal("3000.00")) == 0) {
            System.out.println("PASSED (₹" + paidAfterRefund + ")");
            passed++;
        } else {
            System.out.println("FAILED (" + paidAfterRefund + ")");
            failed++;
        }

        // Test 8: Balance Due Calculation
        System.out.print("[Test 8] Balance Due Calculation (Total 5000 - Paid 3000 = 2000)... ");
        BigDecimal totalBill = new BigDecimal("5000.00");
        BigDecimal balance = BillCalculator.calculateBalanceDue(totalBill, paidAfterRefund);
        if (balance.compareTo(new BigDecimal("2000.00")) == 0) {
            System.out.println("PASSED (₹" + balance + ")");
            passed++;
        } else {
            System.out.println("FAILED (" + balance + ")");
            failed++;
        }

        // Test 9: Prevention of Overpayment
        System.out.print("[Test 9] Overpayment Prevention (Attempting 2500 against 2000 balance)... ");
        try {
            BillCalculator.validatePaymentAmount(new BigDecimal("2500.00"), balance);
            System.out.println("FAILED (Should have thrown IllegalArgumentException)");
            failed++;
        } catch (IllegalArgumentException e) {
            System.out.println("PASSED (Correctly blocked: " + e.getMessage() + ")");
            passed++;
        }

        // Test 10: Zero/Negative Payment Prevention
        System.out.print("[Test 10] Zero/Negative Payment Prevention... ");
        try {
            BillCalculator.validatePaymentAmount(BigDecimal.ZERO, balance);
            System.out.println("FAILED (Should have blocked zero payment)");
            failed++;
        } catch (IllegalArgumentException e) {
            System.out.println("PASSED (" + e.getMessage() + ")");
            passed++;
        }

        System.out.println("-------------------------------------------------");
        System.out.println("Summary: Passed: " + passed + " / " + (passed + failed) + ", Failed: " + failed);
        System.out.println("=================================================");

        if (failed == 0) {
            System.out.println("ALL BILL CALCULATOR TESTS PASSED!");
        } else {
            System.exit(1);
        }
    }
}
