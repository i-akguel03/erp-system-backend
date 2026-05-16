package com.erp.backend.dto;

import java.math.BigDecimal;

public class DashboardKpiDto {

    private long totalCustomers;
    private long activeSubscriptions;
    private BigDecimal monthlyRecurringRevenue;
    private long openInvoicesCount;
    private BigDecimal openInvoicesTotalAmount;
    private BigDecimal totalOutstandingAmount;
    private long overdueItemsCount;
    private BigDecimal overdueItemsAmount;

    public DashboardKpiDto() {}

    public long getTotalCustomers() { return totalCustomers; }
    public void setTotalCustomers(long totalCustomers) { this.totalCustomers = totalCustomers; }

    public long getActiveSubscriptions() { return activeSubscriptions; }
    public void setActiveSubscriptions(long activeSubscriptions) { this.activeSubscriptions = activeSubscriptions; }

    public BigDecimal getMonthlyRecurringRevenue() { return monthlyRecurringRevenue; }
    public void setMonthlyRecurringRevenue(BigDecimal monthlyRecurringRevenue) { this.monthlyRecurringRevenue = monthlyRecurringRevenue; }

    public long getOpenInvoicesCount() { return openInvoicesCount; }
    public void setOpenInvoicesCount(long openInvoicesCount) { this.openInvoicesCount = openInvoicesCount; }

    public BigDecimal getOpenInvoicesTotalAmount() { return openInvoicesTotalAmount; }
    public void setOpenInvoicesTotalAmount(BigDecimal openInvoicesTotalAmount) { this.openInvoicesTotalAmount = openInvoicesTotalAmount; }

    public BigDecimal getTotalOutstandingAmount() { return totalOutstandingAmount; }
    public void setTotalOutstandingAmount(BigDecimal totalOutstandingAmount) { this.totalOutstandingAmount = totalOutstandingAmount; }

    public long getOverdueItemsCount() { return overdueItemsCount; }
    public void setOverdueItemsCount(long overdueItemsCount) { this.overdueItemsCount = overdueItemsCount; }

    public BigDecimal getOverdueItemsAmount() { return overdueItemsAmount; }
    public void setOverdueItemsAmount(BigDecimal overdueItemsAmount) { this.overdueItemsAmount = overdueItemsAmount; }
}
