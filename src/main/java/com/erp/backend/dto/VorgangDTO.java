package com.erp.backend.dto;

import com.erp.backend.domain.VorgangStatus;
import com.erp.backend.domain.VorgangTyp;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class VorgangDTO {
    private UUID id;
    private String vorgangsnummer;
    private VorgangTyp typ;
    private VorgangStatus status;
    private String titel;
    private String beschreibung;
    private LocalDateTime startZeitpunkt;
    private LocalDateTime endeZeitpunkt;
    private String ausgeloestVon;
    private boolean automatisch;
    private Integer anzahlVerarbeitet;
    private Integer anzahlErfolgreich;
    private Integer anzahlFehler;
    private BigDecimal gesamtbetrag;
    private String fehlerprotokoll;

    // Berechnete Felder
    private Long dauerInMs;
    private Double erfolgsquote;

    // Konstruktoren
    public VorgangDTO() {}

    public VorgangDTO(UUID id, String vorgangsnummer, VorgangTyp typ, VorgangStatus status,
                      String titel, String beschreibung, LocalDateTime startZeitpunkt,
                      LocalDateTime endeZeitpunkt, String ausgeloestVon, boolean automatisch,
                      Integer anzahlVerarbeitet, Integer anzahlErfolgreich, Integer anzahlFehler,
                      BigDecimal gesamtbetrag, String fehlerprotokoll) {
        this.id = id;
        this.vorgangsnummer = vorgangsnummer;
        this.typ = typ;
        this.status = status;
        this.titel = titel;
        this.beschreibung = beschreibung;
        this.startZeitpunkt = startZeitpunkt;
        this.endeZeitpunkt = endeZeitpunkt;
        this.ausgeloestVon = ausgeloestVon;
        this.automatisch = automatisch;
        this.anzahlVerarbeitet = anzahlVerarbeitet;
        this.anzahlErfolgreich = anzahlErfolgreich;
        this.anzahlFehler = anzahlFehler;
        this.gesamtbetrag = gesamtbetrag;
        this.fehlerprotokoll = fehlerprotokoll;
    }

    // Getters und Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getVorgangsnummer() { return vorgangsnummer; }
    public void setVorgangsnummer(String vorgangsnummer) { this.vorgangsnummer = vorgangsnummer; }

    public VorgangTyp getTyp() { return typ; }
    public void setTyp(VorgangTyp typ) { this.typ = typ; }

    public VorgangStatus getStatus() { return status; }
    public void setStatus(VorgangStatus status) { this.status = status; }

    public String getTitel() { return titel; }
    public void setTitel(String titel) { this.titel = titel; }

    public String getBeschreibung() { return beschreibung; }
    public void setBeschreibung(String beschreibung) { this.beschreibung = beschreibung; }

    public LocalDateTime getStartZeitpunkt() { return startZeitpunkt; }
    public void setStartZeitpunkt(LocalDateTime startZeitpunkt) { this.startZeitpunkt = startZeitpunkt; }

    public LocalDateTime getEndeZeitpunkt() { return endeZeitpunkt; }
    public void setEndeZeitpunkt(LocalDateTime endeZeitpunkt) { this.endeZeitpunkt = endeZeitpunkt; }

    public String getAusgeloestVon() { return ausgeloestVon; }
    public void setAusgeloestVon(String ausgeloestVon) { this.ausgeloestVon = ausgeloestVon; }

    public boolean isAutomatisch() { return automatisch; }
    public void setAutomatisch(boolean automatisch) { this.automatisch = automatisch; }

    public Integer getAnzahlVerarbeitet() { return anzahlVerarbeitet; }
    public void setAnzahlVerarbeitet(Integer anzahlVerarbeitet) { this.anzahlVerarbeitet = anzahlVerarbeitet; }

    public Integer getAnzahlErfolgreich() { return anzahlErfolgreich; }
    public void setAnzahlErfolgreich(Integer anzahlErfolgreich) { this.anzahlErfolgreich = anzahlErfolgreich; }

    public Integer getAnzahlFehler() { return anzahlFehler; }
    public void setAnzahlFehler(Integer anzahlFehler) { this.anzahlFehler = anzahlFehler; }

    public BigDecimal getGesamtbetrag() { return gesamtbetrag; }
    public void setGesamtbetrag(BigDecimal gesamtbetrag) { this.gesamtbetrag = gesamtbetrag; }

    public String getFehlerprotokoll() { return fehlerprotokoll; }
    public void setFehlerprotokoll(String fehlerprotokoll) { this.fehlerprotokoll = fehlerprotokoll; }

    public Long getDauerInMs() { return dauerInMs; }
    public void setDauerInMs(Long dauerInMs) { this.dauerInMs = dauerInMs; }

    public Double getErfolgsquote() { return erfolgsquote; }
    public void setErfolgsquote(Double erfolgsquote) { this.erfolgsquote = erfolgsquote; }
}