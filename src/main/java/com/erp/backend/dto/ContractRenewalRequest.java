package com.erp.backend.dto;

import java.time.LocalDate;

public class ContractRenewalRequest {

    private LocalDate newEndDate;
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
