package com.erp.backend.dto;

import com.erp.backend.domain.CrmNote;

import java.time.LocalDateTime;
import java.util.UUID;

public class CrmNoteDto {

    private UUID id;
    private String title;
    private String content;
    private CrmNote.NotePriority priority;
    private String createdBy;
    private UUID customerId;
    private String customerName;
    private UUID contractId;
    private String contractTitle;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public CrmNoteDto() {}

    public static CrmNoteDto fromEntity(CrmNote note) {
        CrmNoteDto dto = new CrmNoteDto();
        dto.setId(note.getId());
        dto.setTitle(note.getTitle());
        dto.setContent(note.getContent());
        dto.setPriority(note.getPriority());
        dto.setCreatedBy(note.getCreatedBy());
        dto.setCreatedAt(note.getCreatedAt());
        dto.setUpdatedAt(note.getUpdatedAt());

        if (note.getCustomer() != null) {
            try {
                dto.setCustomerId(note.getCustomer().getId());
                dto.setCustomerName(note.getCustomer().getName());
            } catch (Exception ignored) {}
        }
        if (note.getContract() != null) {
            try {
                dto.setContractId(note.getContract().getId());
                dto.setContractTitle(note.getContract().getContractTitle());
            } catch (Exception ignored) {}
        }
        return dto;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public CrmNote.NotePriority getPriority() { return priority; }
    public void setPriority(CrmNote.NotePriority priority) { this.priority = priority; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public UUID getCustomerId() { return customerId; }
    public void setCustomerId(UUID customerId) { this.customerId = customerId; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public UUID getContractId() { return contractId; }
    public void setContractId(UUID contractId) { this.contractId = contractId; }
    public String getContractTitle() { return contractTitle; }
    public void setContractTitle(String contractTitle) { this.contractTitle = contractTitle; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
