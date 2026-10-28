package com.hms.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * RevenueReport
 * 
 * Represents revenue earned over a specified date range.
 * Enforces business rules:
 * - Revenue is calculated from successful payments only (PAID and PARTIAL).
 * - Excludes PENDING and FAILED payments.
 * - Handles partial payments accurately.
 * - Accounts for refunds by tracking refund amounts and net revenue.
 * - Provides date-wise breakdown and payment method breakdown (Cash, Card, UPI).
 */
public class RevenueReport {

    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal totalRevenue;      // Sum of PAID and PARTIAL payments
    private BigDecimal totalRefunds;      // Sum of REFUNDED payments
    private BigDecimal netRevenue;        // totalRevenue - totalRefunds
    private int paymentCount;
    private List<DailyRevenueItem> dailyBreakdown;

    public RevenueReport() {
        this.totalRevenue = BigDecimal.ZERO;
        this.totalRefunds = BigDecimal.ZERO;
        this.netRevenue = BigDecimal.ZERO;
        this.dailyBreakdown = new ArrayList<>();
    }

    public RevenueReport(LocalDate startDate, LocalDate endDate, BigDecimal totalRevenue, 
                         BigDecimal totalRefunds, int paymentCount) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.totalRevenue = totalRevenue != null ? totalRevenue : BigDecimal.ZERO;
        this.totalRefunds = totalRefunds != null ? totalRefunds : BigDecimal.ZERO;
        this.netRevenue = this.totalRevenue.subtract(this.totalRefunds);
        this.paymentCount = paymentCount;
        this.dailyBreakdown = new ArrayList<>();
    }

    public void recalculateNet() {
        if (totalRevenue == null) totalRevenue = BigDecimal.ZERO;
        if (totalRefunds == null) totalRefunds = BigDecimal.ZERO;
        this.netRevenue = totalRevenue.subtract(totalRefunds);
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue;
        recalculateNet();
    }

    public BigDecimal getTotalRefunds() {
        return totalRefunds;
    }

    public void setTotalRefunds(BigDecimal totalRefunds) {
        this.totalRefunds = totalRefunds;
        recalculateNet();
    }

    public BigDecimal getNetRevenue() {
        return netRevenue;
    }

    public void setNetRevenue(BigDecimal netRevenue) {
        this.netRevenue = netRevenue;
    }

    public int getPaymentCount() {
        return paymentCount;
    }

    public void setPaymentCount(int paymentCount) {
        this.paymentCount = paymentCount;
    }

    public List<DailyRevenueItem> getDailyBreakdown() {
        return dailyBreakdown;
    }

    public void setDailyBreakdown(List<DailyRevenueItem> dailyBreakdown) {
        this.dailyBreakdown = dailyBreakdown;
    }

    /**
     * Inner class representing daily payment aggregations by method.
     */
    public static class DailyRevenueItem {
        private LocalDate date;
        private BigDecimal cashAmount;
        private BigDecimal cardAmount;
        private BigDecimal upiAmount;
        private BigDecimal refundAmount;
        private BigDecimal netAmount;
        private int transactionCount;

        public DailyRevenueItem() {
            this.cashAmount = BigDecimal.ZERO;
            this.cardAmount = BigDecimal.ZERO;
            this.upiAmount = BigDecimal.ZERO;
            this.refundAmount = BigDecimal.ZERO;
            this.netAmount = BigDecimal.ZERO;
        }

        public DailyRevenueItem(LocalDate date, BigDecimal cashAmount, BigDecimal cardAmount, 
                                BigDecimal upiAmount, BigDecimal refundAmount, int transactionCount) {
            this.date = date;
            this.cashAmount = cashAmount != null ? cashAmount : BigDecimal.ZERO;
            this.cardAmount = cardAmount != null ? cardAmount : BigDecimal.ZERO;
            this.upiAmount = upiAmount != null ? upiAmount : BigDecimal.ZERO;
            this.refundAmount = refundAmount != null ? refundAmount : BigDecimal.ZERO;
            this.transactionCount = transactionCount;
            this.netAmount = this.cashAmount.add(this.cardAmount).add(this.upiAmount).subtract(this.refundAmount);
        }

        public LocalDate getDate() {
            return date;
        }

        public void setDate(LocalDate date) {
            this.date = date;
        }

        public BigDecimal getCashAmount() {
            return cashAmount;
        }

        public void setCashAmount(BigDecimal cashAmount) {
            this.cashAmount = cashAmount;
        }

        public BigDecimal getCardAmount() {
            return cardAmount;
        }

        public void setCardAmount(BigDecimal cardAmount) {
            this.cardAmount = cardAmount;
        }

        public BigDecimal getUpiAmount() {
            return upiAmount;
        }

        public void setUpiAmount(BigDecimal upiAmount) {
            this.upiAmount = upiAmount;
        }

        public BigDecimal getRefundAmount() {
            return refundAmount;
        }

        public void setRefundAmount(BigDecimal refundAmount) {
            this.refundAmount = refundAmount;
        }

        public BigDecimal getNetAmount() {
            return netAmount;
        }

        public void setNetAmount(BigDecimal netAmount) {
            this.netAmount = netAmount;
        }

        public int getTransactionCount() {
            return transactionCount;
        }

        public void setTransactionCount(int transactionCount) {
            this.transactionCount = transactionCount;
        }
    }
}
