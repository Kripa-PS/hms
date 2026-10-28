package com.hms.dao;

import com.hms.model.Payment;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * PaymentDAO Interface
 * 
 * Defines data access and transactional operations for hotel payments.
 */
public interface PaymentDAO {

    /**
     * Records a payment against a booking within a database transaction.
     * Validates payment amount, prevents overpayment, and prevents duplicate transaction references.
     * 
     * @param payment Payment object to record
     * @return Payment object with generated ID and timestamp
     * @throws OverpaymentException if payment amount exceeds outstanding balance
     * @throws DuplicateTransactionException if transaction reference is already recorded
     * @throws IllegalArgumentException on missing booking or invalid amount
     * @throws RuntimeException on database connection/SQL errors
     */
    Payment recordPayment(Payment payment);

    /**
     * Retrieves all payments recorded for a specific booking, ordered by date.
     * 
     * @param bookingId booking ID
     * @return list of payments
     */
    List<Payment> findPaymentsByBookingId(int bookingId);

    /**
     * Retrieves all payments across all bookings with joined guest and room metadata.
     * 
     * @return list of payments
     */
    List<Payment> findAllPayments();

    /**
     * Finds a payment by its primary key ID.
     * 
     * @param id payment ID
     * @return Optional containing Payment if found
     */
    Optional<Payment> findById(int id);

    /**
     * Calculates the total amount paid so far for a booking (sum of 'PAID' and 'PARTIAL' payments).
     * 
     * @param bookingId booking ID
     * @return total verified paid amount (BigDecimal)
     */
    BigDecimal getTotalPaidForBooking(int bookingId);

    /**
     * Checks if a transaction reference already exists in the payments table.
     * 
     * @param txnRef transaction reference code
     * @return true if exists, false otherwise
     */
    boolean isTransactionReferenceExists(String txnRef);

    /**
     * Thrown when payment amount exceeds outstanding balance.
     */
    class OverpaymentException extends RuntimeException {
        public OverpaymentException(String message) {
            super(message);
        }
    }

    /**
     * Thrown when duplicate transaction reference is submitted.
     */
    class DuplicateTransactionException extends RuntimeException {
        public DuplicateTransactionException(String message) {
            super(message);
        }
    }
}
