package com.erp.backend.dto;

import com.erp.backend.domain.CrmActivity;

import java.time.LocalDateTime;
import java.util.UUID;

public class CrmActivityDto {

    private UUID id;
    private String title;
    private String description;
    private CrmActivity.ActivityType activityType;
    private CrmActivity.ActivityStatus status;
    private LocalDateTime activityDate;
    private LocalDateTime dueDate;
    private String contactPerson;
    private String result;
    private String createdBy;
    private UUID customerId;
    private String customerName;
    private UUID contractId;
    private String contractTitle;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public CrmActivityDto() {}

    public static CrmActivityDto fromEntity(CrmActivity activity) {
        CrmActivityDto dto = new CrmActivityDto();
        dto.setId(activity.getId());
        dto.setTitle(activity.getTitle());
        dto.setDescription(activity.getDescription());
        dto.setActivityType(activity.getActivityType());
        dto.setStatus(activity.getStatus());
        dto.setActivityDate(activity.getActivityDate());
        dto.setDueDate(activity.getDueDate());
        dto.setContactPerson(activity.getContactPerson());
        dto.setResult(activity.getResult());
        dto.setCreatedBy(activity.getCreatedBy());
        dto.setCreatedAt(activity.getCreatedAt());
        dto.setUpdatedAt(activity.getUpdatedAt());

        if (activity.getCustomer() != null) {
            try {
                dto.setCustomerId(activity.getCustomer().getId());
                dto.setCustomerName(activity.getCustomer().getName());
            } catch (Exception ignored) {}
        }
        if (activity.getContract() != null) {
            try {
                dto.setContractId(activity.getContract().getId());
                dto.setContractTitle(activity.getContract().getContractTitle());
            } catch (Exception ignored) {}
        }
        return dto;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public CrmActivity.ActivityType getActivityType() { return activityType; }
    public void setActivityType(CrmActivity.ActivityType activityType) { this.activityType = activityType; }
    public CrmActivity.ActivityStatus getStatus() { return status; }
    public void setStatus(CrmActivity.ActivityStatus status) { this.status = status; }
    public LocalDateTime getActivityDate() { return activityDate; }
    public void setActivityDate(LocalDateTime activityDate) { this.activityDate = activityDate; }
    public LocalDateTime getDueDate() { return dueDate; }
    public void setDueDate(LocalDateTime dueDate) { this.dueDate = dueDate; }
    public String getContactPerson() { return contactPerson; }
    public void setContactPerson(String contactPerson) { this.contactPerson = contactPerson; }
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
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
