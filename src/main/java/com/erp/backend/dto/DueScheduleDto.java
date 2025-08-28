package com.erp.backend.dto;

import com.erp.backend.domain.DueStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

// Haupt-DTO für DueSchedule
public class DueScheduleDto {

    private UUID id;

    private String dueNumber;

    @NotNull(message = "Fälligkeitsdatum ist erforderlich")
    @Future(message = "Fälligkeitsdatum muss in der Zukunft liegen")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dueDate;

    @NotNull(message = "Betrag ist erforderlich")
    @DecimalMin(value = "0.01", message = "Betrag muss größer als 0 sein")
    @Digits(integer = 8, fraction = 2, message = "Betrag darf maximal 8 Vorkomma- und 2 Nachkommastellen haben")
    private BigDecimal amount;

    @NotNull(message = "Periode Start ist erforderlich")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate periodStart;

    @NotNull(message = "Periode Ende ist erforderlich")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate periodEnd;

    @NotNull(message = "Status ist erforderlich")
    private DueStatus status;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate paidDate;

    @DecimalMin(value = "0.00", message = "Bezahlter Betrag darf nicht negativ sein")
    private BigDecimal paidAmount;

    @Size(max = 100, message = "Zahlungsmethode darf maximal 100 Zeichen haben")
    private String paymentMethod;

    @Size(max = 200, message = "Zahlungsreferenz darf maximal 200 Zeichen haben")
    private String paymentReference;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdDate;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedDate;

    @Size(max = 500, message = "Notizen dürfen maximal 500 Zeichen haben")
    private String notes;

    private Boolean reminderSent;

    @Min(value = 0, message = "Anzahl Mahnungen darf nicht negativ sein")
    private Integer reminderCount;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate lastReminderDate;

    @NotNull(message = "Abonnement ID ist erforderlich")
    private UUID subscriptionId;

    // Zusätzliche Felder für die Anzeige
    private String subscriptionProductName;
    private String customerName;
    private String contractNumber;

    // Constructors
    public DueScheduleDto() {}

    // Getter & Setter
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getDueNumber() {
        return dueNumber;
    }

    public void setDueNumber(String dueNumber) {
        this.dueNumber = dueNumber;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDate getPeriodStart() {
        return periodStart;
    }

    public void setPeriodStart(LocalDate periodStart) {
        this.periodStart = periodStart;
    }

    public LocalDate getPeriodEnd() {
        return periodEnd;
    }

    public void setPeriodEnd(LocalDate periodEnd) {
        this.periodEnd = periodEnd;
    }

    public DueStatus getStatus() {
        return status;
    }

    public void setStatus(DueStatus status) {
        this.status = status;
    }

    public LocalDate getPaidDate() {
        return paidDate;
    }

    public void setPaidDate(LocalDate paidDate) {
        this.paidDate = paidDate;
    }

    public BigDecimal getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(BigDecimal paidAmount) {
        this.paidAmount = paidAmount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public void setPaymentReference(String paymentReference) {
        this.paymentReference = paymentReference;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public LocalDateTime getUpdatedDate() {
        return updatedDate;
    }

    public void setUpdatedDate(LocalDateTime updatedDate) {
        this.updatedDate = updatedDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Boolean getReminderSent() {
        return reminderSent;
    }

    public void setReminderSent(Boolean reminderSent) {
        this.reminderSent = reminderSent;
    }

    public Integer getReminderCount() {
        return reminderCount;
    }

    public void setReminderCount(Integer reminderCount) {
        this.reminderCount = reminderCount;
    }

    public LocalDate getLastReminderDate() {
        return lastReminderDate;
    }

    public void setLastReminderDate(LocalDate lastReminderDate) {
        this.lastReminderDate = lastReminderDate;
    }

    public UUID getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(UUID subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public String getSubscriptionProductName() {
        return subscriptionProductName;
    }

    public void setSubscriptionProductName(String subscriptionProductName) {
        this.subscriptionProductName = subscriptionProductName;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getContractNumber() {
        return contractNumber;
    }

    public void setContractNumber(String contractNumber) {
        this.contractNumber = contractNumber;
    }
}
