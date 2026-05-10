package com.erp.backend.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class RenewalBatchResult {

    private int totalProcessed;
    private int successful;
    private int failed;
    private LocalDate batchDate;
    private List<ContractRenewalResult> results;
    private UUID vorgangId;
    private String vorgangsnummer;

    public RenewalBatchResult() {
    }

    public RenewalBatchResult(int totalProcessed, int successful, int failed,
                               LocalDate batchDate, List<ContractRenewalResult> results) {
        this.totalProcessed = totalProcessed;
        this.successful = successful;
        this.failed = failed;
        this.batchDate = batchDate;
        this.results = results;
    }

    public int getTotalProcessed() { return totalProcessed; }
    public void setTotalProcessed(int totalProcessed) { this.totalProcessed = totalProcessed; }

    public int getSuccessful() { return successful; }
    public void setSuccessful(int successful) { this.successful = successful; }

    public int getFailed() { return failed; }
    public void setFailed(int failed) { this.failed = failed; }

    public LocalDate getBatchDate() { return batchDate; }
    public void setBatchDate(LocalDate batchDate) { this.batchDate = batchDate; }

    public List<ContractRenewalResult> getResults() { return results; }
    public void setResults(List<ContractRenewalResult> results) { this.results = results; }

    public UUID getVorgangId() { return vorgangId; }
    public void setVorgangId(UUID vorgangId) { this.vorgangId = vorgangId; }

    public String getVorgangsnummer() { return vorgangsnummer; }
    public void setVorgangsnummer(String vorgangsnummer) { this.vorgangsnummer = vorgangsnummer; }
}
