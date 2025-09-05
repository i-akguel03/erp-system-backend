package com.erp.backend.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "due_schedules")
@SQLDelete(sql = "UPDATE due_schedules SET deleted = true WHERE id = ?")
@SQLRestriction("deleted = false")
public class DueSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "due_number", unique = true)
    private String dueNumber;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end")
    private LocalDate periodEnd;

    @Column(nullable = false)
    private boolean deleted = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DueStatus status;

    @Column(name = "paid_date")
    private LocalDate paidDate;

    @Column(name = "paid_amount", precision = 10, scale = 2)
    private BigDecimal paidAmount;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "payment_reference")
    private String paymentReference;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "updated_date")
    private LocalDateTime updatedDate;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "reminder_sent")
    private Boolean reminderSent = false;

    @Column(name = "reminder_count")
    private Integer reminderCount = 0;

    @Column(name = "last_reminder_date")
    private LocalDate lastReminderDate;

    // Many-to-One Beziehung zum Subscription
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @PrePersist
    protected void onCreate() {
        createdDate = LocalDateTime.now();
        if (status == null) {
            status = DueStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedDate = LocalDateTime.now();
    }

    public DueSchedule() {
    }

    public DueSchedule(LocalDate dueDate, BigDecimal amount, LocalDate periodStart,
                       LocalDate periodEnd, Subscription subscription) {
        this.dueDate = dueDate;
        this.amount = amount;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.subscription = subscription;
        this.status = DueStatus.PENDING;
    }

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

    public Subscription getSubscription() {
        return subscription;
    }

    public void setSubscription(Subscription subscription) {
        this.subscription = subscription;
    }

    // Hilfsmethoden
    public boolean isPaid() {
        return status == DueStatus.PAID;
    }

    public boolean isOverdue() {
        return status == DueStatus.PENDING && dueDate.isBefore(LocalDate.now());
    }

    public void markAsPaid(BigDecimal paidAmount, String paymentMethod, String paymentReference) {
        this.status = DueStatus.PAID;
        this.paidAmount = paidAmount;
        this.paidDate = LocalDate.now();
        this.paymentMethod = paymentMethod;
        this.paymentReference = paymentReference;
    }

    @Override
    public String toString() {
        return "DueSchedule{" +
                "id=" + id +
                ", dueNumber='" + dueNumber + '\'' +
                ", dueDate=" + dueDate +
                ", amount=" + amount +
                ", periodStart=" + periodStart +
                ", periodEnd=" + periodEnd +
                ", status=" + status +
                ", paidDate=" + paidDate +
                ", paidAmount=" + paidAmount +
                ", subscription=" + (subscription != null ? subscription.getId() : null) +
                '}';
    }

    public Boolean isReminderSent() {
        return false;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }
}