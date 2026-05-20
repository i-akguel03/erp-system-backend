package com.erp.backend.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Eingangsrechnung repräsentiert eine Rechnung eines Lieferanten (Kreditorenbuchhaltung).
 * Analogon zur Invoice auf der Debitorenseite.
 */
@Entity
@Table(name = "eingangsrechnungen")
public class Eingangsrechnung {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "eingangsrechnungsnummer", unique = true, nullable = false)
    private String eingangsrechnungsnummer;

    @Column(name = "lieferanten_rechnungsnummer")
    private String lieferantenRechnungsnummer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lieferant_id", nullable = false)
    private Lieferant lieferant;

    @Column(name = "eingangs_datum", nullable = false)
    private LocalDate eingangsDatum;

    @Column(name = "rechnungs_datum")
    private LocalDate rechnungsDatum;

    @Column(name = "faellig_datum", nullable = false)
    private LocalDate faelligDatum;

    @Column(name = "nettobetrag", precision = 10, scale = 2, nullable = false)
    private BigDecimal nettobetrag;

    @Column(name = "steuersatz", precision = 5, scale = 2)
    private BigDecimal steuersatz = BigDecimal.ZERO;

    @Column(name = "steuerbetrag", precision = 10, scale = 2)
    private BigDecimal steuerbetrag = BigDecimal.ZERO;

    @Column(name = "bruttobetrag", precision = 10, scale = 2, nullable = false)
    private BigDecimal bruttobetrag;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private EingangsrechnungStatus status = EingangsrechnungStatus.ERFASST;

    @Column(name = "aufwandskonto_nr")
    private Long aufwandskontoNr;

    @Column(name = "buchungssatz_id")
    private UUID buchungssatzId;

    @Column(name = "gezahlt_am")
    private LocalDate gezahltAm;

    @Column(name = "zahlungsreferenz")
    private String zahlungsreferenz;

    @Column(name = "notizen", columnDefinition = "TEXT")
    private String notizen;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (eingangsDatum == null) eingangsDatum = LocalDate.now();
    }

    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public Eingangsrechnung() {}

    // Getter & Setter
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getEingangsrechnungsnummer() { return eingangsrechnungsnummer; }
    public void setEingangsrechnungsnummer(String eingangsrechnungsnummer) { this.eingangsrechnungsnummer = eingangsrechnungsnummer; }

    public String getLieferantenRechnungsnummer() { return lieferantenRechnungsnummer; }
    public void setLieferantenRechnungsnummer(String lieferantenRechnungsnummer) { this.lieferantenRechnungsnummer = lieferantenRechnungsnummer; }

    public Lieferant getLieferant() { return lieferant; }
    public void setLieferant(Lieferant lieferant) { this.lieferant = lieferant; }

    public LocalDate getEingangsDatum() { return eingangsDatum; }
    public void setEingangsDatum(LocalDate eingangsDatum) { this.eingangsDatum = eingangsDatum; }

    public LocalDate getRechnungsDatum() { return rechnungsDatum; }
    public void setRechnungsDatum(LocalDate rechnungsDatum) { this.rechnungsDatum = rechnungsDatum; }

    public LocalDate getFaelligDatum() { return faelligDatum; }
    public void setFaelligDatum(LocalDate faelligDatum) { this.faelligDatum = faelligDatum; }

    public BigDecimal getNettobetrag() { return nettobetrag; }
    public void setNettobetrag(BigDecimal nettobetrag) { this.nettobetrag = nettobetrag; }

    public BigDecimal getSteuersatz() { return steuersatz; }
    public void setSteuersatz(BigDecimal steuersatz) { this.steuersatz = steuersatz; }

    public BigDecimal getSteuerbetrag() { return steuerbetrag; }
    public void setSteuerbetrag(BigDecimal steuerbetrag) { this.steuerbetrag = steuerbetrag; }

    public BigDecimal getBruttobetrag() { return bruttobetrag; }
    public void setBruttobetrag(BigDecimal bruttobetrag) { this.bruttobetrag = bruttobetrag; }

    public EingangsrechnungStatus getStatus() { return status; }
    public void setStatus(EingangsrechnungStatus status) { this.status = status; }

    public Long getAufwandskontoNr() { return aufwandskontoNr; }
    public void setAufwandskontoNr(Long aufwandskontoNr) { this.aufwandskontoNr = aufwandskontoNr; }

    public UUID getBuchungssatzId() { return buchungssatzId; }
    public void setBuchungssatzId(UUID buchungssatzId) { this.buchungssatzId = buchungssatzId; }

    public LocalDate getGezahltAm() { return gezahltAm; }
    public void setGezahltAm(LocalDate gezahltAm) { this.gezahltAm = gezahltAm; }

    public String getZahlungsreferenz() { return zahlungsreferenz; }
    public void setZahlungsreferenz(String zahlungsreferenz) { this.zahlungsreferenz = zahlungsreferenz; }

    public String getNotizen() { return notizen; }
    public void setNotizen(String notizen) { this.notizen = notizen; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}