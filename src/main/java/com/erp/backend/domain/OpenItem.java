package com.erp.backend.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Repräsentiert einen offenen Posten im ERP-System.
 *
 * Zentrale Entität für Zahlungsmanagement:
 * - Verwaltet offene Beträge aus Rechnungen
 * - Tracks Zahlungen und Zahlungsstatus
 * - Ermöglicht Teilzahlungen
 * - Berechnet ausstehende Beträge
 */
@Entity
@Table(name = "open_items")
@SQLDelete(sql = "UPDATE open_items SET deleted = true WHERE id = ?")
@SQLRestriction("deleted = false")
public class OpenItem {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    /** Referenz zur ursprünglichen Rechnung */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    /** Beschreibung des offenen Postens */
    @Column(nullable = false)
    private String description;

    /** Ursprünglicher Rechnungsbetrag */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    /** Fälligkeitsdatum */
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    /** Soft-Delete Flag */
    @Column(nullable = false)
    private boolean deleted = false;

    /** Status des offenen Postens */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OpenItemStatus status = OpenItemStatus.OPEN;

    /** Bereits gezahlter Betrag */
    @Column(name = "paid_amount", precision = 10, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    /** Datum der (letzten) Zahlung */
    @Column(name = "paid_date")
    private LocalDate paidDate;

    /** Zahlungsmethode */
    @Column(name = "payment_method")
    private String paymentMethod;

    /** Zahlungsreferenz (z.B. Überweisungsreferenz) */
    @Column(name = "payment_reference")
    private String paymentReference;

    /** Zeitstempel der Erstellung */
    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    /** Zeitstempel der letzten Aktualisierung */
    @Column(name = "updated_date")
    private LocalDateTime updatedDate;

    /** Notizen zum offenen Posten */
    @Column(name = "notes", length = 500)
    private String notes;

    /** Datum der letzten Mahnung */
    @Column(name = "last_reminder_date")
    private LocalDate lastReminderDate;

    /** Anzahl gesendeter Mahnungen */
    @Column(name = "reminder_count")
    private Integer reminderCount = 0;

    // ===================================================
    // KORRIGIERT: Subscription Beziehung mit direkter ID
    // ===================================================

    /** WICHTIG: Direkte subscription_id Spalte für NOT NULL Constraint */
    @Column(name = "subscription_id", nullable = false)
    private UUID subscriptionId;

    /** Subscription-Beziehung (optional für Lazy Loading) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", insertable = false, updatable = false)
    private Subscription subscription;

    /**
     * Verknüpfung zum Vorgang, der diesen OpenItem erstellt hat
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vorgang_id")
    private Vorgang vorgang;

    // ===================================================
    // Getter und Setter - KORRIGIERT
    // ===================================================

    public Vorgang getVorgang() { return vorgang; }
    public void setVorgang(Vorgang vorgang) { this.vorgang = vorgang; }

    public UUID getSubscriptionId() { return subscriptionId; }

    /** KRITISCH: Korrekte Implementierung der setSubscriptionId */
    public void setSubscriptionId(UUID subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public Subscription getSubscription() { return subscription; }

    public void setSubscription(Subscription subscription) {
        this.subscription = subscription;
        // Automatisch subscription_id setzen bei Beziehung
        if (subscription != null) {
            this.subscriptionId = subscription.getId();
        }
    }

    // ===================================================
    // Enums
    // ===================================================

    public enum OpenItemStatus {
        OPEN("Offen"),
        PARTIALLY_PAID("Teilweise bezahlt"),
        PAID("Bezahlt"),
        CANCELLED("Storniert"),
        OVERDUE("Überfällig");

        private final String displayName;
        OpenItemStatus(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }

    // ===================================================
    // Konstruktoren
    // ===================================================

    public OpenItem() {
        this.createdDate = LocalDateTime.now();
    }

    public OpenItem(Invoice invoice, String description, BigDecimal amount, LocalDate dueDate) {
        this();
        this.invoice = invoice;
        this.description = description;
        this.amount = amount;
        this.dueDate = dueDate;
        this.status = OpenItemStatus.OPEN;
        this.paidAmount = BigDecimal.ZERO;
    }

    @PrePersist
    protected void onCreate() {
        this.createdDate = LocalDateTime.now();
        if (this.paidAmount == null) {
            this.paidAmount = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedDate = LocalDateTime.now();
    }

    // ===================================================
    // Business-Methoden
    // ===================================================

    /**
     * Markiert eine Zahlung auf diesen offenen Posten
     */
    public void recordPayment(BigDecimal paymentAmount, String paymentMethod, String paymentReference) {
        if (paymentAmount == null || paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be positive");
        }

        if (this.status == OpenItemStatus.PAID || this.status == OpenItemStatus.CANCELLED) {
            throw new IllegalStateException("Cannot record payment on " + this.status + " item");
        }

        this.paidAmount = this.paidAmount.add(paymentAmount);
        this.paymentMethod = paymentMethod;
        this.paymentReference = paymentReference;
        this.paidDate = LocalDate.now();

        updateStatus();
    }

    /**
     * Storniert eine Zahlung (bei Rückbuchungen)
     */
    public void reversePayment(BigDecimal reversalAmount) {
        if (reversalAmount == null || reversalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Reversal amount must be positive");
        }

        if (this.paidAmount.compareTo(reversalAmount) < 0) {
            throw new IllegalArgumentException("Cannot reverse more than paid amount");
        }

        this.paidAmount = this.paidAmount.subtract(reversalAmount);
        updateStatus();

        if (this.paidAmount.compareTo(BigDecimal.ZERO) == 0) {
            this.paidDate = null;
            this.paymentMethod = null;
            this.paymentReference = null;
        }
    }

    /**
     * Storniert den kompletten offenen Posten
     */
    public void cancel() {
        this.status = OpenItemStatus.CANCELLED;
    }

    /**
     * Aktualisiert den Status basierend auf Zahlungen
     */
    private void updateStatus() {
        if (this.status == OpenItemStatus.CANCELLED) {
            return; // Stornierte Items bleiben storniert
        }

        if (this.paidAmount.compareTo(this.amount) >= 0) {
            this.status = OpenItemStatus.PAID;
        } else if (this.paidAmount.compareTo(BigDecimal.ZERO) > 0) {
            this.status = OpenItemStatus.PARTIALLY_PAID;
        } else {
            this.status = isOverdue() ? OpenItemStatus.OVERDUE : OpenItemStatus.OPEN;
        }
    }

    /**
     * Berechnet den ausstehenden Betrag
     */
    public BigDecimal getOutstandingAmount() {
        return amount.subtract(paidAmount != null ? paidAmount : BigDecimal.ZERO);
    }

    /**
     * Prüft, ob der offene Posten überfällig ist
     */
    public boolean isOverdue() {
        return status != OpenItemStatus.PAID &&
                status != OpenItemStatus.CANCELLED &&
                dueDate.isBefore(LocalDate.now());
    }

    /**
     * Prüft, ob der offene Posten vollständig bezahlt ist
     */
    public boolean isPaid() {
        return status == OpenItemStatus.PAID;
    }

    /**
     * Prüft, ob der offene Posten teilweise bezahlt ist
     */
    public boolean isPartiallyPaid() {
        return status == OpenItemStatus.PARTIALLY_PAID;
    }

    /**
     * Prüft, ob der offene Posten noch offen ist (nicht bezahlt)
     */
    public boolean isOpen() {
        return status == OpenItemStatus.OPEN || status == OpenItemStatus.OVERDUE;
    }

    /**
     * Erhöht die Mahnungsanzahl
     */
    public void addReminder() {
        this.reminderCount++;
        this.lastReminderDate = LocalDate.now();

        // Status auf überfällig setzen, falls noch nicht geschehen
        if (isOverdue() && status == OpenItemStatus.OPEN) {
            this.status = OpenItemStatus.OVERDUE;
        }
    }

    // ===================================================
    // Getter & Setter
    // ===================================================

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Invoice getInvoice() { return invoice; }
    public void setInvoice(Invoice invoice) { this.invoice = invoice; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }

    public OpenItemStatus getStatus() { return status; }
    public void setStatus(OpenItemStatus status) { this.status = status; }

    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }

    public LocalDate getPaidDate() { return paidDate; }
    public void setPaidDate(LocalDate paidDate) { this.paidDate = paidDate; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getPaymentReference() { return paymentReference; }
    public void setPaymentReference(String paymentReference) { this.paymentReference = paymentReference; }

    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }

    public LocalDateTime getUpdatedDate() { return updatedDate; }
    public void setUpdatedDate(LocalDateTime updatedDate) { this.updatedDate = updatedDate; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDate getLastReminderDate() { return lastReminderDate; }
    public void setLastReminderDate(LocalDate lastReminderDate) { this.lastReminderDate = lastReminderDate; }

    public Integer getReminderCount() { return reminderCount; }
    public void setReminderCount(Integer reminderCount) { this.reminderCount = reminderCount; }

    @Override
    public String toString() {
        return "OpenItem{" +
                "id=" + id +
                ", invoice=" + (invoice != null ? invoice.getInvoiceNumber() : null) +
                ", description='" + description + '\'' +
                ", amount=" + amount +
                ", paidAmount=" + paidAmount +
                ", outstandingAmount=" + getOutstandingAmount() +
                ", dueDate=" + dueDate +
                ", status=" + status +
                ", subscriptionId=" + subscriptionId +
                ", overdue=" + isOverdue() +
                ", reminderCount=" + reminderCount +
                '}';
    }
}