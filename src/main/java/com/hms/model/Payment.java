package com.hms.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Payment Model
 * 
 * Represents a payment transaction against a hotel booking.
 * Corresponds to the 'payments' table in MySQL.
 * Demonstrates OOP Encapsulation with monetary precision using BigDecimal.
 */
public class Payment {

    private int id;
    private int bookingId;
    private BigDecimal amount;
    private String paymentMethod;       // 'CASH', 'CARD', 'UPI'
    private String paymentStatus;       // 'PAID', 'PARTIAL', 'PENDING', 'REFUNDED', 'FAILED'
    private LocalDateTime paymentDate;
    private String transactionReference;
    private String notes;

    // Joined / Convenient UI Display fields
    private String bookingReference;
    private String guestName;
    private String roomNumber;

    public Payment() {
        this.paymentMethod = "CASH";
        this.paymentStatus = "PAID";
        this.amount = BigDecimal.ZERO;
        this.paymentDate = LocalDateTime.now();
    }

    public Payment(int bookingId, BigDecimal amount, String paymentMethod, String paymentStatus, 
                   String transactionReference, String notes) {
        this.bookingId = bookingId;
        this.amount = amount != null ? amount : BigDecimal.ZERO;
        this.paymentMethod = paymentMethod != null ? paymentMethod : "CASH";
        this.paymentStatus = paymentStatus != null ? paymentStatus : "PAID";
        this.paymentDate = LocalDateTime.now();
        this.transactionReference = transactionReference;
        this.notes = notes;
    }

    public Payment(int id, int bookingId, BigDecimal amount, String paymentMethod, String paymentStatus, 
                   LocalDateTime paymentDate, String transactionReference, String notes) {
        this.id = id;
        this.bookingId = bookingId;
        this.amount = amount != null ? amount : BigDecimal.ZERO;
        this.paymentMethod = paymentMethod != null ? paymentMethod : "CASH";
        this.paymentStatus = paymentStatus != null ? paymentStatus : "PAID";
        this.paymentDate = paymentDate != null ? paymentDate : LocalDateTime.now();
        this.transactionReference = transactionReference;
        this.notes = notes;
    }

    /**
     * Checks if this payment is successful and counts as recorded revenue.
     */
    public boolean isSuccessful() {
        return "PAID".equalsIgnoreCase(paymentStatus) || "PARTIAL".equalsIgnoreCase(paymentStatus);
    }

    /**
     * Validates payment attributes.
     */
    public boolean isValid() {
        return bookingId > 0 && amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getBookingId() {
        return bookingId;
    }

    public void setBookingId(int bookingId) {
        this.bookingId = bookingId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public LocalDateTime getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDateTime paymentDate) {
        this.paymentDate = paymentDate;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getBookingReference() {
        return bookingReference;
    }

    public void setBookingReference(String bookingReference) {
        this.bookingReference = bookingReference;
    }

    public String getGuestName() {
        return guestName;
    }

    public void setGuestName(String guestName) {
        this.guestName = guestName;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    @Override
    public String toString() {
        return "Payment[ID=" + id + ", Booking=" + bookingId + " (" + bookingReference + "), Amount=₹" 
                + amount + ", Method=" + paymentMethod + ", Status=" + paymentStatus + ", Txn=" + transactionReference + "]";
    }
}
