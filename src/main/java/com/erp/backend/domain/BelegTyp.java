package com.erp.backend.domain;

public enum BelegTyp {
    RECHNUNG("Ausgangsrechnung"),
    ZAHLUNG_EINGANG("Zahlungseingang vom Kunden"),
    GUTSCHRIFT("Gutschrift / Storno"),
    EINGANGSRECHNUNG("Eingangsrechnung vom Lieferanten"),
    ZAHLUNG_AUSGANG("Zahlung an Lieferanten"),
    MANUELLE_BUCHUNG("Manuelle Buchung");

    private final String beschreibung;
    BelegTyp(String beschreibung) { this.beschreibung = beschreibung; }
    public String getBeschreibung() { return beschreibung; }
}