package com.erp.backend.service;

import com.erp.backend.domain.*;
import com.erp.backend.dto.BuchungssatzDTO;
import com.erp.backend.dto.KontoDTO;
import com.erp.backend.exception.BusinessLogicException;
import com.erp.backend.exception.ResourceNotFoundException;
import com.erp.backend.repository.BuchungssatzRepository;
import com.erp.backend.repository.KontoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Kernservice der Finanzbuchhaltung.
 *
 * Verantwortlichkeiten:
 * - Automatische GL-Buchungen bei Rechnungen und Zahlungen
 * - Manuelle Buchungssätze
 * - Kontosaldoberechnung
 * - Storno-Buchungen
 * - GuV-Übersicht
 */
@Service
@Transactional
public class BuchhaltungService {

    private static final Logger logger = LoggerFactory.getLogger(BuchhaltungService.class);

    // Standard-Kontonummern (SKR04)
    public static final long KONTO_FORDERUNGEN    = 1200L; // Debitoren (AR)
    public static final long KONTO_BANK           = 1800L; // Bankkonten
    public static final long KONTO_VERBINDLICHK   = 3000L; // Kreditoren (AP)
    public static final long KONTO_UMSATZ_19      = 4000L; // Umsatzerlöse 19%
    public static final long KONTO_UMSATZ_7       = 4100L; // Umsatzerlöse 7%
    public static final long KONTO_UST_19         = 4830L; // Umsatzsteuer 19%
    public static final long KONTO_UST_7          = 4831L; // Umsatzsteuer 7%
    public static final long KONTO_VORSTEUER_19   = 4880L; // Vorsteuer 19%
    public static final long KONTO_VORSTEUER_7    = 4881L; // Vorsteuer 7%

    private final BuchungssatzRepository buchungssatzRepository;
    private final KontoRepository kontoRepository;

    public BuchhaltungService(BuchungssatzRepository buchungssatzRepository,
                               KontoRepository kontoRepository) {
        this.buchungssatzRepository = buchungssatzRepository;
        this.kontoRepository = kontoRepository;
    }

    // =========================================================================
    // Automatische Buchungen — aufgerufen von InvoiceService / OpenItemService
    // =========================================================================

