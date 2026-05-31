package com.erp.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Eine Zeile im Kundenkonto-Auszug (wie Sparkassen-Kontoauszug).
 *
 * Typ "FORDERUNG" = Minus  (Rechnung gestellt, Schuld entsteht)
 * Typ "ZAHLUNG"   = Plus   (Zahlung eingegangen, Schuld reduziert sich)
 *
 * Saldo: negativ = Kunde schuldet noch Geld, 0 = alles ausgeglichen.
 */
public class KontenblattEintragDTO {

    private UUID openItemId;
    private LocalDate datum;

    /** "FORDERUNG" (Minus) oder "ZAHLUNG" (Plus) */
    private String typ;

    private String beschreibung;

    /** Betrag immer positiv */
    private BigDecimal betrag;

    /** Vorzeichenbehaftet: negativ bei FORDERUNG, positiv bei ZAHLUNG */
    private BigDecimal bewegung;

    /** Laufender Saldo nach diesem Eintrag (negativ = offene Schuld) */
    private BigDecimal saldo;

    private String rechnungsnummer;
    private String zahlungsart;
    private String zahlungsreferenz;
    private String openItemStatus;

    /** Noch offener Restbetrag des offenen Postens */
    private BigDecimal offenerBetrag;

    public KontenblattEintragDTO() {}

    public UUID getOpenItemId() { return openItemId; }
    public void setOpenItemId(UUID openItemId) { this.openItemId = openItemId; }

    public LocalDate getDatum() { return datum; }
    public void setDatum(LocalDate datum) { this.datum = datum; }

    public String getTyp() { return typ; }
    public void setTyp(String typ) { this.typ = typ; }

    public String getBeschreibung() { return beschreibung; }
    public void setBeschreibung(String beschreibung) { this.beschreibung = beschreibung; }

    public BigDecimal getBetrag() { return betrag; }
    public void setBetrag(BigDecimal betrag) { this.betrag = betrag; }

    public BigDecimal getBewegung() { return bewegung; }
    public void setBewegung(BigDecimal bewegung) { this.bewegung = bewegung; }

    public BigDecimal getSaldo() { return saldo; }
    public void setSaldo(BigDecimal saldo) { this.saldo = saldo; }

    public String getRechnungsnummer() { return rechnungsnummer; }
    public void setRechnungsnummer(String rechnungsnummer) { this.rechnungsnummer = rechnungsnummer; }

    public String getZahlungsart() { return zahlungsart; }
    public void setZahlungsart(String zahlungsart) { this.zahlungsart = zahlungsart; }

    public String getZahlungsreferenz() { return zahlungsreferenz; }
    public void setZahlungsreferenz(String zahlungsreferenz) { this.zahlungsreferenz = zahlungsreferenz; }

    public String getOpenItemStatus() { return openItemStatus; }
    public void setOpenItemStatus(String openItemStatus) { this.openItemStatus = openItemStatus; }

    public BigDecimal getOffenerBetrag() { return offenerBetrag; }
    public void setOffenerBetrag(BigDecimal offenerBetrag) { this.offenerBetrag = offenerBetrag; }
}
