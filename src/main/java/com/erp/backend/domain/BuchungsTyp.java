package com.erp.backend.domain;

public enum BuchungsTyp {
    SOLL("Soll — Zugang auf Aktiv-/Aufwandskonto"),
    HABEN("Haben — Zugang auf Passiv-/Ertragskonto");

    private final String beschreibung;
    BuchungsTyp(String beschreibung) { this.beschreibung = beschreibung; }
    public String getBeschreibung() { return beschreibung; }
}