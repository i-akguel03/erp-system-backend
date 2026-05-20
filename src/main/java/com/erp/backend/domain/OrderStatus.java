package com.erp.backend.domain;

public enum OrderStatus {
    ENTWURF("Entwurf"),
    BESTAETIGT("Bestätigt"),
    IN_LIEFERUNG("In Lieferung"),
    GELIEFERT("Geliefert"),
    ABGERECHNET("Abgerechnet"),
    STORNIERT("Storniert");

    private final String bezeichnung;

    OrderStatus(String bezeichnung) {
        this.bezeichnung = bezeichnung;
    }

    public String getBezeichnung() {
        return bezeichnung;
    }
}