package com.erp.backend.domain;

/**
 * Status eines Systemvorgangs
 */
public enum VorgangStatus {
    GESTARTET("Vorgang wurde initiiert"),
    LAUFEND("Vorgang wird aktuell verarbeitet"),
    ERFOLGREICH("Vorgang erfolgreich abgeschlossen"),
    FEHLER("Vorgang mit Fehlern abgeschlossen"),
    ABGEBROCHEN("Vorgang wurde abgebrochen"),
    PAUSIERT("Vorgang wurde pausiert"),
    WARTEN("Vorgang wartet auf externe Abhängigkeiten");

    private final String beschreibung;

    VorgangStatus(String beschreibung) {
        this.beschreibung = beschreibung;
    }

    public String getBeschreibung() {
        return beschreibung;
    }

    /**
     * Prüft ob der Status einen abgeschlossenen Vorgang darstellt
     */
    public boolean istAbgeschlossen() {
        return this == ERFOLGREICH || this == FEHLER || this == ABGEBROCHEN;
    }

    /**
     * Prüft ob der Status einen aktiven Vorgang darstellt
     */
    public boolean istAktiv() {
        return this == LAUFEND || this == GESTARTET || this == PAUSIERT || this == WARTEN;
    }
}