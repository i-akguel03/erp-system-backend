package com.erp.backend.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

/**
 * Repräsentiert eine Adresse (z. B. Rechnungs-, Liefer- oder Wohnadresse).
 * Wird von Customer als ManyToOne referenziert.
 */
@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Table(name = "addresses")
@SQLDelete(sql = "UPDATE addresses SET deleted = true WHERE id = ?")
@SQLRestriction("deleted = false") // Soft-Delete: Nur nicht gelöschte Adressen werden standardmäßig geladen
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-Increment ID
    private Long id;

    @Column(nullable = false)
    private String street;

    @Column(name = "postal_code", nullable = false)
    private String postalCode;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String country;

    @Column(nullable = false)
    private boolean deleted = false; // Soft-Delete Flag

    public Address() {
        // Standard-Konstruktor für JPA
    }

    public Address(String street, String postalCode, String city, String country) {
        this.street = street;
        this.postalCode = postalCode;
        this.city = city;
        this.country = country;
    }

    // ---- Getter & Setter ----
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getStreet() { return street; }
    public void setStreet(String street) { this.street = street; }

    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }

    @Override
    public String toString() {
        return street + ", " + postalCode + " " + city + ", " + country;
    }
}
