package com.erp.backend.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Buchungssatz repräsentiert einen Journaleintrag in der doppelten Buchführung.
 * Jeder Buchungssatz enthält mindestens zwei Buchungspositionen (Soll + Haben),
 * wobei die Summe Soll = Summe Haben gilt.
 */
@Entity
@Table(name = "buchungssaetze")
public class Buchungssatz {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "buchungsnummer", unique = true, nullable = false)
    private String buchungsnummer;

    @Column(name = "buchungs_datum", nullable = false)
    private LocalDate buchungsDatum;

    @Column(name = "valuta_datum")
    private LocalDate valutaDatum;

    @Column(name = "beschreibung", nullable = false)
    private String beschreibung;

    @Enumerated(EnumType.STRING)
    @Column(name = "beleg_typ", nullable = false)
    private BelegTyp belegTyp;

    @Column(name = "beleg_referenz_id")
    private String belegReferenzId;

    @Column(name = "beleg_referenz_nummer")
    private String belegReferenzNummer;

    @Column(name = "geschaeftsjahr", nullable = false)
    private int geschaeftsjahr;

    @Column(name = "monat", nullable = false)
    private int monat;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BuchungStatus status = BuchungStatus.GEBUCHT;

    @Column(name = "gebucht_von")
    private String gebuchtVon;

    @Column(name = "gebucht_am", nullable = false)
    private LocalDateTime gebuchtAm;

    @Column(name = "storno_von_id")
    private UUID stornoVonId;

    @OneToMany(mappedBy = "buchungssatz", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Buchungsposition> positionen = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (gebuchtAm == null) gebuchtAm = LocalDateTime.now();
        if (buchungsDatum == null) buchungsDatum = LocalDate.now();
        geschaeftsjahr = buchungsDatum.getYear();
        monat = buchungsDatum.getMonthValue();
    }

    public Buchungssatz() {}

    public void addPosition(Buchungsposition position) {
        positionen.add(position);
        position.setBuchungssatz(this);
    }

    public BigDecimal getSumSoll() {
        return positionen.stream()
                .filter(p -> BuchungsTyp.SOLL.equals(p.getBuchungsTyp()))
                .map(Buchungsposition::getBetrag)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getSumHaben() {
        return positionen.stream()
                .filter(p -> BuchungsTyp.HABEN.equals(p.getBuchungsTyp()))
                .map(Buchungsposition::getBetrag)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public boolean isAusgeglichen() {
        return getSumSoll().compareTo(getSumHaben()) == 0;
    }

    // Getter & Setter
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getBuchungsnummer() { return buchungsnummer; }
    public void setBuchungsnummer(String buchungsnummer) { this.buchungsnummer = buchungsnummer; }

    public LocalDate getBuchungsDatum() { return buchungsDatum; }
    public void setBuchungsDatum(LocalDate buchungsDatum) { this.buchungsDatum = buchungsDatum; }

    public LocalDate getValutaDatum() { return valutaDatum; }
    public void setValutaDatum(LocalDate valutaDatum) { this.valutaDatum = valutaDatum; }

    public String getBeschreibung() { return beschreibung; }
    public void setBeschreibung(String beschreibung) { this.beschreibung = beschreibung; }

    public BelegTyp getBelegTyp() { return belegTyp; }
    public void setBelegTyp(BelegTyp belegTyp) { this.belegTyp = belegTyp; }

    public String getBelegReferenzId() { return belegReferenzId; }
    public void setBelegReferenzId(String belegReferenzId) { this.belegReferenzId = belegReferenzId; }

    public String getBelegReferenzNummer() { return belegReferenzNummer; }
    public void setBelegReferenzNummer(String belegReferenzNummer) { this.belegReferenzNummer = belegReferenzNummer; }

    public int getGeschaeftsjahr() { return geschaeftsjahr; }
    public void setGeschaeftsjahr(int geschaeftsjahr) { this.geschaeftsjahr = geschaeftsjahr; }

    public int getMonat() { return monat; }
    public void setMonat(int monat) { this.monat = monat; }

    public BuchungStatus getStatus() { return status; }
    public void setStatus(BuchungStatus status) { this.status = status; }

    public String getGebuchtVon() { return gebuchtVon; }
    public void setGebuchtVon(String gebuchtVon) { this.gebuchtVon = gebuchtVon; }

    public LocalDateTime getGebuchtAm() { return gebuchtAm; }
    public void setGebuchtAm(LocalDateTime gebuchtAm) { this.gebuchtAm = gebuchtAm; }

    public UUID getStornoVonId() { return stornoVonId; }
    public void setStornoVonId(UUID stornoVonId) { this.stornoVonId = stornoVonId; }

    public List<Buchungsposition> getPositionen() { return positionen; }
    public void setPositionen(List<Buchungsposition> positionen) { this.positionen = positionen; }
}