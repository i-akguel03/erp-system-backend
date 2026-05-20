package com.erp.backend.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", unique = true)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @Column(name = "order_date", nullable = false)
    private LocalDateTime orderDate;

    @Column(name = "total_price", nullable = false)
    private double totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", columnDefinition = "varchar(255) not null default 'ENTWURF'")
    private OrderStatus status = OrderStatus.ENTWURF;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_source", columnDefinition = "varchar(255) not null default 'MANUELL'")
    private OrderSource orderSource = OrderSource.MANUELL;

    @Column(name = "external_order_id")
    private String externalOrderId;

    @Column(name = "angebot_id")
    private UUID angebotId;

    @Column(name = "invoice_id")
    private UUID invoiceId;

    @Column(name = "nettobetrag", precision = 10, scale = 2)
    private BigDecimal nettobetrag;

    @Column(name = "steuerbetrag", precision = 10, scale = 2)
    private BigDecimal steuerbetrag;

    @Column(name = "bruttobetrag", precision = 10, scale = 2)
    private BigDecimal bruttobetrag;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "liefer_adresse_id")
    private Address lieferAdresse;

    @Column(name = "liefer_datum")
    private LocalDate lieferDatum;

    @Column(name = "notizen", columnDefinition = "TEXT")
    private String notizen;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.orderDate == null) {
            this.orderDate = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Order() {}

    public Order(Customer customer, List<OrderItem> items, LocalDateTime orderDate, double totalPrice) {
        this.customer = customer;
        this.items = items;
        this.orderDate = orderDate;
        this.totalPrice = totalPrice;
    }

    // Getter & Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }

    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }

    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }

    public LocalDateTime getOrderDate() { return orderDate; }
    public void setOrderDate(LocalDateTime orderDate) { this.orderDate = orderDate; }

    public double getTotalPrice() { return totalPrice; }
    public void setTotalPrice(double totalPrice) { this.totalPrice = totalPrice; }

    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }

    public OrderSource getOrderSource() { return orderSource; }
    public void setOrderSource(OrderSource orderSource) { this.orderSource = orderSource; }

    public String getExternalOrderId() { return externalOrderId; }
    public void setExternalOrderId(String externalOrderId) { this.externalOrderId = externalOrderId; }

    public UUID getAngebotId() { return angebotId; }
    public void setAngebotId(UUID angebotId) { this.angebotId = angebotId; }

    public UUID getInvoiceId() { return invoiceId; }
    public void setInvoiceId(UUID invoiceId) { this.invoiceId = invoiceId; }

    public BigDecimal getNettobetrag() { return nettobetrag; }
    public void setNettobetrag(BigDecimal nettobetrag) { this.nettobetrag = nettobetrag; }

    public BigDecimal getSteuerbetrag() { return steuerbetrag; }
    public void setSteuerbetrag(BigDecimal steuerbetrag) { this.steuerbetrag = steuerbetrag; }

    public BigDecimal getBruttobetrag() { return bruttobetrag; }
    public void setBruttobetrag(BigDecimal bruttobetrag) { this.bruttobetrag = bruttobetrag; }

    public Address getLieferAdresse() { return lieferAdresse; }
    public void setLieferAdresse(Address lieferAdresse) { this.lieferAdresse = lieferAdresse; }

    public LocalDate getLieferDatum() { return lieferDatum; }
    public void setLieferDatum(LocalDate lieferDatum) { this.lieferDatum = lieferDatum; }

    public String getNotizen() { return notizen; }
    public void setNotizen(String notizen) { this.notizen = notizen; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}