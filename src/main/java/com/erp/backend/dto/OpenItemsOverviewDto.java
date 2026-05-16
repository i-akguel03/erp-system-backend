package com.erp.backend.dto;

import java.math.BigDecimal;

public class OpenItemsOverviewDto {

    private BigDecimal totalOutstandingAmount;
    private long totalOpenCount;

    private StatusBreakdown open;
    private StatusBreakdown partiallyPaid;
    private StatusBreakdown overdue;

    private AgingBreakdown aging;

    public OpenItemsOverviewDto() {}

    public BigDecimal getTotalOutstandingAmount() { return totalOutstandingAmount; }
    public void setTotalOutstandingAmount(BigDecimal totalOutstandingAmount) { this.totalOutstandingAmount = totalOutstandingAmount; }

    public long getTotalOpenCount() { return totalOpenCount; }
    public void setTotalOpenCount(long totalOpenCount) { this.totalOpenCount = totalOpenCount; }

    public StatusBreakdown getOpen() { return open; }
    public void setOpen(StatusBreakdown open) { this.open = open; }

    public StatusBreakdown getPartiallyPaid() { return partiallyPaid; }
    public void setPartiallyPaid(StatusBreakdown partiallyPaid) { this.partiallyPaid = partiallyPaid; }

    public StatusBreakdown getOverdue() { return overdue; }
    public void setOverdue(StatusBreakdown overdue) { this.overdue = overdue; }

    public AgingBreakdown getAging() { return aging; }
    public void setAging(AgingBreakdown aging) { this.aging = aging; }

    public static class StatusBreakdown {
        private long count;
        private BigDecimal amount;

        public StatusBreakdown(long count, BigDecimal amount) {
            this.count = count;
            this.amount = amount != null ? amount : BigDecimal.ZERO;
        }

        public long getCount() { return count; }
        public BigDecimal getAmount() { return amount; }
    }

    public static class AgingBreakdown {
        private BigDecimal current;
        private BigDecimal days1to30;
        private BigDecimal days31to60;
        private BigDecimal days61to90;
        private BigDecimal over90days;

        public AgingBreakdown(BigDecimal current, BigDecimal days1to30,
                              BigDecimal days31to60, BigDecimal days61to90,
                              BigDecimal over90days) {
            this.current = orZero(current);
            this.days1to30 = orZero(days1to30);
            this.days31to60 = orZero(days31to60);
            this.days61to90 = orZero(days61to90);
            this.over90days = orZero(over90days);
        }

        private static BigDecimal orZero(BigDecimal v) {
            return v != null ? v : BigDecimal.ZERO;
        }

        public BigDecimal getCurrent() { return current; }
        public BigDecimal getDays1to30() { return days1to30; }
        public BigDecimal getDays31to60() { return days31to60; }
        public BigDecimal getDays61to90() { return days61to90; }
        public BigDecimal getOver90days() { return over90days; }
    }
}
