package com.hms.dao;

import com.hms.model.Invoice;

import java.util.List;
import java.util.Optional;

/**
 * InvoiceDAO Interface
 * 
 * Defines operations for generating, saving, and retrieving invoices.
 */
public interface InvoiceDAO {

    /**
     * Generates a persistent invoice for a booking based on actual stay and payment data.
     * Computes room charges, amount paid, and balance due.
     * Inserts or updates the record in the 'invoices' table.
     * 
     * @param bookingId target booking ID
     * @param notes optional invoice notes / terms
     * @return Generated Invoice object with full metadata
     */
    Invoice generateInvoice(int bookingId, String notes);

    /**
     * Finds an invoice by its unique booking ID.
     * 
     * @param bookingId booking ID
     * @return Optional containing Invoice with metadata if found
     */
    Optional<Invoice> findInvoiceByBookingId(int bookingId);

    default Optional<Invoice> findByBookingId(int bookingId) {
        return findInvoiceByBookingId(bookingId);
    }

    /**
     * Finds an invoice by its unique invoice number (e.g. INV-2026-1001).
     * 
     * @param invoiceNumber invoice number
     * @return Optional containing Invoice if found
     */
    Optional<Invoice> findInvoiceByNumber(String invoiceNumber);

    /**
     * Retrieves all saved invoices in the system, newest first.
     * 
     * @return list of invoices
     */
    List<Invoice> findAllInvoices();

    /**
     * Retrieves all invoices for a specific guest, enforcing customer data isolation.
     * 
     * @param guestId target guest ID
     * @return list of invoices for the guest
     */
    List<Invoice> findInvoicesByGuestId(int guestId);
}
