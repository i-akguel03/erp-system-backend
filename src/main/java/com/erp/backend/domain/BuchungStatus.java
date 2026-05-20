package com.erp.backend.domain;

public enum BuchungStatus {
    ENTWURF("Noch nicht gebucht"),
    GEBUCHT("Gebucht und abgeschlossen"),
    STORNIERT("Storniert");

    private final String beschreibung;
    BuchungStatus(String beschreibung) { this.beschreibung = beschreibung; }
    public String getBeschreibung() { return beschreibung; }
}