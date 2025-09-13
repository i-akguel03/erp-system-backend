package com.erp.backend.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "invoice_items")
public class InvoiceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "description", nullable = false, length = 500)
    private String description;

    @Column(name = "quantity", precision = 10, scale = 3, nullable = false)
    private BigDecimal quantity;

    @Column(name = "unit_price", precision = 10, scale = 2, nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "line_total", precision = 10, scale = 2, nullable = false)
    private BigDecimal lineTotal;

    @Column(name = "tax_rate", precision = 5, scale = 2)
    private BigDecimal taxRate;

    @Column(name = "tax_amount", precision = 10, scale = 2)
    private BigDecimal taxAmount;

    @Column(name = "discount_amount", precision = 10, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Beziehung zur Rechnung
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    // === NEUE FELDER FÜR DUESCHEDULE REFERENZ ===

    /**
     * Referenz zur ursprünglichen DueSchedule, falls dieses Item daraus erstellt wurde
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "due_schedule_id")
    private DueSchedule dueSchedule;

    /**
     * Typ des InvoiceItems
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false)
    private InvoiceItemType itemType = InvoiceItemType.SERVICE;

    /**
     * Periode Start (für Abonnement-Items)
     */
    @Column(name = "period_start")
    private java.time.LocalDate periodStart;

    /**
     * Periode Ende (für Abonnement-Items)
     */
    @Column(name = "period_end")
    private java.time.LocalDate periodEnd;

    /**
     * Produkt/Service-Referenz
     */
    @Column(name = "product_code")
    private String productCode;

    @Column(name = "product_name")
    private String productName;

    // Enum für Item-Typen
    public enum InvoiceItemType {
        SERVICE("Dienstleistung"),
        PRODUCT("Produkt"),
        SUBSCRIPTION("Abonnement"),
        DISCOUNT("Rabatt"),
        FEE("Gebühr");

        private final String displayName;

        InvoiceItemType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    // Konstruktoren
    public InvoiceItem() {
        this.createdAt = LocalDateTime.now();
        this.quantity = BigDecimal.ONE;
    }

    public InvoiceItem(String description, BigDecimal quantity, BigDecimal unitPrice) {
        this();
        this.description = description;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        calculateLineTotal();
    }

    // Lifecycle-Methoden
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        calculateLineTotal();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        calculateLineTotal();
    }

    // Business-Methoden
    public void calculateLineTotal() {
        BigDecimal gross = quantity.multiply(unitPrice);

        if (discountAmount != null) {
            gross = gross.subtract(discountAmount);
        }

        this.lineTotal = gross;

        // Berechne Steuer falls angegeben
        if (taxRate != null && taxRate.compareTo(BigDecimal.ZERO) > 0) {
            this.taxAmount = lineTotal.multiply(taxRate.divide(BigDecimal.valueOf(100)));
        }
    }

    /**
     * Erstellt ein InvoiceItem basierend auf einer DueSchedule
     */
    public static InvoiceItem fromDueSchedule(DueSchedule dueSchedule, Invoice invoice) {
        InvoiceItem item = new InvoiceItem();
        item.setInvoice(invoice);
        item.setDueSchedule(dueSchedule);
        item.setItemType(InvoiceItemType.SUBSCRIPTION);

        // Setze Periode
        item.setPeriodStart(dueSchedule.getPeriodStart());
        item.setPeriodEnd(dueSchedule.getPeriodEnd());

        // Erstelle Beschreibung
        String description = createDescriptionFromDueSchedule(dueSchedule);
        item.setDescription(description);

        // Setze Beträge
        item.setQuantity(BigDecimal.ONE);
        item.setUnitPrice(dueSchedule.getAmount());

        // Produkt-Informationen falls verfügbar
        if (dueSchedule.getSubscription() != null &&
                dueSchedule.getSubscription().getProduct() != null) {
            Product product = dueSchedule.getSubscription().getProduct();
            item.setProductCode(product.getProductNumber());
            item.setProductName(product.getName());
        }

        item.calculateLineTotal();
        return item;
    }

    private static String createDescriptionFromDueSchedule(DueSchedule dueSchedule) {
        StringBuilder desc = new StringBuilder();

        // Produkt-/Service-Name
        if (dueSchedule.getSubscription() != null) {
            Subscription subscription = dueSchedule.getSubscription();
            if (subscription.getProduct() != null) {
                desc.append(subscription.getProduct().getName());
            } else {
                desc.append("Abonnement");
            }

            // Periode hinzufügen
            if (dueSchedule.getPeriodStart() != null) {
                desc.append(" - Zeitraum: ");
                desc.append(dueSchedule.getPeriodStart().format(
                        java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy")));

                if (dueSchedule.getPeriodEnd() != null) {
                    desc.append(" bis ");
                    desc.append(dueSchedule.getPeriodEnd().format(
                            java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy")));
                }
            }

            // Subscription-Details
            if (subscription.getSubscriptionNumber() != null) {
                desc.append(" (Abo-Nr.: ").append(subscription.getSubscriptionNumber()).append(")");
            }
        }

        return desc.toString();
    }

    // === GETTER & SETTER ===

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }

    public void setLineTotal(BigDecimal lineTotal) {
        this.lineTotal = lineTotal;
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

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
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

    public Invoice getInvoice() {
        return invoice;
    }

    public void setInvoice(Invoice invoice) {
        this.invoice = invoice;
    }

    // === NEUE GETTER & SETTER ===

    public DueSchedule getDueSchedule() {
        return dueSchedule;
    }

    public void setDueSchedule(DueSchedule dueSchedule) {
        this.dueSchedule = dueSchedule;
    }

    public InvoiceItemType getItemType() {
        return itemType;
    }

    public void setItemType(InvoiceItemType itemType) {
        this.itemType = itemType;
    }

    public java.time.LocalDate getPeriodStart() {
        return periodStart;
    }

    public void setPeriodStart(java.time.LocalDate periodStart) {
        this.periodStart = periodStart;
    }

    public java.time.LocalDate getPeriodEnd() {
        return periodEnd;
    }

    public void setPeriodEnd(java.time.LocalDate periodEnd) {
        this.periodEnd = periodEnd;
    }

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    @Override
    public String toString() {
        return "InvoiceItem{" +
                "id=" + id +
                ", description='" + description + '\'' +
                ", quantity=" + quantity +
                ", unitPrice=" + unitPrice +
                ", lineTotal=" + lineTotal +
                ", itemType=" + itemType +
                ", periodStart=" + periodStart +
                ", periodEnd=" + periodEnd +
                ", dueSchedule=" + (dueSchedule != null ? dueSchedule.getDueNumber() : null) +
                '}';
    }
}