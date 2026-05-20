package com.erp.backend.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "lieferanten")
public class Lieferant {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "lieferantennummer", unique = true, nullable = false)
    private String lieferantennummer;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "email")
    private String email;

    @Column(name = "tel")
    private String tel;

    @Column(name = "steuernummer")
    private String steuernummer;

    @Column(name = "ust_id_nr")
    private String ustIdNr;

    @Column(name = "iban")
    private String iban;

    @Column(name = "bic")
    private String bic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "adresse_id")
    private Address adresse;

    @Column(name = "aktiv", nullable = false)
    private boolean aktiv = true;

    @Column(name = "notizen", columnDefinition = "TEXT")
    private String notizen;

    @OneToMany(mappedBy = "lieferant", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Eingangsrechnung> eingangsrechnungen = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }

    public Lieferant() {}

    // Getter & Setter
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getLieferantennummer() { return lieferantennummer; }
    public void setLieferantennummer(String lieferantennummer) { this.lieferantennummer = lieferantennummer; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTel() { return tel; }
    public void setTel(String tel) { this.tel = tel; }

    public String getSteuernummer() { return steuernummer; }
    public void setSteuernummer(String steuernummer) { this.steuernummer = steuernummer; }

    public String getUstIdNr() { return ustIdNr; }
    public void setUstIdNr(String ustIdNr) { this.ustIdNr = ustIdNr; }

    public String getIban() { return iban; }
    public void setIban(String iban) { this.iban = iban; }

    public String getBic() { return bic; }
    public void setBic(String bic) { this.bic = bic; }

    public Address getAdresse() { return adresse; }
    public void setAdresse(Address adresse) { this.adresse = adresse; }

    public boolean isAktiv() { return aktiv; }
    public void setAktiv(boolean aktiv) { this.aktiv = aktiv; }

    public String getNotizen() { return notizen; }
    public void setNotizen(String notizen) { this.notizen = notizen; }

    public List<Eingangsrechnung> getEingangsrechnungen() { return eingangsrechnungen; }
    public void setEingangsrechnungen(List<Eingangsrechnung> eingangsrechnungen) { this.eingangsrechnungen = eingangsrechnungen; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}