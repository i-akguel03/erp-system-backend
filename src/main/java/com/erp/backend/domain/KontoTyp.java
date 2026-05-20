package com.erp.backend.domain;

public enum KontoTyp {
    AKTIV("Aktivkonto — Vermögen"),
    PASSIV("Passivkonto — Schulden und Eigenkapital"),
    AUFWAND("Aufwandskonto — Kosten"),
    ERTRAG("Ertragskonto — Einnahmen");

    private final String beschreibung;

    KontoTyp(String beschreibung) { this.beschreibung = beschreibung; }
    public String getBeschreibung() { return beschreibung; }
}