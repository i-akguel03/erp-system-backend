package com.erp.backend.dto;

import com.erp.backend.domain.DueStatus;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class DueScheduleDto {

    private UUID id;
    private String dueNumber;
    private BigDecimal amount;
    private BigDecimal paidAmount;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dueDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate periodStart;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate periodEnd;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate paidDate;

    private DueStatus status;
    private String paymentMethod;
    private String paymentReference;
    private String notes;

    // Mahnung-bezogene Felder
    private Boolean reminderSent;     // Wrapper für null-fähig
    private Integer reminderCount;    // Wrapper für null-fähig

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate lastReminderDate;

    // Verknüpfte Entitäten (IDs und Namen für Darstellung)
    private UUID subscriptionId;
    private String subscriptionNumber;
    private String subscriptionProductName;

    private UUID contractId;
    private String contractNumber;

    private UUID customerId;
    private String customerName;
    private String customerNumber;

    public DueScheduleDto() {}

    public DueScheduleDto(UUID id, String dueNumber, BigDecimal amount, LocalDate dueDate,
                          DueStatus status, UUID subscriptionId) {
        this.id = id;
        this.dueNumber = dueNumber;
        this.amount = amount;
        this.dueDate = dueDate;
        this.status = status;
        this.subscriptionId = subscriptionId;
    }

    // ===== Hilfsmethoden =====
    public BigDecimal getOpenAmount() {
        if (paidAmount == null) return amount;
        return amount.subtract(paidAmount);
    }

    public boolean isOverdue() {
        return status == DueStatus.OVERDUE ||
                (status == DueStatus.PENDING && dueDate.isBefore(LocalDate.now()));
    }

    public boolean isPaid() {
        return status == DueStatus.PAID;
    }

    public boolean isPartialPaid() {
        return status == DueStatus.PARTIAL_PAID;
    }

    // ===== Getter & Setter =====
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getDueNumber() { return dueNumber; }
    public void setDueNumber(String dueNumber) { this.dueNumber = dueNumber; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public LocalDate getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDate periodStart) { this.periodStart = periodStart; }

    public LocalDate getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(LocalDate periodEnd) { this.periodEnd = periodEnd; }

    public LocalDate getPaidDate() { return paidDate; }
    public void setPaidDate(LocalDate paidDate) { this.paidDate = paidDate; }

    public DueStatus getStatus() { return status; }
    public void setStatus(DueStatus status) { this.status = status; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getPaymentReference() { return paymentReference; }
    public void setPaymentReference(String paymentReference) { this.paymentReference = paymentReference; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Boolean getReminderSent() { return reminderSent; }
    public void setReminderSent(Boolean reminderSent) { this.reminderSent = reminderSent; }

    public Integer getReminderCount() { return reminderCount; }
    public void setReminderCount(Integer reminderCount) { this.reminderCount = reminderCount; }

    public LocalDate getLastReminderDate() { return lastReminderDate; }
    public void setLastReminderDate(LocalDate lastReminderDate) { this.lastReminderDate = lastReminderDate; }

    public UUID getSubscriptionId() { return subscriptionId; }
    public void setSubscriptionId(UUID subscriptionId) { this.subscriptionId = subscriptionId; }

    public String getSubscriptionNumber() { return subscriptionNumber; }
    public void setSubscriptionNumber(String subscriptionNumber) { this.subscriptionNumber = subscriptionNumber; }

    public String getSubscriptionProductName() { return subscriptionProductName; }
    public void setSubscriptionProductName(String subscriptionProductName) { this.subscriptionProductName = subscriptionProductName; }

    public UUID getContractId() { return contractId; }
    public void setContractId(UUID contractId) { this.contractId = contractId; }

    public String getContractNumber() { return contractNumber; }
    public void setContractNumber(String contractNumber) { this.contractNumber = contractNumber; }

    public UUID getCustomerId() { return customerId; }
    public void setCustomerId(UUID customerId) { this.customerId = customerId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerNumber() { return customerNumber; }
    public void setCustomerNumber(String customerNumber) { this.customerNumber = customerNumber; }

    @Override
    public String toString() {
        return "DueScheduleDto{" +
                "id=" + id +
                ", dueNumber='" + dueNumber + '\'' +
                ", amount=" + amount +
                ", dueDate=" + dueDate +
                ", status=" + status +
                ", customerName='" + customerName + '\'' +
                '}';
    }
}
