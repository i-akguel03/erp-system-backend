package com.erp.backend.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "buchungspositionen")
public class Buchungsposition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buchungssatz_id", nullable = false)
    private Buchungssatz buchungssatz;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kontonummer", nullable = false)
    private Konto konto;

    @Enumerated(EnumType.STRING)
    @Column(name = "buchungs_typ", nullable = false)
    private BuchungsTyp buchungsTyp;

    @Column(name = "betrag", precision = 12, scale = 2, nullable = false)
    private BigDecimal betrag;

    @Column(name = "beschreibung")
    private String beschreibung;

    @Column(name = "kostenstelle")
    private String kostenstelle;

    public Buchungsposition() {}

    public Buchungsposition(Konto konto, BuchungsTyp buchungsTyp, BigDecimal betrag, String beschreibung) {
        this.konto = konto;
        this.buchungsTyp = buchungsTyp;
        this.betrag = betrag;
        this.beschreibung = beschreibung;
    }

    // Getter & Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Buchungssatz getBuchungssatz() { return buchungssatz; }
    public void setBuchungssatz(Buchungssatz buchungssatz) { this.buchungssatz = buchungssatz; }

    public Konto getKonto() { return konto; }
    public void setKonto(Konto konto) { this.konto = konto; }

    public BuchungsTyp getBuchungsTyp() { return buchungsTyp; }
    public void setBuchungsTyp(BuchungsTyp buchungsTyp) { this.buchungsTyp = buchungsTyp; }

    public BigDecimal getBetrag() { return betrag; }
    public void setBetrag(BigDecimal betrag) { this.betrag = betrag; }

    public String getBeschreibung() { return beschreibung; }
    public void setBeschreibung(String beschreibung) { this.beschreibung = beschreibung; }

    public String getKostenstelle() { return kostenstelle; }
    public void setKostenstelle(String kostenstelle) { this.kostenstelle = kostenstelle; }
}