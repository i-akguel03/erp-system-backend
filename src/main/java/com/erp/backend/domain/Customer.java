package com.erp.backend.domain;

public class Customer {

    private String id;
    private String firstName;
    private String lastName;
    private String email;
    private String tel;
    private String customerNumber;

    private String billingAddressId;
    private String shippingAddressId;
    private String residentialAddressId;


    public Customer(String id, String firstName, String lastName, String email, String tel) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.tel = tel;
    }

    // Getter & Setter

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCustomerNumber() {
        return customerNumber;
    }

    public void setCustomerNumber(String customerNumber) {
        this.customerNumber = customerNumber;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTel() {
        return tel;
    }

    public void setTel(String tel) {
        this.tel = tel;
    }

    public String getBillingAddressId() {
        return billingAddressId;
    }

    public void setBillingAddressId(String billingAddressId) {
        this.billingAddressId = billingAddressId;
    }

    public String getShippingAddressId() {
        return shippingAddressId;
    }

    public void setShippingAddressId(String shippingAddressId) {
        this.shippingAddressId = shippingAddressId;
    }

    public String getResidentialAddressId() {
        return residentialAddressId;
    }

    public void setResidentialAddressId(String residentialAddressId) {
        this.residentialAddressId = residentialAddressId;
    }

    @Override
    public String toString() {
        return "Customer{" +
                "id='" + id + '\'' +
                ", customerNumber='" + customerNumber + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", email='" + email + '\'' +
                ", tel='" + tel + '\'' +
                ", billingAddressId='" + billingAddressId + '\'' +
                ", shippingAddressId='" + shippingAddressId + '\'' +
                ", residentialAddressId='" + residentialAddressId + '\'' +
                '}';
    }

}
