package com.erp.backend.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Domain-Entität für System-Vorgänge.
 *
 * Loggt alle wichtigen Geschäftsprozesse im System:
 * - Rechnungsläufe
 * - Datenimporte
 * - Bulk-Operationen
 * - Status-Änderungen
 * - Zahlungseingänge
 * - etc.
 *
 * Jeder Vorgang kann mit mehreren Entitäten verknüpft werden.
 */
@Entity
@Table(name = "vorgaenge")
public class Vorgang {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Eindeutige Vorgangsnummer für bessere Nachverfolgung
     * Format: VG-YYYYMMDD-XXXXX
     */
    @Column(unique = true, nullable = false)
    private String vorgangsnummer;

    /**
     * Typ des Vorgangs
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VorgangTyp typ;

    /**
     * Aktueller Status des Vorgangs
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VorgangStatus status;

    /**
     * Titel/Kurzbeschreibung des Vorgangs
     */
    @Column(nullable = false, length = 500)
    private String titel;

    /**
     * Detaillierte Beschreibung
     */
    @Column(columnDefinition = "TEXT")
    private String beschreibung;

    /**
     * Start-Zeitpunkt des Vorgangs
     */
    @Column(nullable = false)
    private LocalDateTime startZeitpunkt;

    /**
     * Ende-Zeitpunkt (falls abgeschlossen)
     */
    private LocalDateTime endeZeitpunkt;

    /**
     * Benutzer der den Vorgang ausgelöst hat
     */
    private String ausgeloestVon;

    /**
     * Automatisch vs. manuell ausgelöst
     */
    @Column(nullable = false)
    private boolean automatisch = false;

    /**
     * Anzahl verarbeiteter Datensätze
     */
    private Integer anzahlVerarbeitet = 0;

    /**
     * Anzahl erfolgreich verarbeiteter Datensätze
     */
    private Integer anzahlErfolgreich = 0;

    /**
     * Anzahl fehlgeschlagener Datensätze
     */
    private Integer anzahlFehler = 0;

    /**
     * Gesamtbetrag (falls monetärer Vorgang)
     */
    private BigDecimal gesamtbetrag;

    /**
     * Fehlerprotokoll (JSON oder Text)
     */
    @Column(columnDefinition = "TEXT")
    private String fehlerprotokoll;

    /**
     * Zusätzliche Metadaten (JSON)
     */
    @Column(columnDefinition = "TEXT")
    private String metadaten;

// Vorgang Entity - Anpassung für JSON-Serialisierung
// Fügen Sie diese Annotationen zu den OneToMany-Beziehungen in Ihrer Vorgang-Klasse hinzu:

