package com.erp.backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public class PaymentDto {

    @NotNull(message = "Zahlungsbetrag ist erforderlich")
    @Positive(message = "Zahlungsbetrag muss positiv sein")
    private BigDecimal paidAmount;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate paidDate;

    private String paymentMethod;
    private String paymentReference;
    private String notes;

    // Konstruktoren
    public PaymentDto() {
        this.paidDate = LocalDate.now();
    }

    public PaymentDto(BigDecimal paidAmount, LocalDate paidDate, String paymentMethod) {
        this.paidAmount = paidAmount;
        this.paidDate = paidDate;
        this.paymentMethod = paymentMethod;
    }

    public PaymentDto(BigDecimal paidAmount, String paymentMethod, String paymentReference) {
        this.paidAmount = paidAmount;
        this.paidDate = LocalDate.now();
        this.paymentMethod = paymentMethod;
        this.paymentReference = paymentReference;
    }

    // Getter und Setter
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
                ", paidDate=" + paidDate +
                ", paymentMethod='" + paymentMethod + '\'' +
                ", paymentReference='" + paymentReference + '\'' +
                '}';
    }
}