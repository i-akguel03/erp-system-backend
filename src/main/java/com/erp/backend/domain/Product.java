package com.erp.backend.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "product_number", unique = true)
    private String productNumber;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "price", precision = 10, scale = 2, nullable = false)
    private BigDecimal price;

    @Column(name = "unit")
    private String unit; // z.B. "Stück", "kg", "m", "Std."

    @Column(name = "tax_rate", precision = 5, scale = 2)
    private BigDecimal taxRate;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_type")
    private ProductType productType;

    @Column(name = "active")
    private Boolean active;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Enum für Produkttypen
    public enum ProductType {
        PRODUCT("Produkt"),
        SERVICE("Dienstleistung"),
        DIGITAL("Digital"),
        SUBSCRIPTION("Abonnement");

        private final String displayName;

        ProductType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    // Konstruktoren
    public Product() {
        this.active = true;
        this.productType = ProductType.PRODUCT;
        this.createdAt = LocalDateTime.now();
    }

    public Product(String name, BigDecimal price, String unit) {
        this();
        this.name = name;
        this.price = price;
        this.unit = unit;
    }

    public Product(String productNumber, String name, String description, BigDecimal price, String unit, BigDecimal taxRate) {
        this();
        this.productNumber = productNumber;
        this.name = name;
        this.description = description;
        this.price = price;
        this.unit = unit;
        this.taxRate = taxRate;
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

    // Business-Methoden
    public BigDecimal getPriceWithTax() {
        if (taxRate != null && taxRate.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal taxAmount = price.multiply(taxRate.divide(BigDecimal.valueOf(100)));
            return price.add(taxAmount);
        }
        return price;
    }

    public boolean isService() {
        return ProductType.SERVICE.equals(productType);
    }

    public boolean isActive() {
        return active != null && active;
    }

    // Getter & Setter
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getProductNumber() {
        return productNumber;
    }

    public void setProductNumber(String productNumber) {
        this.productNumber = productNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(BigDecimal taxRate) {
        this.taxRate = taxRate;
    }

    public ProductType getProductType() {
        return productType;
    }

    public void setProductType(ProductType productType) {
        this.productType = productType;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
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

    @Override
    public String toString() {
        return "Product{" +
                "id=" + id +
                ", productNumber='" + productNumber + '\'' +
                ", name='" + name + '\'' +
                ", price=" + price +
                ", unit='" + unit + '\'' +
                ", taxRate=" + taxRate +
                ", productType=" + productType +
                ", active=" + active +
                '}';
    }
}