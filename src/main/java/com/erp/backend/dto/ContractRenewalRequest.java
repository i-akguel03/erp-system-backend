package com.erp.backend.dto;

import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public class ContractRenewalRequest {

    private LocalDate newEndDate;

    @Positive(message = "extensionMonths muss eine positive Zahl sein")
    private Integer extensionMonths;

    public LocalDate getNewEndDate() {
        return newEndDate;
    }

    public void setNewEndDate(LocalDate newEndDate) {
        this.newEndDate = newEndDate;
    }

    public Integer getExtensionMonths() {
        return extensionMonths;
    }

    public void setExtensionMonths(Integer extensionMonths) {
        this.extensionMonths = extensionMonths;
    }
}
