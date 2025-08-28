package com.erp.backend.dto;

import java.math.BigDecimal;

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
    private BigDecimal totalPartialPaidAmount;

    // Zusätzliche Statistiken
    private double paymentRate; // Prozentsatz bezahlter Fälligkeiten
    private double overdueRate;  // Prozentsatz überfälliger Fälligkeiten

    // Konstruktoren
    public DueScheduleStatisticsDto() {}

    public DueScheduleStatisticsDto(long totalCount, long pendingCount, long overdueCount, long paidCount,
                                    BigDecimal totalPendingAmount, BigDecimal totalOverdueAmount,
                                    BigDecimal totalPaidAmount, long needingReminderCount) {
        this.totalCount = totalCount;
        this.pendingCount = pendingCount;
        this.overdueCount = overdueCount;
        this.paidCount = paidCount;
        this.totalPendingAmount = totalPendingAmount;
        this.totalOverdueAmount = totalOverdueAmount;
        this.totalPaidAmount = totalPaidAmount;
        this.needingReminderCount = needingReminderCount;

        // Berechnungen
        this.paymentRate = totalCount > 0 ? (double) paidCount / totalCount * 100 : 0.0;
        this.overdueRate = totalCount > 0 ? (double) overdueCount / totalCount * 100 : 0.0;
    }

    // Berechnete Felder
    public BigDecimal getTotalOpenAmount() {
        return totalPendingAmount.add(totalOverdueAmount).add(totalPartialPaidAmount != null ?
                totalPartialPaidAmount : BigDecimal.ZERO);
    }

    public BigDecimal getTotalAmount() {
        return getTotalOpenAmount().add(totalPaidAmount);
    }

    public long getActiveCount() {
        return pendingCount + overdueCount + partialPaidCount;
    }

    public double getCollectionRate() {
        BigDecimal totalAmount = getTotalAmount();
        if (totalAmount.compareTo(BigDecimal.ZERO) == 0) return 0.0;
        return totalPaidAmount.divide(totalAmount, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)).doubleValue();
    }

    // Getter und Setter
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

    public BigDecimal getTotalPartialPaidAmount() {
        return totalPartialPaidAmount;
    }

    public void setTotalPartialPaidAmount(BigDecimal totalPartialPaidAmount) {
        this.totalPartialPaidAmount = totalPartialPaidAmount;
    }

    public double getPaymentRate() {
        return paymentRate;
    }

    public void setPaymentRate(double paymentRate) {
        this.paymentRate = paymentRate;
    }

    public double getOverdueRate() {
        return overdueRate;
    }

    public void setOverdueRate(double overdueRate) {
        this.overdueRate = overdueRate;
    }

    @Override
    public String toString() {
        return "DueScheduleStatisticsDto{" +
                "totalCount=" + totalCount +
                ", pendingCount=" + pendingCount +
                ", overdueCount=" + overdueCount +
                ", paidCount=" + paidCount +
                ", paymentRate=" + String.format("%.2f", paymentRate) + "%" +
                ", totalPaidAmount=" + totalPaidAmount +
                '}';
    }
}