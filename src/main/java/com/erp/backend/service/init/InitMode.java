package com.erp.backend.service.init;

/**
 * VERSCHIEDENE INITIALISIERUNGSMODI
 * <p>
 * Definiert wie "tief" die Initialisierung gehen soll:
 * - BASIC: Nur Grunddaten
 * - CONTRACTS: + Geschäftsdaten
 * - SCHEDULES: + Abrechnungspläne
 * - FULL: + Rechnungslauf
 */
public enum InitMode {
    BASIC("Nur Stammdaten (Adressen, Kunden, Produkte)"),
    CONTRACTS("Bis Verträge und Abonnements"),
    SCHEDULES("Bis Fälligkeitspläne"),
    INVOICES_MANUAL("Mit manuellen Sample-Rechnungen"),
    FULL("Komplett mit automatischem Rechnungslauf"),
    FULL_WITH_BILLING("Komplett mit Rechnungslauf bis Stichtag");

    private final String description;

    InitMode(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