    /**
     * Verknüpfte Rechnungen
     */
    @OneToMany(mappedBy = "vorgang", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore  // Verhindert zirkuläre Referenzen
    private List<Invoice> rechnungen = new ArrayList<>();

    /**
     * Verknüpfte OpenItems
     */
    @OneToMany(mappedBy = "vorgang", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore  // Verhindert zirkuläre Referenzen
    private List<OpenItem> openItems = new ArrayList<>();

    /**
     * Verknüpfte DueSchedules
     */
    @OneToMany(mappedBy = "vorgang", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore  // Verhindert zirkuläre Referenzen
    private List<DueSchedule> dueSchedules = new ArrayList<>();

// Alternative: JsonManagedReference verwenden
// @JsonManagedReference("vorgang-rechnungen")
// private List<Invoice> rechnungen = new ArrayList<>();

    // ===============================================================================================
    // KONSTRUKTOREN
    // ===============================================================================================

    public Vorgang() {}

    public Vorgang(VorgangTyp typ, String titel) {
        this.typ = typ;
        this.titel = titel;
        this.status = VorgangStatus.GESTARTET;
        this.startZeitpunkt = LocalDateTime.now();
    }

    public Vorgang(VorgangTyp typ, String titel, String beschreibung) {
        this(typ, titel);
        this.beschreibung = beschreibung;
    }

    public Vorgang(VorgangTyp typ, String titel, String ausgeloestVon, boolean automatisch) {
        this(typ, titel);
        this.ausgeloestVon = ausgeloestVon;
        this.automatisch = automatisch;
    }

    // ===============================================================================================
    // GESCHÄFTSMETHODEN
    // ===============================================================================================

    /**
     * Startet den Vorgang (setzt Status und Zeitpunkt)
     */
    public void starten() {
        this.status = VorgangStatus.LAUFEND;
        if (this.startZeitpunkt == null) {
            this.startZeitpunkt = LocalDateTime.now();
        }
    }

    /**
     * Schließt den Vorgang erfolgreich ab
     */
    public void erfolgreichAbschliessen() {
        this.status = VorgangStatus.ERFOLGREICH;
        this.endeZeitpunkt = LocalDateTime.now();
    }

    /**
     * Schließt den Vorgang mit Fehlern ab
     */
    public void mitFehlerAbschliessen(String fehlerDetails) {
        this.status = VorgangStatus.FEHLER;
        this.endeZeitpunkt = LocalDateTime.now();
        if (fehlerDetails != null) {
            this.fehlerprotokoll = fehlerDetails;
        }
    }

    /**
     * Bricht den Vorgang ab
     */
    public void abbrechen(String grund) {
        this.status = VorgangStatus.ABGEBROCHEN;
        this.endeZeitpunkt = LocalDateTime.now();
        if (grund != null) {
            this.fehlerprotokoll = "Abgebrochen: " + grund;
        }
    }

    /**
     * Aktualisiert die Verarbeitungsstatistiken
     */
    public void updateStatistiken(int verarbeitet, int erfolgreich, int fehler) {
        this.anzahlVerarbeitet = verarbeitet;
        this.anzahlErfolgreich = erfolgreich;
        this.anzahlFehler = fehler;
    }

    /**
     * Fügt eine Rechnung zu diesem Vorgang hinzu
     */
    public void addRechnung(Invoice rechnung) {
        if (rechnung != null) {
            this.rechnungen.add(rechnung);
            rechnung.setVorgang(this);
        }
    }

    /**
     * Fügt einen OpenItem zu diesem Vorgang hinzu
     */
    public void addOpenItem(OpenItem openItem) {
        if (openItem != null) {
            this.openItems.add(openItem);
            openItem.setVorgang(this);
        }
    }

    /**
     * Fügt eine DueSchedule zu diesem Vorgang hinzu
     */
    public void addDueSchedule(DueSchedule dueSchedule) {
        if (dueSchedule != null) {
            this.dueSchedules.add(dueSchedule);
            dueSchedule.setVorgang(this);
        }
    }

    /**
     * Berechnet die Dauer des Vorgangs in Millisekunden
     */
    public Long getDauerInMs() {
        if (startZeitpunkt == null) return null;
        LocalDateTime ende = endeZeitpunkt != null ? endeZeitpunkt : LocalDateTime.now();
        return java.time.Duration.between(startZeitpunkt, ende).toMillis();
    }

    /**
     * Prüft ob der Vorgang erfolgreich war
     */
    public boolean istErfolgreich() {
        return status == VorgangStatus.ERFOLGREICH;
    }

    /**
     * Prüft ob der Vorgang noch läuft
     */
    public boolean istLaufend() {
        return status == VorgangStatus.LAUFEND || status == VorgangStatus.GESTARTET;
    }

    /**
     * Berechnet die Erfolgsquote in Prozent
     */
    public Double getErfolgsquote() {
        if (anzahlVerarbeitet == null || anzahlVerarbeitet == 0) return null;
        if (anzahlErfolgreich == null) return 0.0;
        return (anzahlErfolgreich * 100.0) / anzahlVerarbeitet;
    }

    // ===============================================================================================
    // FACTORY-METHODEN FÜR HÄUFIGE VORGÄNGE
    // ===============================================================================================

    public static Vorgang rechnungslauf(String stichtag, String batchId) {
        Vorgang vorgang = new Vorgang(
                VorgangTyp.RECHNUNGSLAUF,
                "Rechnungslauf zum " + stichtag,
                "Automatischer Rechnungslauf mit Batch-ID: " + batchId
        );
        vorgang.setAutomatisch(true);
        vorgang.setMetadaten("{\"batchId\":\"" + batchId + "\",\"stichtag\":\"" + stichtag + "\"}");
        return vorgang;
    }

    public static Vorgang datenImport(String dateityp, String dateiname) {
        return new Vorgang(
                VorgangTyp.DATENIMPORT,
                "Import: " + dateityp,
                "Import der Datei: " + dateiname
        );
    }

    public static Vorgang statusUpdate(String entitaetstyp, String neuerStatus) {
        return new Vorgang(
                VorgangTyp.STATUS_AENDERUNG,
                "Status-Update: " + entitaetstyp + " → " + neuerStatus
        );
    }

    public static Vorgang zahlungseingang(BigDecimal betrag) {
        Vorgang vorgang = new Vorgang(
                VorgangTyp.ZAHLUNGSEINGANG,
                "Zahlungseingang über " + betrag + " EUR"
        );
        vorgang.setGesamtbetrag(betrag);
        return vorgang;
    }

    public static Vorgang bulkOperation(String operation, int anzahlDatensaetze) {
        Vorgang vorgang = new Vorgang(
                VorgangTyp.BULK_OPERATION,
                "Bulk-Operation: " + operation,
                "Verarbeitung von " + anzahlDatensaetze + " Datensätzen"
        );
        vorgang.setAnzahlVerarbeitet(anzahlDatensaetze);
        return vorgang;
    }

    // ===============================================================================================
    // GETTER UND SETTER
    // ===============================================================================================

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

    public String getMetadaten() { return metadaten; }
    public void setMetadaten(String metadaten) { this.metadaten = metadaten; }

    public List<Invoice> getRechnungen() { return rechnungen; }
    public void setRechnungen(List<Invoice> rechnungen) { this.rechnungen = rechnungen; }

    public List<OpenItem> getOpenItems() { return openItems; }
    public void setOpenItems(List<OpenItem> openItems) { this.openItems = openItems; }

    public List<DueSchedule> getDueSchedules() { return dueSchedules; }
    public void setDueSchedules(List<DueSchedule> dueSchedules) { this.dueSchedules = dueSchedules; }

    @Override
    public String toString() {
        return String.format("Vorgang{id=%s, nummer='%s', typ=%s, status=%s, titel='%s', start=%s}",
                id, vorgangsnummer, typ, status, titel, startZeitpunkt);
    }
}