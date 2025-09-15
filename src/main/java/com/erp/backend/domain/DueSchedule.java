package com.erp.backend.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Repräsentiert eine einzelne Fälligkeit (DueSchedule) eines Abonnements.
 *
 * Verwaltet nur die Zeitplanung und den Status für Fälligkeiten.
 * Preise werden zur Laufzeit aus der Subscription und den zugehörigen Produkten berechnet.
 */
@Entity
@Table(name = "due_schedules")
@SQLDelete(sql = "UPDATE due_schedules SET deleted = true WHERE id = ?")
@SQLRestriction("deleted = false")
public class DueSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Eindeutige Nummer der Fälligkeit
     */
    @Column(name = "due_number", unique = true)
    private String dueNumber;

    /**
     * Datum, an dem die Fälligkeit fällig ist
     */
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    /**
     * Zeitraum, den diese Fälligkeit abdeckt
     */
    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end")
    private LocalDate periodEnd;

    /**
     * Status der Fälligkeit (ACTIVE, PAUSED, SUSPENDED, COMPLETED)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DueStatus status;

    // ===== NEU: Rechnungsreferenzen =====
    /**
     * Referenz zur erstellten Rechnung (nach Abrechnung)
     */
    @Column(name = "invoice_id")
    private UUID invoiceId;

    /**
     * Batch-ID des Rechnungslaufs
     */
    @Column(name = "invoice_batch_id")
    private String invoiceBatchId;

    /**
     * Datum der Abrechnung
     */
    @Column(name = "invoiced_date")
    private LocalDate invoicedDate;
    // =====================================

    /**
     * Soft-Delete Flag
     */
    @Column(nullable = false)
    private boolean deleted = false;

    /**
     * Zeitstempel der Erstellung
     */
    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    /**
     * Zeitstempel der letzten Aktualisierung
     */
    @Column(name = "updated_date")
    private LocalDateTime updatedDate;

    @Column(name = "notes", length = 500)
    private String notes;

    /**
     * Zugehöriges Abonnement
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    // === Lifecycle Callbacks ===

    @PrePersist
    protected void onCreate() {
        createdDate = LocalDateTime.now();
        if (status == null) {
            status = DueStatus.ACTIVE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedDate = LocalDateTime.now();
    }

    // === Konstruktoren ===

    public DueSchedule() {}

    public DueSchedule(LocalDate dueDate, LocalDate periodStart,
                       LocalDate periodEnd, Subscription subscription) {
        this.dueDate = dueDate;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.subscription = subscription;
        this.status = DueStatus.ACTIVE;
    }

    // === Business Methoden ===

    /**
     * Markiert die Fälligkeit als abgeschlossen
     */
    public void markAsCompleted() {
        this.status = DueStatus.COMPLETED;
        this.invoicedDate = LocalDate.now();
    }

    /**
     * Markiert die Fälligkeit als abgerechnet mit Rechnungsreferenz
     */
    public void markAsInvoiced(UUID invoiceId, String batchId) {
        this.status = DueStatus.COMPLETED;
        this.invoiceId = invoiceId;
        this.invoiceBatchId = batchId;
        this.invoicedDate = LocalDate.now();
    }

    /**
     * Setzt die Fälligkeit auf aktiv zurück und entfernt Rechnungsreferenzen
     */
    public void revertToActive() {
        this.status = DueStatus.ACTIVE;
        this.invoiceId = null;
        this.invoiceBatchId = null;
        this.invoicedDate = null;
    }

    public void pause() {
        if (status == DueStatus.ACTIVE) {
            this.status = DueStatus.PAUSED;
        }
    }

    public void activate() {
        if (status == DueStatus.PAUSED || status == DueStatus.SUSPENDED) {
            this.status = DueStatus.ACTIVE;
        }
    }

    public void suspend() {
        if (status != DueStatus.COMPLETED) {
            this.status = DueStatus.SUSPENDED;
        }
    }

    // === Hilfsmethoden ===

    public boolean isCompleted() {
        return status == DueStatus.COMPLETED;
    }

    public boolean isActive() {
        return status == DueStatus.ACTIVE;
    }

    public boolean isPaused() {
        return status == DueStatus.PAUSED;
    }

    public boolean isSuspended() {
        return status == DueStatus.SUSPENDED;
    }

    public boolean isOverdue() {
        return status == DueStatus.ACTIVE && dueDate.isBefore(LocalDate.now());
    }

    public boolean canBeProcessed() {
        return status == DueStatus.ACTIVE;
    }

    public boolean isInvoiced() {
        return invoiceId != null;
    }

    public boolean hasInvoice() {
        return invoiceId != null && status == DueStatus.COMPLETED;
    }

    // === Getter & Setter ===

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getDueNumber() { return dueNumber; }
    public void setDueNumber(String dueNumber) { this.dueNumber = dueNumber; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public LocalDate getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDate periodStart) { this.periodStart = periodStart; }

    public LocalDate getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(LocalDate periodEnd) { this.periodEnd = periodEnd; }

    public DueStatus getStatus() { return status; }
    public void setStatus(DueStatus status) { this.status = status; }

    public UUID getInvoiceId() { return invoiceId; }
    public void setInvoiceId(UUID invoiceId) { this.invoiceId = invoiceId; }

    public String getInvoiceBatchId() { return invoiceBatchId; }
    public void setInvoiceBatchId(String invoiceBatchId) { this.invoiceBatchId = invoiceBatchId; }

    public LocalDate getInvoicedDate() { return invoicedDate; }
    public void setInvoicedDate(LocalDate invoicedDate) { this.invoicedDate = invoicedDate; }

    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }

    public LocalDateTime getUpdatedDate() { return updatedDate; }
    public void setUpdatedDate(LocalDateTime updatedDate) { this.updatedDate = updatedDate; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Subscription getSubscription() { return subscription; }
    public void setSubscription(Subscription subscription) { this.subscription = subscription; }

    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }

    // === toString ===
    @Override
    public String toString() {
        return "DueSchedule{" +
                "id=" + id +
                ", dueNumber='" + dueNumber + '\'' +
                ", dueDate=" + dueDate +
                ", periodStart=" + periodStart +
                ", periodEnd=" + periodEnd +
                ", status=" + status +
                ", invoiceId=" + invoiceId +
                ", batchId='" + invoiceBatchId + '\'' +
                ", invoicedDate=" + invoicedDate +
                ", subscription=" + (subscription != null ? subscription.getId() : null) +
                '}';
    }
}