package com.erp.backend.dto;

import com.erp.backend.domain.ContractStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class ContractDTO {

    private UUID id;
    private String contractNumber;
    private String contractTitle;   // exakt wie im Entity
    private LocalDate startDate;
    private LocalDate endDate;
    private ContractStatus contractStatus;
    private String notes;

    private UUID customerId; // nur die ID, nicht das ganze Customer-Objekt
    private List<UUID> subscriptionIds; // nur IDs, keine ganzen Objekte (optional)

    // --- Getter & Setter ---

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getContractNumber() {
        return contractNumber;
    }

    public void setContractNumber(String contractNumber) {
        this.contractNumber = contractNumber;
    }

    public String getContractTitle() {
        return contractTitle;
    }

    public void setContractTitle(String contractTitle) {
        this.contractTitle = contractTitle;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public ContractStatus getContractStatus() {
        return contractStatus;
    }

    public void setContractStatus(ContractStatus contractStatus) {
        this.contractStatus = contractStatus;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
    }

    public List<UUID> getSubscriptionIds() {
        return subscriptionIds;
    }

    public void setSubscriptionIds(List<UUID> subscriptionIds) {
        this.subscriptionIds = subscriptionIds;
    }
}
