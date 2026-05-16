package com.erp.backend.dto;

import java.math.BigDecimal;

public class OutstandingPaymentsDto {

    private BigDecimal totalOutstandingAmount;
    private long totalOutstandingCount;
    private BigDecimal overdueAmount;
    private long overdueCount;
    private BigDecimal partiallyPaidAmount;
    private long partiallyPaidCount;
    private BigDecimal totalCollectedAmount;

    public OutstandingPaymentsDto() {}

    public BigDecimal getTotalOutstandingAmount() { return totalOutstandingAmount; }
    public void setTotalOutstandingAmount(BigDecimal totalOutstandingAmount) { this.totalOutstandingAmount = totalOutstandingAmount; }

    public long getTotalOutstandingCount() { return totalOutstandingCount; }
    public void setTotalOutstandingCount(long totalOutstandingCount) { this.totalOutstandingCount = totalOutstandingCount; }

    public BigDecimal getOverdueAmount() { return overdueAmount; }
    public void setOverdueAmount(BigDecimal overdueAmount) { this.overdueAmount = overdueAmount; }

    public long getOverdueCount() { return overdueCount; }
    public void setOverdueCount(long overdueCount) { this.overdueCount = overdueCount; }

    public BigDecimal getPartiallyPaidAmount() { return partiallyPaidAmount; }
    public void setPartiallyPaidAmount(BigDecimal partiallyPaidAmount) { this.partiallyPaidAmount = partiallyPaidAmount; }

    public long getPartiallyPaidCount() { return partiallyPaidCount; }
    public void setPartiallyPaidCount(long partiallyPaidCount) { this.partiallyPaidCount = partiallyPaidCount; }

    public BigDecimal getTotalCollectedAmount() { return totalCollectedAmount; }
    public void setTotalCollectedAmount(BigDecimal totalCollectedAmount) { this.totalCollectedAmount = totalCollectedAmount; }
}
