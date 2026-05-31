package com.erp.backend.dto;

import com.erp.backend.domain.CrmDocument;

import java.time.LocalDateTime;
import java.util.UUID;

public class CrmDocumentDto {

    private UUID id;
    private String originalFileName;
    private String mimeType;
    private Long fileSize;
    private String description;
    private CrmDocument.DocumentType documentType;
    private String uploadedBy;
    private UUID customerId;
    private String customerName;
    private UUID contractId;
    private String contractTitle;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public CrmDocumentDto() {}

    public static CrmDocumentDto fromEntity(CrmDocument doc) {
        CrmDocumentDto dto = new CrmDocumentDto();
        dto.setId(doc.getId());
        dto.setOriginalFileName(doc.getOriginalFileName());
        dto.setMimeType(doc.getMimeType());
        dto.setFileSize(doc.getFileSize());
        dto.setDescription(doc.getDescription());
        dto.setDocumentType(doc.getDocumentType());
        dto.setUploadedBy(doc.getUploadedBy());
        dto.setCreatedAt(doc.getCreatedAt());
        dto.setUpdatedAt(doc.getUpdatedAt());

        if (doc.getCustomer() != null) {
            try {
                dto.setCustomerId(doc.getCustomer().getId());
                dto.setCustomerName(doc.getCustomer().getName());
            } catch (Exception ignored) {}
        }
        if (doc.getContract() != null) {
            try {
                dto.setContractId(doc.getContract().getId());
                dto.setContractTitle(doc.getContract().getContractTitle());
            } catch (Exception ignored) {}
        }
        return dto;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getOriginalFileName() { return originalFileName; }
    public void setOriginalFileName(String originalFileName) { this.originalFileName = originalFileName; }
    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public CrmDocument.DocumentType getDocumentType() { return documentType; }
    public void setDocumentType(CrmDocument.DocumentType documentType) { this.documentType = documentType; }
    public String getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(String uploadedBy) { this.uploadedBy = uploadedBy; }
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
