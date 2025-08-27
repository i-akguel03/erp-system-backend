package com.erp.backend.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Entity
@Table(name = "invoice_items")
public class InvoiceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "position", nullable = false)
    private Integer position;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "quantity", precision = 10, scale = 3, nullable = false)
    private BigDecimal quantity;

    @Column(name = "unit")
    private String unit; // z.B. "Stück", "kg", "m", "Std."

    @Column(name = "unit_price", precision = 10, scale = 2, nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "discount_percentage", precision = 5, scale = 2)
    private BigDecimal discountPercentage;

    @Column(name = "discount_amount", precision = 10, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "tax_rate", precision = 5, scale = 2)
    private BigDecimal taxRate;

    @Column(name = "line_total", precision = 10, scale = 2)
    private BigDecimal lineTotal;

    @Column(name = "notes")
    private String notes;

    // Beziehung zur Rechnung
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    // Optionale Beziehung zu einem Produkt/Service
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    // Konstruktoren
    public InvoiceItem() {
        this.quantity = BigDecimal.ONE;
        this.discountPercentage = BigDecimal.ZERO;
        this.taxRate = BigDecimal.ZERO;
    }

    public InvoiceItem(String description, BigDecimal quantity, String unit, BigDecimal unitPrice) {
        this();
        this.description = description;
        this.quantity = quantity;
        this.unit = unit;
        this.unitPrice = unitPrice;
        calculateLineTotal();
    }

    public InvoiceItem(Product product, BigDecimal quantity, BigDecimal unitPrice) {
        this();
        this.product = product;
        this.description = product != null ? product.getName() : "";
        this.quantity = quantity;
        this.unit = product != null ? product.getUnit() : null;
        this.unitPrice = unitPrice != null ? unitPrice : (product != null ? product.getPrice() : BigDecimal.ZERO);
        calculateLineTotal();
    }

    // Business-Methoden
    @PrePersist
    @PreUpdate
    protected void calculateLineTotal() {
        if (quantity == null || unitPrice == null) {
            this.lineTotal = BigDecimal.ZERO;
            return;
        }

        // Basis-Betrag: Menge × Einzelpreis
        BigDecimal baseAmount = quantity.multiply(unitPrice);

        // Rabatt anwenden
        BigDecimal afterDiscount = baseAmount;
        if (discountPercentage != null && discountPercentage.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal discountValue = baseAmount.multiply(discountPercentage.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
            afterDiscount = baseAmount.subtract(discountValue);
        } else if (discountAmount != null && discountAmount.compareTo(BigDecimal.ZERO) > 0) {
            afterDiscount = baseAmount.subtract(discountAmount);
        }

        // Endergebnis auf 2 Dezimalstellen runden
        this.lineTotal = afterDiscount.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getLineTotalWithTax() {
        if (lineTotal == null) {
            calculateLineTotal();
        }

        if (taxRate != null && taxRate.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal taxAmount = lineTotal.multiply(taxRate.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
            return lineTotal.add(taxAmount).setScale(2, RoundingMode.HALF_UP);
        }

        return lineTotal;
    }

    public BigDecimal getTaxAmount() {
        if (lineTotal == null) {
            calculateLineTotal();
        }

        if (taxRate != null && taxRate.compareTo(BigDecimal.ZERO) > 0) {
            return lineTotal.multiply(taxRate.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        return BigDecimal.ZERO;
    }

    public void updateFromProduct(Product product) {
        if (product != null) {
            this.product = product;
            this.description = product.getName();
            this.unit = product.getUnit();
            if (this.unitPrice == null || this.unitPrice.compareTo(BigDecimal.ZERO) == 0) {
                this.unitPrice = product.getPrice();
            }
            calculateLineTotal();
        }
    }

    // Getter & Setter
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
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
        calculateLineTotal();
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
        calculateLineTotal();
    }

    public BigDecimal getDiscountPercentage() {
        return discountPercentage;
    }

    public void setDiscountPercentage(BigDecimal discountPercentage) {
        this.discountPercentage = discountPercentage;
        // Reset discount amount wenn percentage gesetzt wird
        this.discountAmount = null;
        calculateLineTotal();
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
        // Reset discount percentage wenn amount gesetzt wird
        this.discountPercentage = null;
        calculateLineTotal();
    }

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(BigDecimal taxRate) {
        this.taxRate = taxRate;
    }

    public BigDecimal getLineTotal() {
        if (lineTotal == null) {
            calculateLineTotal();
        }
        return lineTotal;
    }

    public void setLineTotal(BigDecimal lineTotal) {
        this.lineTotal = lineTotal;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Invoice getInvoice() {
        return invoice;
    }

    public void setInvoice(Invoice invoice) {
        this.invoice = invoice;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
        updateFromProduct(product);
    }

    @Override
    public String toString() {
        return "InvoiceItem{" +
                "id=" + id +
                ", position=" + position +
                ", description='" + description + '\'' +
                ", quantity=" + quantity +
                ", unit='" + unit + '\'' +
                ", unitPrice=" + unitPrice +
                ", discountPercentage=" + discountPercentage +
                ", discountAmount=" + discountAmount +
                ", taxRate=" + taxRate +
                ", lineTotal=" + lineTotal +
                ", productId=" + (product != null ? product.getId() : null) +
                '}';
    }
}