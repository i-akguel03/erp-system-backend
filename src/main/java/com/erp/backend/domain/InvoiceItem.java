package com.erp.backend.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
        this.unitPrice = BigDecimal.ZERO;
        this.lineTotal = BigDecimal.ZERO;
    }

    public InvoiceItem(String description, BigDecimal quantity, BigDecimal unitPrice) {
        this();
        this.description = description;
        this.quantity = quantity != null ? quantity : BigDecimal.ONE;
        this.unitPrice = unitPrice != null ? unitPrice : BigDecimal.ZERO;
        calculateLineTotal();
    }

    // Lifecycle-Methoden
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.quantity == null) this.quantity = BigDecimal.ONE;
        if (this.unitPrice == null) this.unitPrice = BigDecimal.ZERO;
        calculateLineTotal();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        calculateLineTotal();
    }

    // Business-Methoden
    public void calculateLineTotal() {
        if (quantity == null || unitPrice == null) {
            this.lineTotal = BigDecimal.ZERO;
            this.taxAmount = BigDecimal.ZERO;
            return;
        }

        BigDecimal gross = quantity.multiply(unitPrice);

        if (discountAmount != null && discountAmount.compareTo(BigDecimal.ZERO) > 0) {
            gross = gross.subtract(discountAmount);
            if (gross.compareTo(BigDecimal.ZERO) < 0) {
                gross = BigDecimal.ZERO;
            }
        }

        this.lineTotal = gross;

        // Berechne Steuer falls angegeben
        if (taxRate != null && taxRate.compareTo(BigDecimal.ZERO) > 0) {
            this.taxAmount = lineTotal.multiply(taxRate.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
        } else {
            this.taxAmount = BigDecimal.ZERO;
        }
    }

    /**
     * Aktualisiert Preis und Menge und berechnet LineTotal neu
     */
    public void updatePriceAndQuantity(BigDecimal newUnitPrice, BigDecimal newQuantity) {
        this.unitPrice = newUnitPrice != null ? newUnitPrice : BigDecimal.ZERO;
        this.quantity = newQuantity != null ? newQuantity : BigDecimal.ONE;
        calculateLineTotal();
    }

    /**
     * Setzt einen Rabatt und berechnet LineTotal neu
     */
    public void applyDiscount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
        calculateLineTotal();
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
        calculateLineTotal(); // Automatisch neu berechnen
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
        calculateLineTotal(); // Automatisch neu berechnen
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
        calculateLineTotal(); // Automatisch neu berechnen
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
        calculateLineTotal(); // Automatisch neu berechnen
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
                ", taxAmount=" + taxAmount +
                ", itemType=" + itemType +
                ", periodStart=" + periodStart +
                ", periodEnd=" + periodEnd +
                '}';
    }
}