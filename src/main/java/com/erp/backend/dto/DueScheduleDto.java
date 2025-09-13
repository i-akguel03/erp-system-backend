package com.erp.backend.dto;

import com.erp.backend.domain.DueStatus;
import com.erp.backend.domain.Subscription;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO für Fälligkeitspläne (DueSchedule)
 * Defensiv gestaltet, um fehlende Subscriptions/Contracts zu tolerieren.
 */
public class DueScheduleDto {

    private UUID id;
    private String dueNumber;
    private UUID subscriptionId;
    private String subscriptionNumber;
    private BigDecimal amount;
    private BigDecimal paidAmount;
    private LocalDate dueDate;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private DueStatus status;
    private LocalDate paidDate;
    private String paymentMethod;
    private String paymentReference;
    private String notes;
    private boolean reminderSent;
    private int reminderCount;
    private LocalDate lastReminderDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    // Erweiterte Felder
    private String customerName; // Optional, über Contract → Customer
    private String productName;  // Optional, über Contract → Product

    // Konstruktoren
    public DueScheduleDto() {
        this.paidAmount = BigDecimal.ZERO;
        this.reminderSent = false;
        this.reminderCount = 0;
        this.customerName = "Unknown Customer";
        this.productName = "Unknown Product";
    }

    public DueScheduleDto(UUID subscriptionId, BigDecimal amount, LocalDate dueDate,
                          LocalDate periodStart, LocalDate periodEnd) {
        this();
        this.subscriptionId = subscriptionId;
        this.amount = amount;
        this.dueDate = dueDate;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.status = DueStatus.ACTIVE;
    }

    // Getters & Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getDueNumber() { return dueNumber; }
    public void setDueNumber(String dueNumber) { this.dueNumber = dueNumber; }

    public UUID getSubscriptionId() { return subscriptionId; }
    public void setSubscriptionId(UUID subscriptionId) { this.subscriptionId = subscriptionId; }

    public String getSubscriptionNumber() { return subscriptionNumber; }
    public void setSubscriptionNumber(String subscriptionNumber) { this.subscriptionNumber = subscriptionNumber; }

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

    public DueStatus getStatus() { return status; }
    public void setStatus(DueStatus status) { this.status = status; }

    public LocalDate getPaidDate() { return paidDate; }
    public void setPaidDate(LocalDate paidDate) { this.paidDate = paidDate; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getPaymentReference() { return paymentReference; }
    public void setPaymentReference(String paymentReference) { this.paymentReference = paymentReference; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public boolean isReminderSent() { return reminderSent; }
    public void setReminderSent(boolean reminderSent) { this.reminderSent = reminderSent; }

    public int getReminderCount() { return reminderCount; }
    public void setReminderCount(int reminderCount) { this.reminderCount = reminderCount; }

    public LocalDate getLastReminderDate() { return lastReminderDate; }
    public void setLastReminderDate(LocalDate lastReminderDate) { this.lastReminderDate = lastReminderDate; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) {
        this.customerName = (customerName != null) ? customerName : "Unknown Customer";
    }

    public String getProductName() { return productName; }
    public void setProductName(String productName) {
        this.productName = (productName != null) ? productName : "Unknown Product";
    }

    // Berechnete Eigenschaften
    public BigDecimal getOutstandingAmount() {
        if (paidAmount == null) return amount;
        return amount.subtract(paidAmount);
    }

    public boolean isOverdue() {
        return dueDate != null && dueDate.isBefore(LocalDate.now()) &&
                (status == DueStatus.ACTIVE);
    }

    public boolean isFullyPaid() {
        return status == DueStatus.COMPLETED || (paidAmount != null && paidAmount.compareTo(amount) >= 0);
    }

    public long getDaysOverdue() {
        if (!isOverdue()) return 0;
        return dueDate.until(LocalDate.now()).getDays();
    }

    public boolean needsReminder() {
        return isOverdue() && !reminderSent;
    }

    // Validierung
    public boolean isValidForCreation() {
        return subscriptionId != null &&
                amount != null && amount.compareTo(BigDecimal.ZERO) > 0 &&
                dueDate != null &&
                periodStart != null &&
                periodEnd != null && // <-- hier prüfen
                !periodStart.isAfter(periodEnd) &&
                !dueDate.isBefore(periodEnd);
    }


    @Override
    public String toString() {
        return "DueScheduleDto{" +
                "id=" + id +
                ", dueNumber='" + dueNumber + '\'' +
                ", subscriptionId=" + subscriptionId +
                ", amount=" + amount +
                ", paidAmount=" + paidAmount +
                ", dueDate=" + dueDate +
                ", status=" + status +
                ", customerName='" + customerName + '\'' +
                ", productName='" + productName + '\'' +
                '}';
    }

    public Subscription getSubscription() {
        return subscription;
    }

    public void setSubscription(Subscription subscription) {
        this.subscription = subscription;
    }
}
