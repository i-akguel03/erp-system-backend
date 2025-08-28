package com.erp.backend.dto;

import com.erp.backend.domain.Address;
import com.erp.backend.domain.Customer;

import java.util.UUID;

public class CustomerDto {
    private UUID id;
    private String customerNumber;
    private String firstName;
    private String lastName;
    private String email;
    private String tel;
    private Address residentialAddress;
    private Address billingAddress;
    private Address shippingAddress;

    public CustomerDto() {}

    public static CustomerDto fromEntity(Customer c) {
        CustomerDto dto = new CustomerDto();
        dto.setId(c.getId());
        dto.setCustomerNumber(c.getCustomerNumber());
        dto.setFirstName(c.getFirstName());
        dto.setLastName(c.getLastName());
        dto.setEmail(c.getEmail());
        dto.setTel(c.getTel());
        dto.setResidentialAddress(c.getResidentialAddress());
        dto.setBillingAddress(c.getBillingAddress());
        dto.setShippingAddress(c.getShippingAddress());
        return dto;
    }

    // Getter & Setter
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getCustomerNumber() { return customerNumber; }
    public void setCustomerNumber(String customerNumber) { this.customerNumber = customerNumber; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getTel() { return tel; }
    public void setTel(String tel) { this.tel = tel; }
    public Address getResidentialAddress() { return residentialAddress; }
    public void setResidentialAddress(Address residentialAddress) { this.residentialAddress = residentialAddress; }
    public Address getBillingAddress() { return billingAddress; }
    public void setBillingAddress(Address billingAddress) { this.billingAddress = billingAddress; }
    public Address getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(Address shippingAddress) { this.shippingAddress = shippingAddress; }
}
