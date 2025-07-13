package com.erp.backend.domain;

public class Address {
    private String id;
    private String street;
    private String postalCode;
    private String city;
    private String country;


    // Konstruktor mit allen Parametern in der Reihenfolge id, street, postalCode, city, country
    public Address(String id, String street, String postalCode, String city, String country) {
        this.id = id;
        this.street = street;
        this.postalCode = postalCode;
        this.city = city;
        this.country = country;
    }

    // Getter & Setter für alle Felder
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getStreet() { return street; }
    public void setStreet(String street) { this.street = street; }

    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
}