    /**
     * Bucht eine Ausgangsrechnung ins Hauptbuch.
     * SOLL 1200 Forderungen  = Bruttobetrag
     * HABEN 4000 Umsatzerlöse = Nettobetrag
     * HABEN 4830 Umsatzsteuer = Steuerbetrag
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<BuchungssatzDTO> bucheRechnung(Invoice invoice) {
        if (!kontoExistiert(KONTO_FORDERUNGEN) || !kontoExistiert(KONTO_UMSATZ_19)) {
            logger.warn("GL-Buchung für Rechnung {} übersprungen — Kontenplan nicht initialisiert",
                    invoice.getInvoiceNumber());
            return Optional.empty();
        }

        BigDecimal netto = invoice.getSubtotal() != null ? invoice.getSubtotal() : BigDecimal.ZERO;
        BigDecimal steuer = invoice.getTaxAmount() != null ? invoice.getTaxAmount() : BigDecimal.ZERO;
        BigDecimal brutto = invoice.getTotalAmount() != null ? invoice.getTotalAmount() : netto.add(steuer);

        if (brutto.compareTo(BigDecimal.ZERO) == 0) return Optional.empty();

        Buchungssatz buchung = neuBuchungssatz(
                BelegTyp.RECHNUNG,
                invoice.getId().toString(),
                invoice.getInvoiceNumber(),
                "Rechnung " + invoice.getInvoiceNumber() + " an " +
                        (invoice.getCustomer() != null ? invoice.getCustomer().getName() : "")
        );

        // SOLL: Forderungen (Brutto)
        buchung.addPosition(new Buchungsposition(
                getKonto(KONTO_FORDERUNGEN), BuchungsTyp.SOLL, brutto,
                "Forderung aus Rechnung " + invoice.getInvoiceNumber()));

        // HABEN: Umsatzerlöse (Netto) — wähle 7% oder 19% Konto je nach Steuersatz
        long umsatzKonto = ermiттleUmsatzKonto(invoice.getTaxRate());
        if (!kontoExistiert(umsatzKonto)) {
            umsatzKonto = KONTO_UMSATZ_19; // Fallback auf 19%-Konto
        }
        buchung.addPosition(new Buchungsposition(
                getKonto(umsatzKonto), BuchungsTyp.HABEN, netto,
                "Umsatzerlöse Rechnung " + invoice.getInvoiceNumber()));

        // HABEN: Umsatzsteuer (nur wenn Steuer > 0)
        if (steuer.compareTo(BigDecimal.ZERO) > 0) {
            long ustKonto = istSieben(invoice.getTaxRate()) ? KONTO_UST_7 : KONTO_UST_19;
            if (kontoExistiert(ustKonto)) {
                buchung.addPosition(new Buchungsposition(
                        getKonto(ustKonto), BuchungsTyp.HABEN, steuer,
                        "Umsatzsteuer Rechnung " + invoice.getInvoiceNumber()));
            } else {
                // Steuer in Umsatzerlöse verrechnen falls USt-Konto fehlt
                buchung.getPositionen().get(1).setBetrag(brutto);
            }
        }

        Buchungssatz saved = buchungssatzRepository.save(buchung);
        logger.info("GL-Buchung für Rechnung {}: {} ({})", invoice.getInvoiceNumber(),
                saved.getBuchungsnummer(), brutto);
        return Optional.of(BuchungssatzDTO.fromEntity(saved));
    }

    /**
     * Bucht einen Zahlungseingang auf eine Forderung.
     * SOLL 1800 Bank            = Betrag
     * HABEN 1200 Forderungen   = Betrag
     */
    public Optional<BuchungssatzDTO> bucheZahlungseingang(OpenItem openItem, BigDecimal betrag) {
        if (!kontoExistiert(KONTO_BANK) || !kontoExistiert(KONTO_FORDERUNGEN)) {
            logger.warn("GL-Buchung Zahlung übersprungen — Kontenplan nicht initialisiert");
            return Optional.empty();
        }

        String rechnungsNr = openItem.getInvoice() != null
                ? openItem.getInvoice().getInvoiceNumber() : "unbekannt";

        Buchungssatz buchung = neuBuchungssatz(
                BelegTyp.ZAHLUNG_EINGANG,
                openItem.getId().toString(),
                rechnungsNr,
                "Zahlungseingang zu Rechnung " + rechnungsNr
        );

        buchung.addPosition(new Buchungsposition(
                getKonto(KONTO_BANK), BuchungsTyp.SOLL, betrag,
                "Zahlungseingang Rechnungs-Nr. " + rechnungsNr));
        buchung.addPosition(new Buchungsposition(
                getKonto(KONTO_FORDERUNGEN), BuchungsTyp.HABEN, betrag,
                "Ausgleich Forderung Rechnungs-Nr. " + rechnungsNr));

        Buchungssatz saved = buchungssatzRepository.save(buchung);
        logger.info("GL-Buchung Zahlung für Rechnung {}: {} ({})", rechnungsNr,
                saved.getBuchungsnummer(), betrag);
        return Optional.of(BuchungssatzDTO.fromEntity(saved));
    }

    /**
     * Bucht eine Eingangsrechnung (Kreditorenbuchhaltung).
     * SOLL Aufwandskonto (z.B. 6300) = Netto
     * SOLL 4880 Vorsteuer            = Steuer
     * HABEN 3000 Verbindlichkeiten   = Brutto
     */
    public Optional<BuchungssatzDTO> bucheEingangsrechnung(
            UUID eingangsrechnungId, String liefRechnungsNr, String lieferantName,
            BigDecimal netto, BigDecimal steuer, BigDecimal brutto,
            long aufwandskontoNr) {

        if (!kontoExistiert(KONTO_VERBINDLICHK) || !kontoExistiert(aufwandskontoNr)) {
            logger.warn("GL-Buchung Eingangsrechnung übersprungen — Konten fehlen");
            return Optional.empty();
        }

        Buchungssatz buchung = neuBuchungssatz(
                BelegTyp.EINGANGSRECHNUNG,
                eingangsrechnungId.toString(),
                liefRechnungsNr,
                "Eingangsrechnung " + liefRechnungsNr + " von " + lieferantName
        );

        buchung.addPosition(new Buchungsposition(
                getKonto(aufwandskontoNr), BuchungsTyp.SOLL, netto,
                "Aufwand aus Eingangsrechnung " + liefRechnungsNr));

        if (steuer.compareTo(BigDecimal.ZERO) > 0 && kontoExistiert(KONTO_VORSTEUER_19)) {
            buchung.addPosition(new Buchungsposition(
                    getKonto(KONTO_VORSTEUER_19), BuchungsTyp.SOLL, steuer,
                    "Vorsteuer aus Eingangsrechnung " + liefRechnungsNr));
        }

        buchung.addPosition(new Buchungsposition(
                getKonto(KONTO_VERBINDLICHK), BuchungsTyp.HABEN,
                steuer.compareTo(BigDecimal.ZERO) > 0 ? brutto : netto,
                "Verbindlichkeit gegenüber " + lieferantName));

        Buchungssatz saved = buchungssatzRepository.save(buchung);
        logger.info("GL-Buchung Eingangsrechnung {}: {}", liefRechnungsNr, saved.getBuchungsnummer());
        return Optional.of(BuchungssatzDTO.fromEntity(saved));
    }

