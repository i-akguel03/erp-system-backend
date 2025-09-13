package com.erp.backend.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "invoices")
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "invoice_number", unique = true, nullable = false)
    private String invoiceNumber;

    @Column(name = "invoice_date", nullable = false)
    private LocalDate invoiceDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private InvoiceStatus status;

    @Column(name = "subtotal", precision = 10, scale = 2, nullable = false)
    private BigDecimal subtotal;

    @Column(name = "tax_rate", precision = 5, scale = 2)
    private BigDecimal taxRate;

    @Column(name = "tax_amount", precision = 10, scale = 2)
    private BigDecimal taxAmount;

    @Column(name = "total_amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "discount_amount", precision = 10, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "payment_terms")
    private String paymentTerms;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // === NEUE FELDER FÜR BESSERES MANAGEMENT ===

    /**
     * Batch-ID des Rechnungslaufs, in dem diese Rechnung erstellt wurde
     */
    @Column(name = "invoice_batch_id")
    private String invoiceBatchId;

    /**
     * Typ der Rechnung (automatisch generiert vs. manuell erstellt)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "invoice_type", nullable = false)
    private InvoiceType invoiceType = InvoiceType.MANUAL;

    /**
     * Referenz zur ursprünglichen Rechnung bei Stornorechnungen
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_invoice_id")
    private Invoice originalInvoice;

    /**
     * Liste der Stornorechnungen zu dieser Rechnung
     */
    @OneToMany(mappedBy = "originalInvoice", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Invoice> creditNotes = new ArrayList<>();

    /**
     * Zahlungsinformationen
     */
    @Column(name = "paid_amount", precision = 10, scale = 2)
    private BigDecimal paidAmount;

    @Column(name = "paid_date")
    private LocalDate paidDate;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "payment_reference")
    private String paymentReference;

    /**
     * Datum der letzten Mahnung
     */
    @Column(name = "last_reminder_date")
    private LocalDate lastReminderDate;

    /**
     * Anzahl gesendeter Mahnungen
     */
    @Column(name = "reminder_count")
    private Integer reminderCount = 0;

    // Beziehung zum Kunden
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    // Rechnungsadresse (kann von Kundenadresse abweichen)
    @ManyToOne
    @JoinColumn(name = "billing_address_id")
    private Address billingAddress;

    // Rechnungspositionen
    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<InvoiceItem> invoiceItems = new ArrayList<>();

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OpenItem> openItems = new ArrayList<>();

    // Getter & Setter
    public List<OpenItem> getOpenItems() {
        return openItems;
    }

    public void setOpenItems(List<OpenItem> openItems) {
        this.openItems = openItems;
    }

    // === NEUE BEZIEHUNG ZU DUESCHEDULES ===

    /**
     * Liste der DueSchedules, die in dieser Rechnung abgerechnet wurden
     */
    @OneToMany(mappedBy = "invoice", fetch = FetchType.LAZY)
    private List<DueSchedule> dueSchedules = new ArrayList<>();

    // Enums
    public enum InvoiceStatus {
        DRAFT("Entwurf"),
        SENT("Versendet"),
        PAID("Bezahlt"),
        PARTIALLY_PAID("Teilweise bezahlt"),
        OVERDUE("Überfällig"),
        CANCELLED("Storniert"),
        CREDIT_NOTE("Gutschrift");

        private final String displayName;

        InvoiceStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum InvoiceType {
        MANUAL("Manuell erstellt"),
        AUTO_GENERATED("Automatisch generiert"),
        RECURRING("Wiederkehrend"),
        CREDIT_NOTE("Gutschrift");

        private final String displayName;

        InvoiceType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    // Konstruktoren
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

    // Lifecycle-Methoden
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // === ERWEITERTE BUSINESS-METHODEN ===

    public void calculateTotals() {
        // sichere Initialwerte
        BigDecimal subtotalCalc = invoiceItems.stream()
                .map(item -> item.getLineTotal() != null ? item.getLineTotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Rabatt berücksichtigen, falls gesetzt
        if (discountAmount != null) {
            subtotalCalc = subtotalCalc.subtract(discountAmount);
        }

        // sicherstellen, dass subtotal nicht negativ wird
        if (subtotalCalc.compareTo(BigDecimal.ZERO) < 0) {
            subtotalCalc = BigDecimal.ZERO;
        }

        // Steuer berechnen
        BigDecimal taxCalc = BigDecimal.ZERO;
        if (taxRate != null && taxRate.compareTo(BigDecimal.ZERO) > 0) {
            taxCalc = subtotalCalc.multiply(taxRate.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
        }

        // Felder setzen
        this.subtotal = subtotalCalc;
        this.taxAmount = taxCalc;
        this.totalAmount = subtotalCalc.add(taxCalc);
    }


    public void addInvoiceItem(InvoiceItem item) {
        if (item == null) {
            throw new IllegalArgumentException("InvoiceItem darf nicht null sein");
        }

        // sicherstellen, dass die Referenz gesetzt ist
        item.setInvoice(this);

        // Default-Werte absichern
        if (item.getQuantity() == null) {
            item.setQuantity(BigDecimal.ONE);
        }
        if (item.getUnitPrice() == null) {
            item.setUnitPrice(BigDecimal.ZERO);
        }

        invoiceItems.add(item);

        // Totale neu berechnen
        calculateTotals();
    }


    public void removeInvoiceItem(InvoiceItem item) {
        invoiceItems.remove(item);
        item.setInvoice(null);
        calculateTotals();
    }

    /**
     * Markiert die Rechnung als bezahlt
     */
    public void markAsPaid(BigDecimal paidAmount, String paymentMethod, String paymentReference) {
        this.paidAmount = paidAmount;
        this.paidDate = LocalDate.now();
        this.paymentMethod = paymentMethod;
        this.paymentReference = paymentReference;

        if (paidAmount.compareTo(totalAmount) >= 0) {
            this.status = InvoiceStatus.PAID;
        } else {
            this.status = InvoiceStatus.PARTIALLY_PAID;
        }

        // Markiere auch die zugehörigen DueSchedules als bezahlt
        for (DueSchedule dueSchedule : dueSchedules) {
            if (dueSchedule.getStatus() == DueStatus.INVOICED) {
                // Berechne anteiligen Betrag für diese DueSchedule
                BigDecimal proportionalAmount = calculateProportionalPayment(dueSchedule, paidAmount);
                dueSchedule.markAsPaid(proportionalAmount, paymentMethod, paymentReference);
            }
        }
    }

    /**
     * Berechnet anteilige Zahlung für eine DueSchedule
     */
    private BigDecimal calculateProportionalPayment(DueSchedule dueSchedule, BigDecimal totalPaidAmount) {
        if (totalAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal proportion = dueSchedule.getAmount().divide(totalAmount, 4, BigDecimal.ROUND_HALF_UP);
        return totalPaidAmount.multiply(proportion);
    }

    /**
     * Storniert die Rechnung und setzt DueSchedules zurück
     */
    public void cancel() {
        this.status = InvoiceStatus.CANCELLED;

        // Setze DueSchedules zurück
        for (DueSchedule dueSchedule : dueSchedules) {
            dueSchedule.revertInvoicing();
        }
        dueSchedules.clear();
    }

    /**
     * Erstellt eine Gutschrift zu dieser Rechnung
     */
    public Invoice createCreditNote() {
        Invoice creditNote = new Invoice();
        creditNote.setOriginalInvoice(this);
        creditNote.setCustomer(this.customer);
        creditNote.setBillingAddress(this.billingAddress);
        creditNote.setInvoiceDate(LocalDate.now());
        creditNote.setDueDate(LocalDate.now()); // Gutschriften sind sofort fällig
        creditNote.setStatus(InvoiceStatus.CREDIT_NOTE);
        creditNote.setInvoiceType(InvoiceType.CREDIT_NOTE);

        // Kopiere Items mit negativen Beträgen
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

    public boolean isOverdue() {
        return status != InvoiceStatus.PAID &&
                status != InvoiceStatus.CANCELLED &&
                status != InvoiceStatus.CREDIT_NOTE &&
                dueDate.isBefore(LocalDate.now());
    }

    public boolean isPaid() {
        return status == InvoiceStatus.PAID;
    }

    public boolean isPartiallyPaid() {
        return status == InvoiceStatus.PARTIALLY_PAID;
    }

    public BigDecimal getOutstandingAmount() {
        if (paidAmount == null) {
            return totalAmount;
        }
        return totalAmount.subtract(paidAmount);
    }

    // === GETTER & SETTER (Original + Neue) ===

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public LocalDate getInvoiceDate() {
        return invoiceDate;
    }

    public void setInvoiceDate(LocalDate invoiceDate) {
        this.invoiceDate = invoiceDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public InvoiceStatus getStatus() {
        return status;
    }

    public void setStatus(InvoiceStatus status) {
        this.status = status;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(BigDecimal taxRate) {
        this.taxRate = taxRate;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getPaymentTerms() {
        return paymentTerms;
    }

    public void setPaymentTerms(String paymentTerms) {
        this.paymentTerms = paymentTerms;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public Address getBillingAddress() {
        return billingAddress;
    }

    public void setBillingAddress(Address billingAddress) {
        this.billingAddress = billingAddress;
    }

    public List<InvoiceItem> getInvoiceItems() {
        return invoiceItems;
    }

    public void setInvoiceItems(List<InvoiceItem> invoiceItems) {
        this.invoiceItems = invoiceItems;
    }

    // === NEUE GETTER & SETTER ===

    public String getInvoiceBatchId() {
        return invoiceBatchId;
    }

    public void setInvoiceBatchId(String invoiceBatchId) {
        this.invoiceBatchId = invoiceBatchId;
    }

    public InvoiceType getInvoiceType() {
        return invoiceType;
    }

    public void setInvoiceType(InvoiceType invoiceType) {
        this.invoiceType = invoiceType;
    }

    public Invoice getOriginalInvoice() {
        return originalInvoice;
    }

    public void setOriginalInvoice(Invoice originalInvoice) {
        this.originalInvoice = originalInvoice;
    }

    public List<Invoice> getCreditNotes() {
        return creditNotes;
    }

    public void setCreditNotes(List<Invoice> creditNotes) {
        this.creditNotes = creditNotes;
    }

    public BigDecimal getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(BigDecimal paidAmount) {
        this.paidAmount = paidAmount;
    }

    public LocalDate getPaidDate() {
        return paidDate;
    }

    public void setPaidDate(LocalDate paidDate) {
        this.paidDate = paidDate;
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

    public LocalDate getLastReminderDate() {
        return lastReminderDate;
    }

    public void setLastReminderDate(LocalDate lastReminderDate) {
        this.lastReminderDate = lastReminderDate;
    }

    public Integer getReminderCount() {
        return reminderCount;
    }

    public void setReminderCount(Integer reminderCount) {
        this.reminderCount = reminderCount;
    }

    public List<DueSchedule> getDueSchedules() {
        return dueSchedules;
    }

    public void setDueSchedules(List<DueSchedule> dueSchedules) {
        this.dueSchedules = dueSchedules;
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
                ", paidAmount=" + paidAmount +
                ", outstandingAmount=" + getOutstandingAmount() +
                ", dueSchedulesCount=" + dueSchedules.size() +
                ", customer=" + (customer != null ? customer.getCustomerNumber() : null) +
                ", batchId='" + invoiceBatchId + '\'' +
                '}';
    }
}