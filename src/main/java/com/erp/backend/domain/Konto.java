package com.erp.backend.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "konten")
public class Konto {

    @Id
    @Column(name = "kontonummer", nullable = false)
    private Long kontonummer;

    @Column(name = "bezeichnung", nullable = false)
    private String bezeichnung;

    @Enumerated(EnumType.STRING)
    @Column(name = "konto_typ", nullable = false)
    private KontoTyp kontoTyp;

    @Column(name = "konto_klasse")
    private String kontoKlasse;

    @Column(name = "sammelkonto", nullable = false)
    private boolean sammelkonto = false;

    @Column(name = "aktiv", nullable = false)
    private boolean aktiv = true;

    @Column(name = "beschreibung_lang", columnDefinition = "TEXT")
    private String beschreibungLang;

    public Konto() {}

    public Konto(Long kontonummer, String bezeichnung, KontoTyp kontoTyp, String kontoKlasse) {
        this.kontonummer = kontonummer;
        this.bezeichnung = bezeichnung;
        this.kontoTyp = kontoTyp;
        this.kontoKlasse = kontoKlasse;
    }

    // Getter & Setter
    public Long getKontonummer() { return kontonummer; }
    public void setKontonummer(Long kontonummer) { this.kontonummer = kontonummer; }

    public String getBezeichnung() { return bezeichnung; }
    public void setBezeichnung(String bezeichnung) { this.bezeichnung = bezeichnung; }

    public KontoTyp getKontoTyp() { return kontoTyp; }
    public void setKontoTyp(KontoTyp kontoTyp) { this.kontoTyp = kontoTyp; }

    public String getKontoKlasse() { return kontoKlasse; }
    public void setKontoKlasse(String kontoKlasse) { this.kontoKlasse = kontoKlasse; }

    public boolean isSammelkonto() { return sammelkonto; }
    public void setSammelkonto(boolean sammelkonto) { this.sammelkonto = sammelkonto; }

    public boolean isAktiv() { return aktiv; }
    public void setAktiv(boolean aktiv) { this.aktiv = aktiv; }

    public String getBeschreibungLang() { return beschreibungLang; }
    public void setBeschreibungLang(String beschreibungLang) { this.beschreibungLang = beschreibungLang; }
}