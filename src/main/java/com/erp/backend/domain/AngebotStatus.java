package com.erp.backend.domain;

public enum AngebotStatus {
    ENTWURF("Entwurf"),
    VERSANDT("Versandt"),
    ANGENOMMEN("Angenommen"),
    ABGELEHNT("Abgelehnt"),
    ABGELAUFEN("Abgelaufen");

    private final String bezeichnung;

    AngebotStatus(String bezeichnung) {
        this.bezeichnung = bezeichnung;
    }

    public String getBezeichnung() {
        return bezeichnung;
    }
}