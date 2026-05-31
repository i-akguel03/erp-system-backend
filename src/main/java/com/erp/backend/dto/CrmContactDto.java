package com.erp.backend.dto;

import com.erp.backend.domain.CrmContact;

import java.time.LocalDateTime;
import java.util.UUID;

public class CrmContactDto {

    private UUID id;
    private String firstName;
    private String lastName;
    private String fullName;
    private String email;
    private String phone;
    private String mobile;
    private String position;
    private String department;
    private String notes;
    private boolean primaryContact;
    private UUID customerId;
    private String customerName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public CrmContactDto() {}

    public static CrmContactDto fromEntity(CrmContact contact) {
        CrmContactDto dto = new CrmContactDto();
        dto.setId(contact.getId());
        dto.setFirstName(contact.getFirstName());
        dto.setLastName(contact.getLastName());
        dto.setFullName(contact.getFullName());
        dto.setEmail(contact.getEmail());
        dto.setPhone(contact.getPhone());
        dto.setMobile(contact.getMobile());
        dto.setPosition(contact.getPosition());
        dto.setDepartment(contact.getDepartment());
        dto.setNotes(contact.getNotes());
        dto.setPrimaryContact(contact.isPrimaryContact());
        dto.setCreatedAt(contact.getCreatedAt());
        dto.setUpdatedAt(contact.getUpdatedAt());

        if (contact.getCustomer() != null) {
            try {
                dto.setCustomerId(contact.getCustomer().getId());
                dto.setCustomerName(contact.getCustomer().getName());
            } catch (Exception ignored) {}
        }
        return dto;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }
    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public boolean isPrimaryContact() { return primaryContact; }
    public void setPrimaryContact(boolean primaryContact) { this.primaryContact = primaryContact; }
    public UUID getCustomerId() { return customerId; }
    public void setCustomerId(UUID customerId) { this.customerId = customerId; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
