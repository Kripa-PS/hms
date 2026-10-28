package com.hms.dao;

import com.hms.model.BookingReportItem;
import com.hms.model.OccupancyReport;
import com.hms.model.RevenueReport;

import java.time.LocalDate;
import java.util.List;

/**
 * ReportDAO Interface
 * 
 * Defines reporting operations using real database queries for:
 * 1. Booking Reports (filter by date range & booking status)
 * 2. Occupancy Reports (hotel capacity, occupied rooms, occupancy % for a date)
 * 3. Revenue Reports (successful payments, refunds, net revenue, payment method breakdown)
 */
public interface ReportDAO {

    /**
     * Generates a booking report with optional date filtering and status filtering.
     * 
     * @param startDate start of check-in date range (inclusive, or null for all)
     * @param endDate end of check-in date range (inclusive, or null for all)
     * @param statusFilter booking status filter ('ALL', 'CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT', 'CANCELLED')
     * @return list of booking report items
     */
    List<BookingReportItem> generateBookingReport(LocalDate startDate, LocalDate endDate, String statusFilter);

    /**
     * Calculates hotel room occupancy statistics for a specific target date.
     * Enforces rules:
     * - Only active bookings ('CONFIRMED', 'CHECKED_IN') count towards occupancy.
     * - Cancelled bookings are excluded.
     * - Rooms under maintenance are excluded from operational capacity.
     * 
     * @param targetDate date for which occupancy is calculated
     * @return OccupancyReport with room counts, percentage, and list of occupied rooms
     */
    OccupancyReport generateOccupancyReport(LocalDate targetDate);

    /**
     * Generates a financial revenue report for a specified date range.
     * Enforces rules:
     * - Revenue is calculated ONLY from successful payments ('PAID' and 'PARTIAL').
     * - 'PENDING' and 'FAILED' payments are strictly excluded.
     * - 'REFUNDED' payments are tracked and subtracted to produce Net Revenue.
     * - Includes a daily breakdown of collections across CASH, CARD, and UPI.
     * 
     * @param startDate start date (inclusive)
     * @param endDate end date (inclusive)
     * @return RevenueReport with aggregates and daily breakdowns
     */
    RevenueReport generateRevenueReport(LocalDate startDate, LocalDate endDate);
}
