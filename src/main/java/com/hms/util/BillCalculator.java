package com.hms.util;

import com.hms.model.Payment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * BillCalculator
 * 
 * Provides financial and stay charge calculations for Hotel Management System.
 * Adheres strictly to:
 * - Room Charge = Room Rate × Number of Nights
 * - All monetary calculations use java.math.BigDecimal with RoundingMode.HALF_UP
 * - Strict date validation (checkOut > checkIn)
 * - Accurate revenue and balance tracking (excludes pending and failed payments)
 * - Overpayment prevention logic
 */
public class BillCalculator {

    public static final int MONEY_SCALE = 2;
    public static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    /**
     * Calculates the number of nights stayed between check-in and check-out dates.
     * Enforces checkOut strictly after checkIn.
     * 
     * @param checkIn check-in date
     * @param checkOut check-out date
     * @return number of nights (minimum 1)
     * @throws IllegalArgumentException if dates are null or checkOut is not after checkIn
     */
    public static long calculateNights(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null || checkOut == null) {
            throw new IllegalArgumentException("Check-in and check-out dates cannot be null.");
        }
        if (!checkOut.isAfter(checkIn)) {
            throw new IllegalArgumentException("Check-out date (" + checkOut 
                    + ") must be strictly after check-in date (" + checkIn + ").");
        }
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        return Math.max(1, nights);
    }

    /**
     * Calculates total room charges: Room Rate × Number of Nights.
     * 
     * @param roomRate price per night (BigDecimal)
     * @param nights number of nights
     * @return total room charges formatted to 2 decimal places
     * @throws IllegalArgumentException if rate is null or negative, or nights < 1
     */
    public static BigDecimal calculateRoomCharge(BigDecimal roomRate, long nights) {
        if (roomRate == null || roomRate.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Room rate must be non-null and non-negative.");
        }
        if (nights < 1) {
            throw new IllegalArgumentException("Number of nights must be at least 1.");
        }
        return roomRate.multiply(BigDecimal.valueOf(nights)).setScale(MONEY_SCALE, ROUNDING_MODE);
    }

    /**
     * Calculates total bill including any extra charges or discounts.
     * Total = (Room Charges + Other Charges) - Discount
     */
    public static BigDecimal calculateTotalBill(BigDecimal roomCharges, BigDecimal otherCharges, BigDecimal discount) {
        BigDecimal total = roomCharges != null ? roomCharges : BigDecimal.ZERO;
        if (otherCharges != null && otherCharges.compareTo(BigDecimal.ZERO) > 0) {
            total = total.add(otherCharges);
        }
        if (discount != null && discount.compareTo(BigDecimal.ZERO) > 0) {
            total = total.subtract(discount);
        }
        if (total.compareTo(BigDecimal.ZERO) < 0) {
            total = BigDecimal.ZERO;
        }
        return total.setScale(MONEY_SCALE, ROUNDING_MODE);
    }

    /**
     * Calculates the total amount paid by summing only successful payments ('PAID' and 'PARTIAL').
     * Explicitly ignores 'PENDING' and 'FAILED' payments.
     * Subtracts 'REFUNDED' payments if any are recorded.
     * 
     * @param payments list of payments for the booking
     * @return total verified paid amount
     */
    public static BigDecimal calculateTotalPaid(List<Payment> payments) {
        if (payments == null || payments.isEmpty()) {
            return BigDecimal.ZERO.setScale(MONEY_SCALE, ROUNDING_MODE);
        }

        BigDecimal totalPaid = BigDecimal.ZERO;
        BigDecimal refunds = BigDecimal.ZERO;

        for (Payment p : payments) {
            if (p == null || p.getAmount() == null) continue;

            String status = p.getPaymentStatus();
            if (status == null) status = "PAID";

            if ("PAID".equalsIgnoreCase(status) || "PARTIAL".equalsIgnoreCase(status)) {
                totalPaid = totalPaid.add(p.getAmount());
            } else if ("REFUNDED".equalsIgnoreCase(status)) {
                refunds = refunds.add(p.getAmount());
            }
            // PENDING and FAILED payments are ignored
        }

        BigDecimal netPaid = totalPaid.subtract(refunds);
        if (netPaid.compareTo(BigDecimal.ZERO) < 0) {
            netPaid = BigDecimal.ZERO;
        }
        return netPaid.setScale(MONEY_SCALE, ROUNDING_MODE);
    }

    /**
     * Calculates the remaining balance due: max(0, Total Bill - Total Paid).
     * 
     * @param totalBill total charges
     * @param amountPaid total amount already paid
     * @return outstanding balance due
     */
    public static BigDecimal calculateBalanceDue(BigDecimal totalBill, BigDecimal amountPaid) {
        BigDecimal bill = totalBill != null ? totalBill : BigDecimal.ZERO;
        BigDecimal paid = amountPaid != null ? amountPaid : BigDecimal.ZERO;
        BigDecimal balance = bill.subtract(paid);
        if (balance.compareTo(BigDecimal.ZERO) < 0) {
            balance = BigDecimal.ZERO;
        }
        return balance.setScale(MONEY_SCALE, ROUNDING_MODE);
    }

    /**
     * Validates payment amount against remaining balance.
     * Prevents:
     * 1. Null or non-positive amounts
     * 2. Overpayments (payment > balance due)
     * 
     * @param paymentAmount amount being submitted
     * @param balanceDue current outstanding balance
     * @throws IllegalArgumentException on invalid amount or overpayment
     */
    public static void validatePaymentAmount(BigDecimal paymentAmount, BigDecimal balanceDue) {
        if (paymentAmount == null || paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than ₹0.00.");
        }
        if (balanceDue == null) {
            balanceDue = BigDecimal.ZERO;
        }
        // Round both to MONEY_SCALE for precise comparison
        BigDecimal roundedAmount = paymentAmount.setScale(MONEY_SCALE, ROUNDING_MODE);
        BigDecimal roundedBalance = balanceDue.setScale(MONEY_SCALE, ROUNDING_MODE);

        if (roundedAmount.compareTo(roundedBalance) > 0) {
            throw new IllegalArgumentException("Overpayment not allowed: Payment amount (₹" 
                    + roundedAmount + ") exceeds outstanding balance (₹" + roundedBalance + ").");
        }
    }

    /**
     * Evaluates appropriate status for a demo payment based on amount and remaining balance.
     * 
     * @param paymentAmount amount being paid
     * @param currentBalance remaining balance before this payment
     * @return 'PAID' if full balance cleared, 'PARTIAL' if partially paid
     */
    public static String determineStatusForAmount(BigDecimal paymentAmount, BigDecimal currentBalance) {
        if (paymentAmount == null || currentBalance == null) {
            return "PARTIAL";
        }
        BigDecimal p = paymentAmount.setScale(MONEY_SCALE, ROUNDING_MODE);
        BigDecimal b = currentBalance.setScale(MONEY_SCALE, ROUNDING_MODE);
        if (p.compareTo(b) >= 0) {
            return "PAID";
        }
        return "PARTIAL";
    }
}
