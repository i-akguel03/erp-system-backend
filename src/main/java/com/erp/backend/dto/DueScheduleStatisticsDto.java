package com.erp.backend.dto;

import java.math.BigDecimal;

// DTO für Statistiken
public class DueScheduleStatisticsDto {

    private Long totalDueSchedules;
    private Long pendingDueSchedules;
    private Long overdueDueSchedules;
    private Long paidDueSchedules;
    private BigDecimal totalPendingAmount;
    private BigDecimal totalOverdueAmount;
    private BigDecimal totalPaidAmount;
    private Long schedulesNeedingReminder;

    // Constructors
    public DueScheduleStatisticsDto() {}

    public DueScheduleStatisticsDto(Long totalDueSchedules, Long pendingDueSchedules,
                                    Long overdueDueSchedules, Long paidDueSchedules,
                                    BigDecimal totalPendingAmount, BigDecimal totalOverdueAmount,
                                    BigDecimal totalPaidAmount, Long schedulesNeedingReminder) {
        this.totalDueSchedules = totalDueSchedules;
        this.pendingDueSchedules = pendingDueSchedules;
        this.overdueDueSchedules = overdueDueSchedules;
        this.paidDueSchedules = paidDueSchedules;
        this.totalPendingAmount = totalPendingAmount;
        this.totalOverdueAmount = totalOverdueAmount;
        this.totalPaidAmount = totalPaidAmount;
        this.schedulesNeedingReminder = schedulesNeedingReminder;
    }

    // Getter & Setter
    public Long getTotalDueSchedules() {
        return totalDueSchedules;
    }

    public void setTotalDueSchedules(Long totalDueSchedules) {
        this.totalDueSchedules = totalDueSchedules;
    }

    public Long getPendingDueSchedules() {
        return pendingDueSchedules;
    }

    public void setPendingDueSchedules(Long pendingDueSchedules) {
        this.pendingDueSchedules = pendingDueSchedules;
    }

    public Long getOverdueDueSchedules() {
        return overdueDueSchedules;
    }

    public void setOverdueDueSchedules(Long overdueDueSchedules) {
        this.overdueDueSchedules = overdueDueSchedules;
    }

    public Long getPaidDueSchedules() {
        return paidDueSchedules;
    }

    public void setPaidDueSchedules(Long paidDueSchedules) {
        this.paidDueSchedules = paidDueSchedules;
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

    public Long getSchedulesNeedingReminder() {
        return schedulesNeedingReminder;
    }

    public void setSchedulesNeedingReminder(Long schedulesNeedingReminder) {
        this.schedulesNeedingReminder = schedulesNeedingReminder;
    }
}