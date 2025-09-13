package com.erp.backend.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Repräsentiert eine einzelne Fälligkeit (DueSchedule) eines Abonnements.
 *
 * Verwaltet nur die Zeitplanung und den Status für den Rechnungslauf.
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

    // === Felder für Invoice Management ===

    /**
     * Referenz zur Rechnung, falls diese Fälligkeit bereits abgerechnet wurde
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

    /**
     * Datum, an dem diese Fälligkeit in Rechnung gestellt wurde
     */
    @Column(name = "invoiced_date")
    private LocalDate invoicedDate;

    /**
     * Referenz zum InvoiceItem, das aus dieser DueSchedule erzeugt wurde
     */
    @OneToOne(mappedBy = "dueSchedule", fetch = FetchType.LAZY)
    private InvoiceItem invoiceItem;

    /**
     * Flag, ob diese Fälligkeit bereits in einem Rechnungslauf berücksichtigt wurde
     */
    @Column(name = "processed_for_invoicing", nullable = false)
    private boolean processedForInvoicing = false;

    /**
     * Batch-ID des Rechnungslaufs
     */
    @Column(name = "invoice_batch_id")
    private String invoiceBatchId;

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
     * Markiert die Fälligkeit als in Rechnung gestellt
     */
    public void markAsInvoiced(Invoice invoice, String batchId) {
        this.invoice = invoice;
        this.invoicedDate = LocalDate.now();
        this.processedForInvoicing = true;
        this.invoiceBatchId = batchId;
        this.status = DueStatus.COMPLETED;
        this.notes = "Abgerechnet mit Rechnung " + invoice.getInvoiceNumber() +
                " am " + invoice.getInvoiceDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
    }

    /**
     * Setzt die Fälligkeit zurück, z.B. bei Storno einer Rechnung
     */
    public void revertInvoicing() {
        this.invoice = null;
        this.invoicedDate = null;
        this.processedForInvoicing = false;
        this.invoiceBatchId = null;
        this.status = DueStatus.ACTIVE;
        this.notes = null;
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
        return status == DueStatus.COMPLETED || invoice != null;
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

    public boolean canBeInvoiced() {
        return status == DueStatus.ACTIVE && !processedForInvoicing;
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

    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }

    public LocalDateTime getUpdatedDate() { return updatedDate; }
    public void setUpdatedDate(LocalDateTime updatedDate) { this.updatedDate = updatedDate; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Subscription getSubscription() { return subscription; }
    public void setSubscription(Subscription subscription) { this.subscription = subscription; }

    public Invoice getInvoice() { return invoice; }
    public void setInvoice(Invoice invoice) { this.invoice = invoice; }

    public LocalDate getInvoicedDate() { return invoicedDate; }
    public void setInvoicedDate(LocalDate invoicedDate) { this.invoicedDate = invoicedDate; }

    public InvoiceItem getInvoiceItem() { return invoiceItem; }
    public void setInvoiceItem(InvoiceItem invoiceItem) { this.invoiceItem = invoiceItem; }

    public boolean isProcessedForInvoicing() { return processedForInvoicing; }
    public void setProcessedForInvoicing(boolean processedForInvoicing) { this.processedForInvoicing = processedForInvoicing; }

    public String getInvoiceBatchId() { return invoiceBatchId; }
    public void setInvoiceBatchId(String invoiceBatchId) { this.invoiceBatchId = invoiceBatchId; }

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
                ", invoiced=" + isCompleted() +
                ", invoice=" + (invoice != null ? invoice.getInvoiceNumber() : null) +
                ", subscription=" + (subscription != null ? subscription.getId() : null) +
                '}';
    }
}