    /**
     * Bucht eine Zahlung an Lieferanten.
     * SOLL 3000 Verbindlichkeiten = Betrag
     * HABEN 1800 Bank             = Betrag
     */
    public Optional<BuchungssatzDTO> bucheZahlungsausgang(
            UUID eingangsrechnungId, String liefRechnungsNr, String lieferantName, BigDecimal betrag) {

        if (!kontoExistiert(KONTO_BANK) || !kontoExistiert(KONTO_VERBINDLICHK)) {
            logger.warn("GL-Buchung Zahlungsausgang übersprungen — Konten fehlen");
            return Optional.empty();
        }

        Buchungssatz buchung = neuBuchungssatz(
                BelegTyp.ZAHLUNG_AUSGANG,
                eingangsrechnungId.toString(),
                liefRechnungsNr,
                "Zahlung an " + lieferantName + " für Rechnung " + liefRechnungsNr
        );

        buchung.addPosition(new Buchungsposition(
                getKonto(KONTO_VERBINDLICHK), BuchungsTyp.SOLL, betrag,
                "Ausgleich Verbindlichkeit " + liefRechnungsNr));
        buchung.addPosition(new Buchungsposition(
                getKonto(KONTO_BANK), BuchungsTyp.HABEN, betrag,
                "Zahlung an " + lieferantName));

        Buchungssatz saved = buchungssatzRepository.save(buchung);
        return Optional.of(BuchungssatzDTO.fromEntity(saved));
    }

    // =========================================================================
    // CRUD + Abfragen
    // =========================================================================

