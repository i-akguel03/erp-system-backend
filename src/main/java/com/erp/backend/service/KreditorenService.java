package com.erp.backend.service;

import com.erp.backend.domain.*;
import com.erp.backend.exception.BusinessLogicException;
import com.erp.backend.exception.DuplicateResourceException;
import com.erp.backend.exception.ResourceNotFoundException;
import com.erp.backend.repository.EingangsrechnungRepository;
import com.erp.backend.repository.LieferantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class KreditorenService {

    private static final Logger logger = LoggerFactory.getLogger(KreditorenService.class);

    private final LieferantRepository lieferantRepository;
    private final EingangsrechnungRepository eingangsrechnungRepository;
    private final BuchhaltungService buchhaltungService;

    public KreditorenService(LieferantRepository lieferantRepository,
                              EingangsrechnungRepository eingangsrechnungRepository,
                              BuchhaltungService buchhaltungService) {
        this.lieferantRepository = lieferantRepository;
        this.eingangsrechnungRepository = eingangsrechnungRepository;
        this.buchhaltungService = buchhaltungService;
    }

    // =========================================================================
    // Lieferanten
    // =========================================================================

    @Transactional(readOnly = true)
    public List<Lieferant> findAllLieferanten() {
        return lieferantRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Lieferant findLieferantById(UUID id) {
        return lieferantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lieferant nicht gefunden: " + id));
    }

    public Lieferant lieferantAnlegen(Lieferant lieferant) {
        lieferant.setLieferantennummer(generiereLieferantennummer());
        Lieferant saved = lieferantRepository.save(lieferant);
        logger.info("Lieferant angelegt: {} {}", saved.getLieferantennummer(), saved.getName());
        return saved;
    }

    public Lieferant lieferantAktualisieren(UUID id, Lieferant aktualisiert) {
        Lieferant existing = findLieferantById(id);
        existing.setName(aktualisiert.getName());
        existing.setEmail(aktualisiert.getEmail());
        existing.setTel(aktualisiert.getTel());
        existing.setSteuernummer(aktualisiert.getSteuernummer());
        existing.setUstIdNr(aktualisiert.getUstIdNr());
        existing.setIban(aktualisiert.getIban());
        existing.setBic(aktualisiert.getBic());
        existing.setNotizen(aktualisiert.getNotizen());
        return lieferantRepository.save(existing);
    }

    // =========================================================================
    // Eingangsrechnungen
    // =========================================================================

    @Transactional(readOnly = true)
    public List<Eingangsrechnung> findAllEingangsrechnungen() {
        return eingangsrechnungRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Eingangsrechnung findEingangsrechnungById(UUID id) {
        return eingangsrechnungRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Eingangsrechnung nicht gefunden: " + id));
    }

    @Transactional(readOnly = true)
    public List<Eingangsrechnung> findUeberfaellig() {
        return eingangsrechnungRepository.findUeberfaellig(LocalDate.now());
    }

    public Eingangsrechnung erfassen(UUID lieferantId, String liefRechnungsNr,
                                     LocalDate rechnungsDatum, LocalDate faelligDatum,
                                     BigDecimal nettobetrag, BigDecimal steuersatz,
                                     Long aufwandskontoNr, String notizen) {
        Lieferant lieferant = findLieferantById(lieferantId);

        BigDecimal steuer = berechnesteuer(nettobetrag, steuersatz);
        BigDecimal brutto = nettobetrag.add(steuer);

        Eingangsrechnung er = new Eingangsrechnung();
        er.setEingangsrechnungsnummer(generiereEingangsrechnungsnummer());
        er.setLieferantenRechnungsnummer(liefRechnungsNr);
        er.setLieferant(lieferant);
        er.setRechnungsDatum(rechnungsDatum);
        er.setFaelligDatum(faelligDatum);
        er.setNettobetrag(nettobetrag);
        er.setSteuersatz(steuersatz != null ? steuersatz : BigDecimal.ZERO);
        er.setSteuerbetrag(steuer);
        er.setBruttobetrag(brutto);
        if (aufwandskontoNr == null) {
            throw new BusinessLogicException("Aufwandskonto ist Pflichtfeld und muss angegeben werden.");
        }
        er.setAufwandskontoNr(aufwandskontoNr);
        er.setNotizen(notizen);
        er.setStatus(EingangsrechnungStatus.ERFASST);

        Eingangsrechnung saved = eingangsrechnungRepository.save(er);
        logger.info("Eingangsrechnung erfasst: {} von {}", saved.getEingangsrechnungsnummer(), lieferant.getName());
        return saved;
    }

    public Eingangsrechnung freigeben(UUID id) {
        Eingangsrechnung er = findEingangsrechnungById(id);

        if (!EingangsrechnungStatus.GEPRUEFT.equals(er.getStatus()) &&
                !EingangsrechnungStatus.ERFASST.equals(er.getStatus())) {
            throw new BusinessLogicException("Eingangsrechnung kann nicht freigegeben werden — Status: " + er.getStatus());
        }

        er.setStatus(EingangsrechnungStatus.FREIGEGEBEN);
        Eingangsrechnung saved = eingangsrechnungRepository.save(er);

        // GL-Buchung
        long aufwandsKonto = er.getAufwandskontoNr() != null
                ? er.getAufwandskontoNr()
                : BuchhaltungService.KONTO_VERBINDLICHK;

        buchhaltungService.bucheEingangsrechnung(
                er.getId(),
                er.getLieferantenRechnungsnummer() != null ? er.getLieferantenRechnungsnummer() : er.getEingangsrechnungsnummer(),
                er.getLieferant().getName(),
                er.getNettobetrag(),
                er.getSteuerbetrag(),
                er.getBruttobetrag(),
                aufwandsKonto,
                er.getSteuersatz()
        ).ifPresent(b -> {
            saved.setBuchungssatzId(UUID.fromString(b.getId().toString()));
            eingangsrechnungRepository.save(saved);
        });

        logger.info("Eingangsrechnung {} freigegeben und gebucht", saved.getEingangsrechnungsnummer());
        return saved;
    }

    public Eingangsrechnung bezahlen(UUID id, String zahlungsreferenz) {
        Eingangsrechnung er = findEingangsrechnungById(id);

        if (EingangsrechnungStatus.BEZAHLT.equals(er.getStatus())) {
            throw new BusinessLogicException("Eingangsrechnung ist bereits bezahlt");
        }
        if (EingangsrechnungStatus.STORNIERT.equals(er.getStatus())) {
            throw new BusinessLogicException("Stornierte Eingangsrechnung kann nicht bezahlt werden");
        }

        er.setStatus(EingangsrechnungStatus.BEZAHLT);
        er.setGezahltAm(LocalDate.now());
        er.setZahlungsreferenz(zahlungsreferenz);
        Eingangsrechnung saved = eingangsrechnungRepository.save(er);

        // GL-Buchung: Verbindlichkeit → Bank
        buchhaltungService.bucheZahlungsausgang(
                er.getId(),
                er.getEingangsrechnungsnummer(),
                er.getLieferant().getName(),
                er.getBruttobetrag()
        );

        logger.info("Eingangsrechnung {} bezahlt ({})", saved.getEingangsrechnungsnummer(), er.getBruttobetrag());
        return saved;
    }

    // =========================================================================
    // Hilfsmethoden
    // =========================================================================

    private BigDecimal berechnesteuer(BigDecimal netto, BigDecimal steuersatz) {
        if (steuersatz == null || steuersatz.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return netto.multiply(steuersatz).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    private String generiereLieferantennummer() {
        String prefix = "LF-";
        String nr;
        do {
            nr = prefix + String.format("%06d", (long) (Math.random() * 999999) + 1);
        } while (lieferantRepository.existsByLieferantennummer(nr));
        return nr;
    }

    private String generiereEingangsrechnungsnummer() {
        String prefix = "ER-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM")) + "-";
        String nr;
        do {
            nr = prefix + String.format("%05d", (long) (Math.random() * 99999) + 1);
        } while (eingangsrechnungRepository.existsByEingangsrechnungsnummer(nr));
        return nr;
    }
}