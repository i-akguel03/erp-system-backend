package com.erp.backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO für Zahlungen, die auf einen Fälligkeitsplan verbucht werden.
 */
public class PaymentDto {

    @NotNull(message = "Bezahlter Betrag ist erforderlich")
    @DecimalMin(value = "0.01", message = "Bezahlter Betrag muss größer als 0 sein")
    private BigDecimal paidAmount;

    @NotBlank(message = "Zahlungsmethode ist erforderlich")
    @Size(max = 100, message = "Zahlungsmethode darf maximal 100 Zeichen haben")
    private String paymentMethod;

    @Size(max = 200, message = "Zahlungsreferenz darf maximal 200 Zeichen haben")
    private String paymentReference;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate paidDate;

    @Size(max = 500, message = "Notizen dürfen maximal 500 Zeichen haben")
    private String notes;

    // Constructors
    public PaymentDto() {}

    public PaymentDto(BigDecimal paidAmount, String paymentMethod, String paymentReference, LocalDate paidDate, String notes) {
        this.paidAmount = paidAmount;
        this.paymentMethod = paymentMethod;
        this.paymentReference = paymentReference;
        this.paidDate = paidDate;
        this.notes = notes;
    }

    // Getter & Setter
    public BigDecimal getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(BigDecimal paidAmount) {
        this.paidAmount = paidAmount;
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

    public LocalDate getPaidDate() {
        return paidDate;
    }

    public void setPaidDate(LocalDate paidDate) {
        this.paidDate = paidDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @Override
    public String toString() {
        return "PaymentDto{" +
                "paidAmount=" + paidAmount +
                ", paymentMethod='" + paymentMethod + '\'' +
                ", paymentReference='" + paymentReference + '\'' +
                ", paidDate=" + paidDate +
                ", notes='" + notes + '\'' +
                '}';
    }
}
