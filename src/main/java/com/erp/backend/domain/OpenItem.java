package com.erp.backend.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "open_items")
@SQLDelete(sql = "UPDATE open_items SET deleted = true WHERE id = ?")
@Where(clause = "deleted = false")
public class OpenItem {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(nullable = false)
    private boolean deleted = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OpenItemStatus status = OpenItemStatus.OPEN;

    @Column(name = "paid_amount", precision = 10, scale = 2)
    private BigDecimal paidAmount;

    @Column(name = "paid_date")
    private LocalDate paidDate;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "payment_reference")
    private String paymentReference;

    // === ENUM ===
    public enum OpenItemStatus {
        OPEN,
        PARTIALLY_PAID,
        PAID,
        CANCELLED
    }

    // === Konstruktoren ===
    public OpenItem() {}

    public OpenItem(Invoice invoice, String description, BigDecimal amount, LocalDate dueDate) {
        this.invoice = invoice;
        this.description = description;
        this.amount = amount;
        this.dueDate = dueDate;
        this.status = OpenItemStatus.OPEN;
    }

    // === Business-Methoden ===
    public void markAsPaid(BigDecimal paidAmount, String paymentMethod, String paymentReference) {
        this.paidAmount = (this.paidAmount == null ? BigDecimal.ZERO : this.paidAmount).add(paidAmount);
        this.paymentMethod = paymentMethod;
        this.paymentReference = paymentReference;
        this.paidDate = (paidDate != null ? paidDate : LocalDate.now());

        if (this.paidAmount.compareTo(this.amount) >= 0) {
            this.status = OpenItemStatus.PAID;
        } else {
            this.status = OpenItemStatus.PARTIALLY_PAID;
        }
    }

    public BigDecimal getOutstandingAmount() {
        return amount.subtract(paidAmount != null ? paidAmount : BigDecimal.ZERO);
    }

    // === Getter / Setter ===
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

    @Override
    public String toString() {
        return "OpenItem{" +
                "id=" + id +
                ", invoice=" + (invoice != null ? invoice.getInvoiceNumber() : null) +
                ", description='" + description + '\'' +
                ", amount=" + amount +
                ", dueDate=" + dueDate +
                ", status=" + status +
                ", outstanding=" + getOutstandingAmount() +
                '}';
    }
}
