package com.hms.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Invoice Model
 * 
 * Represents an invoice record generated for a booking.
 * Corresponds to the 'invoices' table in MySQL.
 * Contains both persistent invoice data and joined booking/guest/room
 * metadata for previewing, exporting, and printing.
 */
public class Invoice {

    private int id;
    private String invoiceNumber;      // e.g. "INV-2026-1001"
    private int bookingId;
    private LocalDateTime issueDate;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal balanceDue;
    private String notes;

    // Joined display and printing details
    private String bookingReference;
    private String guestName;
    private String guestPhone;
    private String guestEmail;
    private String guestAddress;
    private String roomNumber;
    private String roomType;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private long numberOfNights;
    private BigDecimal ratePerNight;
    private BigDecimal totalRoomCharges;
    private BigDecimal otherCharges;
    private BigDecimal discount;
    private String paymentStatusSummary; // 'PAID', 'PARTIAL', 'UNPAID'

    public Invoice() {
        this.issueDate = LocalDateTime.now();
        this.totalAmount = BigDecimal.ZERO;
        this.paidAmount = BigDecimal.ZERO;
        this.balanceDue = BigDecimal.ZERO;
        this.otherCharges = BigDecimal.ZERO;
        this.discount = BigDecimal.ZERO;
    }

    public Invoice(String invoiceNumber, int bookingId, BigDecimal totalAmount, 
                   BigDecimal paidAmount, BigDecimal balanceDue, String notes) {
        this.invoiceNumber = invoiceNumber;
        this.bookingId = bookingId;
        this.issueDate = LocalDateTime.now();
        this.totalAmount = totalAmount != null ? totalAmount : BigDecimal.ZERO;
        this.paidAmount = paidAmount != null ? paidAmount : BigDecimal.ZERO;
        this.balanceDue = balanceDue != null ? balanceDue : BigDecimal.ZERO;
        this.notes = notes;
        this.otherCharges = BigDecimal.ZERO;
        this.discount = BigDecimal.ZERO;
    }

    public Invoice(int id, String invoiceNumber, int bookingId, LocalDateTime issueDate, 
                   BigDecimal totalAmount, BigDecimal paidAmount, BigDecimal balanceDue, String notes) {
        this.id = id;
        this.invoiceNumber = invoiceNumber;
        this.bookingId = bookingId;
        this.issueDate = issueDate != null ? issueDate : LocalDateTime.now();
        this.totalAmount = totalAmount != null ? totalAmount : BigDecimal.ZERO;
        this.paidAmount = paidAmount != null ? paidAmount : BigDecimal.ZERO;
        this.balanceDue = balanceDue != null ? balanceDue : BigDecimal.ZERO;
        this.notes = notes;
        this.otherCharges = BigDecimal.ZERO;
        this.discount = BigDecimal.ZERO;
    }

    public boolean isSettled() {
        return balanceDue != null && balanceDue.compareTo(BigDecimal.ZERO) <= 0;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public int getBookingId() {
        return bookingId;
    }

    public void setBookingId(int bookingId) {
        this.bookingId = bookingId;
    }

    public LocalDateTime getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(LocalDateTime issueDate) {
        this.issueDate = issueDate;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(BigDecimal paidAmount) {
        this.paidAmount = paidAmount;
    }

    public BigDecimal getBalanceDue() {
        return balanceDue;
    }

    public void setBalanceDue(BigDecimal balanceDue) {
        this.balanceDue = balanceDue;
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

    public String getGuestPhone() {
        return guestPhone;
    }

    public void setGuestPhone(String guestPhone) {
        this.guestPhone = guestPhone;
    }

    public String getGuestEmail() {
        return guestEmail;
    }

    public void setGuestEmail(String guestEmail) {
        this.guestEmail = guestEmail;
    }

    public String getGuestAddress() {
        return guestAddress;
    }

    public void setGuestAddress(String guestAddress) {
        this.guestAddress = guestAddress;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public String getRoomType() {
        return roomType;
    }

    public void setRoomType(String roomType) {
        this.roomType = roomType;
    }

    public LocalDate getCheckInDate() {
        return checkInDate;
    }

    public void setCheckInDate(LocalDate checkInDate) {
        this.checkInDate = checkInDate;
    }

    public LocalDate getCheckOutDate() {
        return checkOutDate;
    }

    public void setCheckOutDate(LocalDate checkOutDate) {
        this.checkOutDate = checkOutDate;
    }

    public long getNumberOfNights() {
        return numberOfNights;
    }

    public void setNumberOfNights(long numberOfNights) {
        this.numberOfNights = numberOfNights;
    }

    public BigDecimal getRatePerNight() {
        return ratePerNight;
    }

    public void setRatePerNight(BigDecimal ratePerNight) {
        this.ratePerNight = ratePerNight;
    }

    public BigDecimal getTotalRoomCharges() {
        return totalRoomCharges;
    }

    public void setTotalRoomCharges(BigDecimal totalRoomCharges) {
        this.totalRoomCharges = totalRoomCharges;
    }

    public BigDecimal getOtherCharges() {
        return otherCharges;
    }

    public void setOtherCharges(BigDecimal otherCharges) {
        this.otherCharges = otherCharges;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public void setDiscount(BigDecimal discount) {
        this.discount = discount;
    }

    public String getPaymentStatusSummary() {
        return paymentStatusSummary;
    }

    public void setPaymentStatusSummary(String paymentStatusSummary) {
        this.paymentStatusSummary = paymentStatusSummary;
    }

    @Override
    public String toString() {
        return "Invoice[" + invoiceNumber + ", Booking=" + bookingId + " (" + bookingReference 
                + "), Guest=" + guestName + ", Total=Rs. " + totalAmount + ", Paid=Rs. " + paidAmount 
                + ", Balance=Rs. " + balanceDue + "]";
    }
}
