package com.erp.backend.dto;

import java.time.LocalDate;
import java.util.UUID;

public class ContractRenewalResult {

    private UUID contractId;
    private String contractNumber;
    private String contractTitle;
    private LocalDate oldEndDate;
    private LocalDate newEndDate;
    private int subscriptionsRenewed;
    private int dueSchedulesCreated;
    private boolean success;
    private String errorMessage;
    private UUID vorgangId;
    private String vorgangsnummer;

    public ContractRenewalResult() {
    }

    public ContractRenewalResult(UUID contractId, String contractNumber, String contractTitle,
                                 LocalDate oldEndDate, LocalDate newEndDate,
                                 int subscriptionsRenewed, int dueSchedulesCreated,
                                 boolean success, String errorMessage) {
        this.contractId = contractId;
        this.contractNumber = contractNumber;
        this.contractTitle = contractTitle;
        this.oldEndDate = oldEndDate;
        this.newEndDate = newEndDate;
        this.subscriptionsRenewed = subscriptionsRenewed;
        this.dueSchedulesCreated = dueSchedulesCreated;
        this.success = success;
        this.errorMessage = errorMessage;
    }

    public static ContractRenewalResult failure(UUID contractId, String contractNumber,
                                                String contractTitle, String errorMessage) {
        return new ContractRenewalResult(contractId, contractNumber, contractTitle,
                null, null, 0, 0, false, errorMessage);
    }

    public UUID getContractId() { return contractId; }
    public void setContractId(UUID contractId) { this.contractId = contractId; }

    public String getContractNumber() { return contractNumber; }
    public void setContractNumber(String contractNumber) { this.contractNumber = contractNumber; }

    public String getContractTitle() { return contractTitle; }
    public void setContractTitle(String contractTitle) { this.contractTitle = contractTitle; }

    public LocalDate getOldEndDate() { return oldEndDate; }
    public void setOldEndDate(LocalDate oldEndDate) { this.oldEndDate = oldEndDate; }

    public LocalDate getNewEndDate() { return newEndDate; }
    public void setNewEndDate(LocalDate newEndDate) { this.newEndDate = newEndDate; }

    public int getSubscriptionsRenewed() { return subscriptionsRenewed; }
    public void setSubscriptionsRenewed(int subscriptionsRenewed) { this.subscriptionsRenewed = subscriptionsRenewed; }

    public int getDueSchedulesCreated() { return dueSchedulesCreated; }
    public void setDueSchedulesCreated(int dueSchedulesCreated) { this.dueSchedulesCreated = dueSchedulesCreated; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public UUID getVorgangId() { return vorgangId; }
    public void setVorgangId(UUID vorgangId) { this.vorgangId = vorgangId; }

    public String getVorgangsnummer() { return vorgangsnummer; }
    public void setVorgangsnummer(String vorgangsnummer) { this.vorgangsnummer = vorgangsnummer; }
}
