package com.erp.backend.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "angebote")
public class Angebot {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "angebotsnummer", unique = true, nullable = false)
    private String angebotsnummer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AngebotStatus status = AngebotStatus.ENTWURF;

    @Column(name = "angebots_datum", nullable = false)
    private LocalDate angebotsDatum;

    @Column(name = "gueltig_bis")
    private LocalDate gueltigBis;

    @OneToMany(mappedBy = "angebot", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AngebotPosition> positionen = new ArrayList<>();

    @Column(name = "nettobetrag", precision = 10, scale = 2)
    private BigDecimal nettobetrag = BigDecimal.ZERO;

    @Column(name = "steuerbetrag", precision = 10, scale = 2)
    private BigDecimal steuerbetrag = BigDecimal.ZERO;

    @Column(name = "bruttobetrag", precision = 10, scale = 2)
    private BigDecimal bruttobetrag = BigDecimal.ZERO;

    @Column(name = "notizen", columnDefinition = "TEXT")
    private String notizen;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.angebotsDatum = (angebotsDatum != null) ? angebotsDatum : LocalDate.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Angebot() {}

    public void addPosition(AngebotPosition position) {
        positionen.add(position);
        position.setAngebot(this);
    }

    public void berechneTotals() {
        nettobetrag = positionen.stream()
                .map(AngebotPosition::getNettobetrag)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        steuerbetrag = positionen.stream()
                .map(AngebotPosition::getSteuerbetrag)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        bruttobetrag = nettobetrag.add(steuerbetrag);
    }

    public boolean isAbgelaufen() {
        return gueltigBis != null && LocalDate.now().isAfter(gueltigBis);
    }

    // Getter & Setter
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getAngebotsnummer() { return angebotsnummer; }
    public void setAngebotsnummer(String angebotsnummer) { this.angebotsnummer = angebotsnummer; }

    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }

    public AngebotStatus getStatus() { return status; }
    public void setStatus(AngebotStatus status) { this.status = status; }

    public LocalDate getAngebotsDatum() { return angebotsDatum; }
    public void setAngebotsDatum(LocalDate angebotsDatum) { this.angebotsDatum = angebotsDatum; }

    public LocalDate getGueltigBis() { return gueltigBis; }
    public void setGueltigBis(LocalDate gueltigBis) { this.gueltigBis = gueltigBis; }

    public List<AngebotPosition> getPositionen() { return positionen; }
    public void setPositionen(List<AngebotPosition> positionen) { this.positionen = positionen; }

    public BigDecimal getNettobetrag() { return nettobetrag; }
    public void setNettobetrag(BigDecimal nettobetrag) { this.nettobetrag = nettobetrag; }

    public BigDecimal getSteuerbetrag() { return steuerbetrag; }
    public void setSteuerbetrag(BigDecimal steuerbetrag) { this.steuerbetrag = steuerbetrag; }

    public BigDecimal getBruttobetrag() { return bruttobetrag; }
    public void setBruttobetrag(BigDecimal bruttobetrag) { this.bruttobetrag = bruttobetrag; }

    public String getNotizen() { return notizen; }
    public void setNotizen(String notizen) { this.notizen = notizen; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}