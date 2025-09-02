package com.erp.backend.dto;

import java.math.BigDecimal;

/**
 * DTO für Fälligkeitsplan-Statistiken
 */
public class DueScheduleStatisticsDto {

    private long totalCount;
    private long pendingCount;
    private long overdueCount;
    private long paidCount;
    private long partialPaidCount;
    private long cancelledCount;
    private long needingReminderCount;

    private BigDecimal totalPendingAmount;
    private BigDecimal totalOverdueAmount;
    private BigDecimal totalPaidAmount;

    // Konstruktor mit allen Parametern
    public DueScheduleStatisticsDto(long totalCount, long pendingCount, long overdueCount,
                                    long paidCount, long partialPaidCount, long cancelledCount,
                                    BigDecimal totalPendingAmount, BigDecimal totalOverdueAmount,
                                    BigDecimal totalPaidAmount, long needingReminderCount) {
        this.totalCount = totalCount;
        this.pendingCount = pendingCount;
        this.overdueCount = overdueCount;
        this.paidCount = paidCount;
        this.partialPaidCount = partialPaidCount;
        this.cancelledCount = cancelledCount;
        this.needingReminderCount = needingReminderCount;
        this.totalPendingAmount = totalPendingAmount;
        this.totalOverdueAmount = totalOverdueAmount;
        this.totalPaidAmount = totalPaidAmount;
    }

    // Alternative Konstruktor für Kompatibilität mit alter Version
    public DueScheduleStatisticsDto(long totalCount, long pendingCount, long overdueCount,
                                    long paidCount, BigDecimal totalPendingAmount,
                                    BigDecimal totalOverdueAmount, BigDecimal totalPaidAmount,
                                    long needingReminderCount) {
        this.totalCount = totalCount;
        this.pendingCount = pendingCount;
        this.overdueCount = overdueCount;
        this.paidCount = paidCount;
        this.partialPaidCount = 0L; // Default-Wert
        this.cancelledCount = 0L; // Default-Wert
        this.needingReminderCount = needingReminderCount;
        this.totalPendingAmount = totalPendingAmount;
        this.totalOverdueAmount = totalOverdueAmount;
        this.totalPaidAmount = totalPaidAmount;
    }

    // Standard-Konstruktor
    public DueScheduleStatisticsDto() {
        this.totalPendingAmount = BigDecimal.ZERO;
        this.totalOverdueAmount = BigDecimal.ZERO;
        this.totalPaidAmount = BigDecimal.ZERO;
    }

    // Getters und Setters
    public long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(long totalCount) {
        this.totalCount = totalCount;
    }

    public long getPendingCount() {
        return pendingCount;
    }

    public void setPendingCount(long pendingCount) {
        this.pendingCount = pendingCount;
    }

    public long getOverdueCount() {
        return overdueCount;
    }

    public void setOverdueCount(long overdueCount) {
        this.overdueCount = overdueCount;
    }

    public long getPaidCount() {
        return paidCount;
    }

    public void setPaidCount(long paidCount) {
        this.paidCount = paidCount;
    }

    public long getPartialPaidCount() {
        return partialPaidCount;
    }

    public void setPartialPaidCount(long partialPaidCount) {
        this.partialPaidCount = partialPaidCount;
    }

    public long getCancelledCount() {
        return cancelledCount;
    }

    public void setCancelledCount(long cancelledCount) {
        this.cancelledCount = cancelledCount;
    }

    public long getNeedingReminderCount() {
        return needingReminderCount;
    }

    public void setNeedingReminderCount(long needingReminderCount) {
        this.needingReminderCount = needingReminderCount;
    }

    public BigDecimal getTotalPendingAmount() {
        return totalPendingAmount;
    }

    public void setTotalPendingAmount(BigDecimal totalPendingAmount) {
        this.totalPendingAmount = totalPendingAmount;
    }

    public BigDecimal getTotalOverdueAmount() {
        return totalOverdueAmount;
    }

    public void setTotalOverdueAmount(BigDecimal totalOverdueAmount) {
        this.totalOverdueAmount = totalOverdueAmount;
    }

    public BigDecimal getTotalPaidAmount() {
        return totalPaidAmount;
    }

    public void setTotalPaidAmount(BigDecimal totalPaidAmount) {
        this.totalPaidAmount = totalPaidAmount;
    }

    // Berechnete Eigenschaften
    public BigDecimal getTotalOpenAmount() {
        return totalPendingAmount.add(totalOverdueAmount);
    }

    public double getPaymentRate() {
        if (totalCount == 0) return 0.0;
        return (double) paidCount / totalCount * 100;
    }

    public double getOverdueRate() {
        if (totalCount == 0) return 0.0;
        return (double) overdueCount / totalCount * 100;
    }

    public BigDecimal getAverageAmount() {
        if (totalCount == 0) return BigDecimal.ZERO;
        BigDecimal total = totalPendingAmount.add(totalOverdueAmount).add(totalPaidAmount);
        return total.divide(BigDecimal.valueOf(totalCount), 2, BigDecimal.ROUND_HALF_UP);
    }

    @Override
    public String toString() {
        return "DueScheduleStatisticsDto{" +
                "totalCount=" + totalCount +
                ", pendingCount=" + pendingCount +
                ", overdueCount=" + overdueCount +
                ", paidCount=" + paidCount +
                ", partialPaidCount=" + partialPaidCount +
                ", cancelledCount=" + cancelledCount +
                ", needingReminderCount=" + needingReminderCount +
                ", totalPendingAmount=" + totalPendingAmount +
                ", totalOverdueAmount=" + totalOverdueAmount +
                ", totalPaidAmount=" + totalPaidAmount +
                '}';
    }
}