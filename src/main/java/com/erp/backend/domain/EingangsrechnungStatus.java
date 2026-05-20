package com.erp.backend.domain;

public enum EingangsrechnungStatus {
    ERFASST("Erfasst — noch nicht geprüft"),
    GEPRUEFT("Geprüft — wartet auf Freigabe"),
    FREIGEGEBEN("Freigegeben — bereit zur Zahlung"),
    BEZAHLT("Bezahlt"),
    STORNIERT("Storniert");

    private final String beschreibung;
    EingangsrechnungStatus(String beschreibung) { this.beschreibung = beschreibung; }
    public String getBeschreibung() { return beschreibung; }
}