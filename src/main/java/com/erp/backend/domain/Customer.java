package com.erp.backend.domain;

public class Customer {
    private String id;
    private String firstName;
    private String lastName;
    private String email;
    private String tel;

    // Konstruktoren
    public Customer() {}

    public Customer(String id, String firstName, String lastName, String email, String tel) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.tel = tel;
    }

    // Getter
    public String getId() { return id; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; }
    public String getTel() { return tel; }

    // Setter
    public void setId(String id) { this.id = id; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public void setEmail(String email) { this.email = email; }
    public void setTel(String tel) { this.tel = tel; }
}