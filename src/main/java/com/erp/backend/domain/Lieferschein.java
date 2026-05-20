package com.erp.backend.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "lieferscheine")
public class Lieferschein {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "lieferscheinnummer", unique = true, nullable = false)
    private String lieferscheinnummer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order auftrag;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "liefer_adresse_id")
    private Address lieferAdresse;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private LieferscheinStatus status = LieferscheinStatus.AUSSTEHEND;

    @Column(name = "liefer_datum")
    private LocalDate lieferDatum;

    @OneToMany(mappedBy = "lieferschein", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LieferscheinPosition> positionen = new ArrayList<>();

    @Column(name = "notizen", columnDefinition = "TEXT")
    private String notizen;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Lieferschein() {}

    public void addPosition(LieferscheinPosition position) {
        positionen.add(position);
        position.setLieferschein(this);
    }

    public boolean isGeliefert() {
        return LieferscheinStatus.GELIEFERT.equals(status);
    }

    // Getter & Setter
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getLieferscheinnummer() { return lieferscheinnummer; }
    public void setLieferscheinnummer(String lieferscheinnummer) { this.lieferscheinnummer = lieferscheinnummer; }

    public Order getAuftrag() { return auftrag; }
    public void setAuftrag(Order auftrag) { this.auftrag = auftrag; }

    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }

    public Address getLieferAdresse() { return lieferAdresse; }
    public void setLieferAdresse(Address lieferAdresse) { this.lieferAdresse = lieferAdresse; }

    public LieferscheinStatus getStatus() { return status; }
    public void setStatus(LieferscheinStatus status) { this.status = status; }

    public LocalDate getLieferDatum() { return lieferDatum; }
    public void setLieferDatum(LocalDate lieferDatum) { this.lieferDatum = lieferDatum; }

    public List<LieferscheinPosition> getPositionen() { return positionen; }
    public void setPositionen(List<LieferscheinPosition> positionen) { this.positionen = positionen; }

    public String getNotizen() { return notizen; }
    public void setNotizen(String notizen) { this.notizen = notizen; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}