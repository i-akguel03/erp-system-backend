package com.erp.backend.dto;

import com.erp.backend.domain.DueStatus;

import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO für Fälligkeitspläne (DueSchedule).
 *
 * Enthält nur Felder aus der Entity + optionale abgeleitete Informationen
 * für das Frontend (z. B. CustomerName, ProductName).
 */
public class DueScheduleDto {

    private UUID id;
    private String dueNumber;
    private UUID subscriptionId;
    private String subscriptionNumber;

    private LocalDate dueDate;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private DueStatus status;

    private String notes;

    // Erweiterte Felder (für UI, nicht in Entity gespeichert)
    private String customerName;
    private String productName;

    // Konstruktoren
    public DueScheduleDto() {
        this.customerName = "Unknown Customer";
        this.productName = "Unknown Product";
    }

    public DueScheduleDto(UUID subscriptionId,
                          LocalDate dueDate,
                          LocalDate periodStart,
                          LocalDate periodEnd) {
        this();
        this.subscriptionId = subscriptionId;
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

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public LocalDate getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDate periodStart) { this.periodStart = periodStart; }

    public LocalDate getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(LocalDate periodEnd) { this.periodEnd = periodEnd; }

    public DueStatus getStatus() { return status; }
    public void setStatus(DueStatus status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) {
        this.customerName = (customerName != null) ? customerName : "Unknown Customer";
    }

    public String getProductName() { return productName; }
    public void setProductName(String productName) {
        this.productName = (productName != null) ? productName : "Unknown Product";
    }

    // Hilfsmethoden für UI
    public boolean isOverdue() {
        return dueDate != null &&
                status == DueStatus.ACTIVE &&
                dueDate.isBefore(LocalDate.now());
    }

    @Override
    public String toString() {
        return "DueScheduleDto{" +
                "id=" + id +
                ", dueNumber='" + dueNumber + '\'' +
                ", subscriptionId=" + subscriptionId +
                ", dueDate=" + dueDate +
                ", status=" + status +
                ", customerName='" + customerName + '\'' +
                ", productName='" + productName + '\'' +
                '}';
    }
}
