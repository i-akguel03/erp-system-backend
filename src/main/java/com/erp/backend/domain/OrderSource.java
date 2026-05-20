package com.erp.backend.domain;

public enum OrderSource {
    MANUELL("Manuell im ERP angelegt"),
    ANGEBOT("Aus Angebot konvertiert"),
    WEBSHOP("Externer Webshop"),
    API("Externes System per API");

    private final String beschreibung;

    OrderSource(String beschreibung) {
        this.beschreibung = beschreibung;
    }

    public String getBeschreibung() {
        return beschreibung;
    }
}