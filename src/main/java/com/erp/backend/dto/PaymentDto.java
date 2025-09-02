package com.erp.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO für Zahlungsinformationen
 */
public class PaymentDto {

    private BigDecimal paidAmount;
    private LocalDate paidDate;
    private String paymentMethod;
    private String paymentReference;
    private String notes;

    // Standard-Konstruktor
    public PaymentDto() {
    }

    // Konstruktor mit Grunddaten
    public PaymentDto(BigDecimal paidAmount, LocalDate paidDate) {
        this.paidAmount = paidAmount;
        this.paidDate = paidDate;
    }

    // Vollständiger Konstruktor
    public PaymentDto(BigDecimal paidAmount, LocalDate paidDate, String paymentMethod,
                      String paymentReference, String notes) {
        this.paidAmount = paidAmount;
        this.paidDate = paidDate;
        this.paymentMethod = paymentMethod;
        this.paymentReference = paymentReference;
        this.notes = notes;
    }

    // Getters und Setters
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

    // Validierungs-Methoden
    public boolean isValid() {
        return paidAmount != null &&
                paidAmount.compareTo(BigDecimal.ZERO) > 0 &&
                paidDate != null;
    }

    @Override
    public String toString() {
        return "PaymentDto{" +
                "paidAmount=" + paidAmount +
                ", paidDate=" + paidDate +
                ", paymentMethod='" + paymentMethod + '\'' +
                ", paymentReference='" + paymentReference + '\'' +
                ", notes='" + notes + '\'' +
                '}';
    }
}