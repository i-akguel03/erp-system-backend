package com.erp.backend.domain;

/**
 * Typ des Systemvorgangs
 */
public enum VorgangTyp {
    RECHNUNGSLAUF("Automatischer Rechnungslauf"),
    DATENIMPORT("Import von externen Daten"),
    DATENEXPORT("Export von Daten"),
    BULK_OPERATION("Massenverarbeitung von Datensätzen"),
    STATUS_AENDERUNG("Status-Änderung von Entitäten"),
    ZAHLUNGSEINGANG("Eingang einer Zahlung"),
    ZAHLUNGSAUSGANG("Ausgehende Zahlung/Überweisung"),
    MAHNUNG("Erstellung und Versand von Mahnungen"),
    DATENMIGRATION("Migration von Datenbeständen"),
    SYSTEM_WARTUNG("Systemwartung oder -update"),
    BENUTZER_AKTION("Manuelle Benutzeraktion"),
    AUTOMATISCHE_VERARBEITUNG("Automatische Hintergrundverarbeitung"),
    REPORT_GENERIERUNG("Erstellung von Reports/Berichten"),
    BACKUP("Datensicherung"),
    CLEANUP("Datenbereinigung"),
    INTEGRATION("Integration mit externen Systemen"),
    VALIDIERUNG("Datenvalidierung und -prüfung"),
    STORNO("Stornierung von Geschäftsvorgängen"),
    ARCHIVIERUNG("Archivierung alter Daten"),
    SONSTIGE("Sonstige Vorgänge");

    private final String beschreibung;

    VorgangTyp(String beschreibung) {
        this.beschreibung = beschreibung;
    }

    public String getBeschreibung() {
        return beschreibung;
    }
}