    @Transactional(readOnly = true)
    public List<BuchungssatzDTO> findAll() {
        return buchungssatzRepository.findAll().stream()
                .map(BuchungssatzDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BuchungssatzDTO findById(UUID id) {
        return buchungssatzRepository.findById(id)
                .map(BuchungssatzDTO::fromEntity)
                .orElseThrow(() -> new ResourceNotFoundException("Buchungssatz nicht gefunden: " + id));
    }

    @Transactional(readOnly = true)
    public List<BuchungssatzDTO> findByReferenz(String referenzId) {
        return buchungssatzRepository.findByBelegReferenzId(referenzId).stream()
                .map(BuchungssatzDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BuchungssatzDTO> findByKonto(Long kontonummer) {
        return buchungssatzRepository.findByKonto(kontonummer).stream()
                .map(BuchungssatzDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public KontoDTO kontoMitSaldo(Long kontonummer, int jahr) {
        Konto konto = kontoRepository.findById(kontonummer)
                .orElseThrow(() -> new ResourceNotFoundException("Konto nicht gefunden: " + kontonummer));

        BigDecimal soll = buchungssatzRepository.sumSollByKontoAndJahr(kontonummer, jahr);
        BigDecimal haben = buchungssatzRepository.sumHabenByKontoAndJahr(kontonummer, jahr);

        KontoDTO dto = KontoDTO.fromEntity(konto);
        BigDecimal saldo = switch (konto.getKontoTyp()) {
            case AKTIV, AUFWAND -> soll.subtract(haben);
            case PASSIV, ERTRAG -> haben.subtract(soll);
        };
        dto.setSaldo(saldo);
        return dto;
    }

    /**
     * Storniert einen Buchungssatz (dreht alle Positionen um).
     */
    public BuchungssatzDTO stornieren(UUID buchungssatzId) {
        Buchungssatz original = buchungssatzRepository.findById(buchungssatzId)
                .orElseThrow(() -> new ResourceNotFoundException("Buchungssatz nicht gefunden: " + buchungssatzId));

        if (BuchungStatus.STORNIERT.equals(original.getStatus())) {
            throw new BusinessLogicException("Buchungssatz ist bereits storniert");
        }

        Buchungssatz storno = neuBuchungssatz(
                original.getBelegTyp(),
                original.getBelegReferenzId(),
                original.getBelegReferenzNummer(),
                "STORNO: " + original.getBeschreibung()
        );
        storno.setStornoVonId(original.getId());

        for (Buchungsposition pos : original.getPositionen()) {
            BuchungsTyp umgekehrt = BuchungsTyp.SOLL.equals(pos.getBuchungsTyp())
                    ? BuchungsTyp.HABEN : BuchungsTyp.SOLL;
            storno.addPosition(new Buchungsposition(pos.getKonto(), umgekehrt, pos.getBetrag(),
                    "STORNO: " + pos.getBeschreibung()));
        }

        original.setStatus(BuchungStatus.STORNIERT);
        buchungssatzRepository.save(original);
        Buchungssatz saved = buchungssatzRepository.save(storno);
        logger.info("Buchungssatz {} storniert → {}", original.getBuchungsnummer(), saved.getBuchungsnummer());
        return BuchungssatzDTO.fromEntity(saved);
    }

    /**
     * Vereinfachte GuV-Übersicht für ein Geschäftsjahr.
     */
    @Transactional(readOnly = true)
    public Map<String, BigDecimal> guvUebersicht(int jahr) {
        Map<String, BigDecimal> ergebnis = new LinkedHashMap<>();

        List<Konto> ertragskonten = kontoRepository.findByKontoTyp(KontoTyp.ERTRAG);
        List<Konto> aufwandskonten = kontoRepository.findByKontoTyp(KontoTyp.AUFWAND);

        BigDecimal gesamtErtrag = BigDecimal.ZERO;
        for (Konto k : ertragskonten) {
            BigDecimal saldo = buchungssatzRepository.sumHabenByKontoAndJahr(k.getKontonummer(), jahr)
                    .subtract(buchungssatzRepository.sumSollByKontoAndJahr(k.getKontonummer(), jahr));
            if (saldo.compareTo(BigDecimal.ZERO) != 0) {
                ergebnis.put(k.getKontonummer() + " " + k.getBezeichnung(), saldo);
                gesamtErtrag = gesamtErtrag.add(saldo);
            }
        }
        ergebnis.put("GESAMT_ERTRAG", gesamtErtrag);

        BigDecimal gesamtAufwand = BigDecimal.ZERO;
        for (Konto k : aufwandskonten) {
            BigDecimal saldo = buchungssatzRepository.sumSollByKontoAndJahr(k.getKontonummer(), jahr)
                    .subtract(buchungssatzRepository.sumHabenByKontoAndJahr(k.getKontonummer(), jahr));
            if (saldo.compareTo(BigDecimal.ZERO) != 0) {
                ergebnis.put(k.getKontonummer() + " " + k.getBezeichnung(), saldo.negate());
                gesamtAufwand = gesamtAufwand.add(saldo);
            }
        }
        ergebnis.put("GESAMT_AUFWAND", gesamtAufwand.negate());
        ergebnis.put("ERGEBNIS", gesamtErtrag.subtract(gesamtAufwand));

        return ergebnis;
    }

    // =========================================================================
    // Hilfsmethoden
    // =========================================================================

    private Buchungssatz neuBuchungssatz(BelegTyp typ, String refId, String refNr, String beschreibung) {
        Buchungssatz b = new Buchungssatz();
        b.setBuchungsnummer(generiereBuchungsnummer());
        b.setBelegTyp(typ);
        b.setBelegReferenzId(refId);
        b.setBelegReferenzNummer(refNr);
        b.setBeschreibung(beschreibung);
        b.setBuchungsDatum(LocalDate.now());
        b.setGebuchtAm(LocalDateTime.now());
        b.setStatus(BuchungStatus.GEBUCHT);
        return b;
    }

    private String generiereBuchungsnummer() {
        String prefix = "BU-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM")) + "-";
        String nr;
        do {
            nr = prefix + String.format("%06d", (long) (Math.random() * 999999) + 1);
        } while (buchungssatzRepository.existsByBuchungsnummer(nr));
        return nr;
    }

    private Konto getKonto(long kontonummer) {
        return kontoRepository.findById(kontonummer)
                .orElseThrow(() -> new ResourceNotFoundException("Konto " + kontonummer + " nicht gefunden. Bitte SKR04 initialisieren."));
    }

    private boolean kontoExistiert(long kontonummer) {
        return kontoRepository.existsByKontonummer(kontonummer);
    }

    private long ermiттleUmsatzKonto(BigDecimal taxRate) {
        return istSieben(taxRate) ? KONTO_UMSATZ_7 : KONTO_UMSATZ_19;
    }

    private boolean istSieben(BigDecimal taxRate) {
        return taxRate != null && taxRate.compareTo(BigDecimal.valueOf(7)) == 0;
    }
}