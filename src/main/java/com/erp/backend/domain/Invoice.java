package com.erp.backend.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Repräsentiert eine Rechnung im ERP-System.
 *
 * Reine Rechnungsinformationen:
 * - Keine Zahlungsinformationen (werden in OpenItem verwaltet)
 * - Keine direkte Verbindung zu DueSchedule
 * - Fokus auf Rechnungsstruktur und -inhalt
 */
@Entity
@Table(name = "invoices")
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    /** Eindeutige Rechnungsnummer, z. B. "INV-2025-0001" */
    @Column(name = "invoice_number", unique = true, nullable = false)
    private String invoiceNumber;

    /** Datum der Rechnungsausstellung */
    @Column(name = "invoice_date", nullable = false)
    private LocalDate invoiceDate;

    /** Zahlungsziel */
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    /** Status der Rechnung */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private InvoiceStatus status;

    /** Zwischensumme aller Positionen vor Steuern */
    @Column(name = "subtotal", precision = 10, scale = 2, nullable = false)
    private BigDecimal subtotal;

    /** MwSt. Prozentwert */
    @Column(name = "tax_rate", precision = 5, scale = 2)
    private BigDecimal taxRate;

    /** Steuerbetrag */
    @Column(name = "tax_amount", precision = 10, scale = 2)
    private BigDecimal taxAmount;

    /** Endbetrag inkl. Steuern */
    @Column(name = "total_amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    /** Rabattbetrag, falls vorhanden */
    @Column(name = "discount_amount", precision = 10, scale = 2)
    private BigDecimal discountAmount;

    /** Notizen zur Rechnung */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    /** Zahlungsbedingungen */
    @Column(name = "payment_terms")
    private String paymentTerms;

    /** Zeitstempel Erstellung */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /** Zeitstempel letzte Änderung */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** Batch-ID, falls Rechnung im Stapel erstellt wurde */
    @Column(name = "invoice_batch_id")
    private String invoiceBatchId;

    /** Typ der Rechnung (manuell / automatisch) */
    @Enumerated(EnumType.STRING)
    @Column(name = "invoice_type", nullable = false)
    private InvoiceType invoiceType = InvoiceType.MANUAL;

    /** Originalrechnung bei Gutschriften */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_invoice_id")
    private Invoice originalInvoice;

    /** Gutschriften, die auf dieser Rechnung basieren */
    @OneToMany(mappedBy = "originalInvoice", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Invoice> creditNotes = new ArrayList<>();

    // ===================================================
    // Beziehungen - KORRIGIERT
    // ===================================================

    /** Kunde der Rechnung */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    /** WICHTIG: Direkte subscription_id Spalte für bessere Performance und Abfragen */
    @Column(name = "subscription_id")
    private UUID subscriptionId;

    /** Subscription-Beziehung (optional für Lazy Loading) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", insertable = false, updatable = false)
    private Subscription subscription;

    /** Rechnungsadresse (kann von Kundenadresse abweichen) */
    @ManyToOne
    @JoinColumn(name = "billing_address_id")
    private Address billingAddress;

    /** Positionen der Rechnung */
    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<InvoiceItem> invoiceItems = new ArrayList<>();

    /** Offene Posten, die aus der Rechnung abgeleitet werden */
    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OpenItem> openItems = new ArrayList<>();

    /** Verknüpfung zum Vorgang, der diese Rechnung erstellt hat */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vorgang_id")
    private Vorgang vorgang;

    // ===================================================
    // Enums
    // ===================================================

    public enum InvoiceStatus {
        DRAFT("Entwurf"),
        ACTIVE("Aktiv"),
        SENT("Versendet"),
        CANCELLED("Storniert");

        private final String displayName;
        InvoiceStatus(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }

    public enum InvoiceType {
        MANUAL("Manuell erstellt"),
        AUTO_GENERATED("Automatisch generiert"),
        RECURRING("Wiederkehrend"),
        CREDIT_NOTE("Gutschrift");

        private final String displayName;
        InvoiceType(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }

    // ===================================================
    // Konstruktoren
    // ===================================================

    public Invoice() {
        this.createdAt = LocalDateTime.now();
        this.status = InvoiceStatus.DRAFT;
        this.invoiceType = InvoiceType.MANUAL;
    }

    public Invoice(String invoiceNumber, Customer customer, LocalDate invoiceDate, LocalDate dueDate) {
        this();
        this.invoiceNumber = invoiceNumber;
        this.customer = customer;
        this.invoiceDate = invoiceDate;
        this.dueDate = dueDate;
    }

    @PrePersist
    protected void onCreate() { this.createdAt = LocalDateTime.now(); }

    @PreUpdate
    protected void onUpdate() { this.updatedAt = LocalDateTime.now(); }

    // ===================================================
    // Business-Methoden
    // ===================================================

    /** Berechnet Subtotal, Steuern und Gesamtbetrag */
    public void calculateTotals() {
        BigDecimal subtotalCalc = invoiceItems.stream()
                .map(item -> item.getLineTotal() != null ? item.getLineTotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (discountAmount != null) subtotalCalc = subtotalCalc.subtract(discountAmount);
        if (subtotalCalc.compareTo(BigDecimal.ZERO) < 0) subtotalCalc = BigDecimal.ZERO;

        BigDecimal taxCalc = BigDecimal.ZERO;
        if (taxRate != null && taxRate.compareTo(BigDecimal.ZERO) > 0) {
            taxCalc = subtotalCalc.multiply(taxRate.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
        }

        this.subtotal = subtotalCalc;
        this.taxAmount = taxCalc;
        this.totalAmount = subtotalCalc.add(taxCalc);
    }

    /** Fügt eine Rechnungsposition hinzu */
    public void addInvoiceItem(InvoiceItem item) {
        if (item == null) throw new IllegalArgumentException("InvoiceItem darf nicht null sein");

        item.setInvoice(this);

        if (item.getQuantity() == null) item.setQuantity(BigDecimal.ONE);
        if (item.getUnitPrice() == null) item.setUnitPrice(BigDecimal.ZERO);

        invoiceItems.add(item);
        calculateTotals();
    }

    /** Entfernt eine Rechnungsposition */
    public void removeInvoiceItem(InvoiceItem item) {
        invoiceItems.remove(item);
        item.setInvoice(null);
        calculateTotals();
    }

    /** Erstellt eine Gutschriftrechnung */
    public Invoice createCreditNote() {
        Invoice creditNote = new Invoice();
        creditNote.setOriginalInvoice(this);
        creditNote.setCustomer(this.customer);
        creditNote.setBillingAddress(this.billingAddress);
        creditNote.setInvoiceDate(LocalDate.now());
        creditNote.setDueDate(LocalDate.now());
        creditNote.setStatus(InvoiceStatus.DRAFT);
        creditNote.setInvoiceType(InvoiceType.CREDIT_NOTE);

        for (InvoiceItem originalItem : this.invoiceItems) {
            InvoiceItem creditItem = new InvoiceItem();
            creditItem.setDescription("Gutschrift: " + originalItem.getDescription());
            creditItem.setQuantity(originalItem.getQuantity().negate());
            creditItem.setUnitPrice(originalItem.getUnitPrice());
            creditNote.addInvoiceItem(creditItem);
        }

        this.creditNotes.add(creditNote);
        return creditNote;
    }

    // ===================================================
    // Getter & Setter
    // ===================================================

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }

    public LocalDate getInvoiceDate() { return invoiceDate; }
    public void setInvoiceDate(LocalDate invoiceDate) { this.invoiceDate = invoiceDate; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public InvoiceStatus getStatus() { return status; }
    public void setStatus(InvoiceStatus status) { this.status = status; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }

    public BigDecimal getTaxRate() { return taxRate; }
    public void setTaxRate(BigDecimal taxRate) { this.taxRate = taxRate; }

    public BigDecimal getTaxAmount() { return taxAmount; }
    public void setTaxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getPaymentTerms() { return paymentTerms; }
    public void setPaymentTerms(String paymentTerms) { this.paymentTerms = paymentTerms; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getInvoiceBatchId() { return invoiceBatchId; }
    public void setInvoiceBatchId(String invoiceBatchId) { this.invoiceBatchId = invoiceBatchId; }

    public InvoiceType getInvoiceType() { return invoiceType; }
    public void setInvoiceType(InvoiceType invoiceType) { this.invoiceType = invoiceType; }

    public Invoice getOriginalInvoice() { return originalInvoice; }
    public void setOriginalInvoice(Invoice originalInvoice) { this.originalInvoice = originalInvoice; }

    public List<Invoice> getCreditNotes() { return creditNotes; }
    public void setCreditNotes(List<Invoice> creditNotes) { this.creditNotes = creditNotes; }

    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }

    public Address getBillingAddress() { return billingAddress; }
    public void setBillingAddress(Address billingAddress) { this.billingAddress = billingAddress; }

    public List<InvoiceItem> getInvoiceItems() { return invoiceItems; }
    public void setInvoiceItems(List<InvoiceItem> invoiceItems) { this.invoiceItems = invoiceItems; }

    public List<OpenItem> getOpenItems() { return openItems; }
    public void setOpenItems(List<OpenItem> openItems) { this.openItems = openItems; }

    public Vorgang getVorgang() { return vorgang; }
    public void setVorgang(Vorgang vorgang) { this.vorgang = vorgang; }

    public UUID getSubscriptionId() { return subscriptionId; }
    public void setSubscriptionId(UUID subscriptionId) { this.subscriptionId = subscriptionId; }

    public Subscription getSubscription() { return subscription; }
    public void setSubscription(Subscription subscription) {
        this.subscription = subscription;
        // Automatisch subscription_id setzen bei Beziehung
        if (subscription != null) {
            this.subscriptionId = subscription.getId();
        }
    }

    @Override
    public String toString() {
        return "Invoice{" +
                "id=" + id +
                ", invoiceNumber='" + invoiceNumber + '\'' +
                ", invoiceDate=" + invoiceDate +
                ", dueDate=" + dueDate +
                ", status=" + status +
                ", invoiceType=" + invoiceType +
                ", totalAmount=" + totalAmount +
                ", subscriptionId=" + subscriptionId +
                ", customer=" + (customer != null ? customer.getCustomerNumber() : null) +
                ", batchId='" + invoiceBatchId + '\'' +
                '}';
    }
}