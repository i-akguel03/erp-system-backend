package com.erp.backend.domain;

public enum LieferscheinStatus {
    AUSSTEHEND("Ausstehend"),
    VERSENDET("Versendet"),
    GELIEFERT("Geliefert"),
    STORNIERT("Storniert");

    private final String bezeichnung;

    LieferscheinStatus(String bezeichnung) {
        this.bezeichnung = bezeichnung;
    }

    public String getBezeichnung() {
        return bezeichnung;
    }
}