package com.hms.dao;

import com.hms.model.BookingReportItem;
import com.hms.model.OccupancyReport;
import com.hms.model.RevenueReport;
import com.hms.util.BillCalculator;
import com.hms.util.DatabaseConnection;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * ReportDAOImpl
 * 
 * JDBC Implementation of ReportDAO.
 * Features:
 * - Direct database queries with PreparedStatements
 * - Strict adherence to revenue and occupancy rules
 * - Excludes PENDING and FAILED payments from financial reports
 * - Correctly handles refunds and partial payments
 * - Does not count cancelled bookings towards occupancy
 */
public class ReportDAOImpl implements ReportDAO {

    @Override
    public List<BookingReportItem> generateBookingReport(LocalDate startDate, LocalDate endDate, String statusFilter) {
        if (com.hms.util.UserSession.isCustomer()) throw new SecurityException("Access Denied: Reports are restricted to administrators.");
        List<BookingReportItem> list = new ArrayList<>();

        StringBuilder sql = new StringBuilder(
                "SELECT b.id, b.booking_reference, g.full_name, g.phone, r.room_number, r.room_type, "
              + "       b.check_in_date, b.check_out_date, b.total_amount, b.status "
              + "FROM bookings b "
              + "JOIN guests g ON b.guest_id = g.id "
              + "JOIN rooms r ON b.room_id = r.id "
              + "WHERE 1=1 ");

        List<Object> params = new ArrayList<>();

        if (startDate != null) {
            sql.append(" AND b.check_in_date >= ?");
            params.add(Date.valueOf(startDate));
        }
        if (endDate != null) {
            sql.append(" AND b.check_in_date <= ?");
            params.add(Date.valueOf(endDate));
        }
        if (statusFilter != null && !statusFilter.isBlank() && !"ALL".equalsIgnoreCase(statusFilter)) {
            sql.append(" AND b.status = ?");
            params.add(statusFilter.trim().toUpperCase());
        }

        sql.append(" ORDER BY b.check_in_date DESC, b.id DESC");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    LocalDate inDate = rs.getDate("check_in_date").toLocalDate();
                    LocalDate outDate = rs.getDate("check_out_date").toLocalDate();
                    long nights = BillCalculator.calculateNights(inDate, outDate);

                    BookingReportItem item = new BookingReportItem(
                            rs.getInt("id"),
                            rs.getString("booking_reference"),
                            rs.getString("full_name"),
                            rs.getString("phone"),
                            rs.getString("room_number"),
                            rs.getString("room_type"),
                            inDate,
                            outDate,
                            nights,
                            rs.getBigDecimal("total_amount"),
                            rs.getString("status")
                    );
                    list.add(item);
                }
            }
        } catch (SQLException e) {
            System.err.println("[ReportDAO] Error generating booking report: " + e.getMessage());
        }
        return list;
    }

    @Override
    public OccupancyReport generateOccupancyReport(LocalDate targetDate) {
        if (com.hms.util.UserSession.isCustomer()) throw new SecurityException("Access Denied: Reports are restricted to administrators.");
        if (targetDate == null) {
            targetDate = LocalDate.now();
        }

        int totalRooms = 0;
        int maintenanceRooms = 0;
        int occupiedRooms = 0;
        List<OccupancyReport.OccupiedRoomDetail> details = new ArrayList<>();

        String totalRoomsSql = "SELECT COUNT(*) FROM rooms";
        String maintenanceSql = "SELECT COUNT(*) FROM rooms WHERE operational_status = 'MAINTENANCE'";
        String occupiedCountSql = "SELECT COUNT(DISTINCT room_id) FROM bookings "
                                + "WHERE status IN ('CONFIRMED', 'CHECKED_IN') "
                                + "  AND check_in_date <= ? AND check_out_date > ?";
        String occupiedDetailsSql = "SELECT r.room_number, r.room_type, b.booking_reference, g.full_name, "
                                  + "       b.check_in_date, b.check_out_date "
                                  + "FROM bookings b "
                                  + "JOIN rooms r ON b.room_id = r.id "
                                  + "JOIN guests g ON b.guest_id = g.id "
                                  + "WHERE b.status IN ('CONFIRMED', 'CHECKED_IN') "
                                  + "  AND b.check_in_date <= ? AND b.check_out_date > ? "
                                  + "ORDER BY r.room_number ASC";

        try (Connection conn = DatabaseConnection.getConnection()) {
            // 1. Total rooms
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(totalRoomsSql)) {
                if (rs.next()) totalRooms = rs.getInt(1);
            }

            // 2. Maintenance rooms
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(maintenanceSql)) {
                if (rs.next()) maintenanceRooms = rs.getInt(1);
            }

            // 3. Occupied rooms count
            Date sqlTargetDate = Date.valueOf(targetDate);
            try (PreparedStatement stmt = conn.prepareStatement(occupiedCountSql)) {
                stmt.setDate(1, sqlTargetDate);
                stmt.setDate(2, sqlTargetDate);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) occupiedRooms = rs.getInt(1);
                }
            }

            // 4. Occupied details
            try (PreparedStatement stmt = conn.prepareStatement(occupiedDetailsSql)) {
                stmt.setDate(1, sqlTargetDate);
                stmt.setDate(2, sqlTargetDate);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        details.add(new OccupancyReport.OccupiedRoomDetail(
                                rs.getString("room_number"),
                                rs.getString("room_type"),
                                rs.getString("booking_reference"),
                                rs.getString("full_name"),
                                rs.getDate("check_in_date").toLocalDate(),
                                rs.getDate("check_out_date").toLocalDate()
                        ));
                    }
                }
            }

        } catch (SQLException e) {
            System.err.println("[ReportDAO] Error generating occupancy report: " + e.getMessage());
        }

        int availableRooms = Math.max(0, totalRooms - maintenanceRooms - occupiedRooms);
        OccupancyReport report = new OccupancyReport(targetDate, totalRooms, occupiedRooms, availableRooms, maintenanceRooms);
        report.setOccupiedDetails(details);
        return report;
    }

    @Override
    public RevenueReport generateRevenueReport(LocalDate startDate, LocalDate endDate) {
        if (com.hms.util.UserSession.isCustomer()) throw new SecurityException("Access Denied: Reports are restricted to administrators.");
        if (startDate == null) startDate = LocalDate.now().minusDays(30);
        if (endDate == null) endDate = LocalDate.now();

        if (endDate.isBefore(startDate)) {
            LocalDate tmp = startDate;
            startDate = endDate;
            endDate = tmp;
        }

        Timestamp startTs = Timestamp.valueOf(startDate.atStartOfDay());
        Timestamp endTs = Timestamp.valueOf(endDate.atTime(23, 59, 59));

        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalRefunds = BigDecimal.ZERO;
        int paymentCount = 0;
        List<RevenueReport.DailyRevenueItem> dailyBreakdown = new ArrayList<>();

        String revenueSql = "SELECT COALESCE(SUM(amount), 0) AS total_rev, COUNT(*) AS txn_count "
                          + "FROM payments "
                          + "WHERE payment_status IN ('PAID', 'PARTIAL') "
                          + "  AND payment_date BETWEEN ? AND ?";

        String refundSql = "SELECT COALESCE(SUM(amount), 0) AS total_refunds "
                         + "FROM payments "
                         + "WHERE payment_status = 'REFUNDED' "
                         + "  AND payment_date BETWEEN ? AND ?";

        String dailySql = "SELECT DATE(payment_date) AS txn_date, "
                        + "       SUM(CASE WHEN payment_method = 'CASH' AND payment_status IN ('PAID', 'PARTIAL') THEN amount ELSE 0 END) AS cash_amt, "
                        + "       SUM(CASE WHEN payment_method = 'CARD' AND payment_status IN ('PAID', 'PARTIAL') THEN amount ELSE 0 END) AS card_amt, "
                        + "       SUM(CASE WHEN payment_method = 'UPI' AND payment_status IN ('PAID', 'PARTIAL') THEN amount ELSE 0 END) AS upi_amt, "
                        + "       SUM(CASE WHEN payment_status = 'REFUNDED' THEN amount ELSE 0 END) AS refund_amt, "
                        + "       COUNT(CASE WHEN payment_status IN ('PAID', 'PARTIAL', 'REFUNDED') THEN 1 ELSE NULL END) AS txn_cnt "
                        + "FROM payments "
                        + "WHERE payment_status IN ('PAID', 'PARTIAL', 'REFUNDED') "
                        + "  AND payment_date BETWEEN ? AND ? "
                        + "GROUP BY DATE(payment_date) "
                        + "ORDER BY txn_date ASC";

        try (Connection conn = DatabaseConnection.getConnection()) {
            // 1. Total revenue
            try (PreparedStatement stmt = conn.prepareStatement(revenueSql)) {
                stmt.setTimestamp(1, startTs);
                stmt.setTimestamp(2, endTs);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        totalRevenue = rs.getBigDecimal("total_rev");
                        paymentCount = rs.getInt("txn_count");
                    }
                }
            }

            // 2. Total refunds
            try (PreparedStatement stmt = conn.prepareStatement(refundSql)) {
                stmt.setTimestamp(1, startTs);
                stmt.setTimestamp(2, endTs);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        totalRefunds = rs.getBigDecimal("total_refunds");
                    }
                }
            }

            // 3. Daily breakdown
            try (PreparedStatement stmt = conn.prepareStatement(dailySql)) {
                stmt.setTimestamp(1, startTs);
                stmt.setTimestamp(2, endTs);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Date d = rs.getDate("txn_date");
                        if (d != null) {
                            dailyBreakdown.add(new RevenueReport.DailyRevenueItem(
                                    d.toLocalDate(),
                                    rs.getBigDecimal("cash_amt").setScale(2, RoundingMode.HALF_UP),
                                    rs.getBigDecimal("card_amt").setScale(2, RoundingMode.HALF_UP),
                                    rs.getBigDecimal("upi_amt").setScale(2, RoundingMode.HALF_UP),
                                    rs.getBigDecimal("refund_amt").setScale(2, RoundingMode.HALF_UP),
                                    rs.getInt("txn_cnt")
                            ));
                        }
                    }
                }
            }

        } catch (SQLException e) {
            System.err.println("[ReportDAO] Error generating revenue report: " + e.getMessage());
        }

        RevenueReport report = new RevenueReport(startDate, endDate, totalRevenue, totalRefunds, paymentCount);
        report.setDailyBreakdown(dailyBreakdown);
        return report;
    }
}
