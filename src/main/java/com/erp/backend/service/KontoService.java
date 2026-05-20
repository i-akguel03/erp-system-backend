package com.erp.backend.service;

import com.erp.backend.domain.Konto;
import com.erp.backend.domain.KontoTyp;
import com.erp.backend.dto.KontoDTO;
import com.erp.backend.exception.BusinessLogicException;
import com.erp.backend.exception.DuplicateResourceException;
import com.erp.backend.exception.ResourceNotFoundException;
import com.erp.backend.repository.KontoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class KontoService {

    private static final Logger logger = LoggerFactory.getLogger(KontoService.class);

    private final KontoRepository kontoRepository;

    public KontoService(KontoRepository kontoRepository) {
        this.kontoRepository = kontoRepository;
    }

    @Transactional(readOnly = true)
    public List<KontoDTO> findAll() {
        return kontoRepository.findAll().stream()
                .map(KontoDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public KontoDTO findByKontonummer(Long kontonummer) {
        return kontoRepository.findById(kontonummer)
                .map(KontoDTO::fromEntity)
                .orElseThrow(() -> new ResourceNotFoundException("Konto nicht gefunden: " + kontonummer));
    }

    public KontoDTO erstellen(KontoDTO dto) {
        if (kontoRepository.existsByKontonummer(dto.getKontonummer())) {
            throw new DuplicateResourceException("Kontonummer bereits vergeben: " + dto.getKontonummer());
        }
        Konto konto = new Konto(dto.getKontonummer(), dto.getBezeichnung(), dto.getKontoTyp(), dto.getKontoKlasse());
        konto.setSammelkonto(dto.isSammelkonto());
        konto.setBeschreibungLang(dto.getBeschreibungLang());
        Konto saved = kontoRepository.save(konto);
        logger.info("Konto angelegt: {} {}", saved.getKontonummer(), saved.getBezeichnung());
        return KontoDTO.fromEntity(saved);
    }

    public KontoDTO aktualisieren(Long kontonummer, KontoDTO dto) {
        Konto konto = kontoRepository.findById(kontonummer)
                .orElseThrow(() -> new ResourceNotFoundException("Konto nicht gefunden: " + kontonummer));
        konto.setBezeichnung(dto.getBezeichnung());
        konto.setKontoTyp(dto.getKontoTyp());
        konto.setKontoKlasse(dto.getKontoKlasse());
        konto.setSammelkonto(dto.isSammelkonto());
        konto.setAktiv(dto.isAktiv());
        konto.setBeschreibungLang(dto.getBeschreibungLang());
        return KontoDTO.fromEntity(kontoRepository.save(konto));
    }

    /**
     * Legt den Standard-Kontenplan nach SKR04 an.
     * Kann mehrfach aufgerufen werden — bereits vorhandene Konten werden übersprungen.
     */
    public int initSkr04() {
        List<Konto> standardKonten = List.of(
            // === Klasse 0: Anlagevermögen ===
            new Konto(100L,  "Immaterielle Vermögensgegenstände",   KontoTyp.AKTIV, "Anlagevermögen"),
            new Konto(200L,  "Sachanlagen",                          KontoTyp.AKTIV, "Anlagevermögen"),
            new Konto(400L,  "Finanzanlagen",                        KontoTyp.AKTIV, "Anlagevermögen"),
            // === Klasse 1: Umlaufvermögen ===
            new Konto(1000L, "Vorräte",                              KontoTyp.AKTIV, "Umlaufvermögen"),
            new Konto(1200L, "Forderungen aus Lieferungen u. Leistungen", KontoTyp.AKTIV, "Umlaufvermögen"),
            new Konto(1400L, "Sonstige Forderungen",                 KontoTyp.AKTIV, "Umlaufvermögen"),
            new Konto(1600L, "Kassenbestand",                        KontoTyp.AKTIV, "Umlaufvermögen"),
            new Konto(1800L, "Bankkonten",                           KontoTyp.AKTIV, "Umlaufvermögen"),
            // === Klasse 2: Eigenkapital ===
            new Konto(2000L, "Eigenkapital",                         KontoTyp.PASSIV, "Eigenkapital"),
            new Konto(2100L, "Jahresüberschuss / -fehlbetrag",       KontoTyp.PASSIV, "Eigenkapital"),
            // === Klasse 3: Verbindlichkeiten ===
            new Konto(3000L, "Verbindlichkeiten aus Lieferungen u. Leistungen", KontoTyp.PASSIV, "Verbindlichkeiten"),
            new Konto(3300L, "Sonstige Verbindlichkeiten",           KontoTyp.PASSIV, "Verbindlichkeiten"),
            new Konto(3500L, "Umsatzsteuer-Zahllast",                KontoTyp.PASSIV, "Verbindlichkeiten"),
            // === Klasse 4: Erlöse ===
            new Konto(4000L, "Umsatzerlöse 19% MwSt",               KontoTyp.ERTRAG, "Erlöse"),
            new Konto(4100L, "Umsatzerlöse 7% MwSt",                KontoTyp.ERTRAG, "Erlöse"),
            new Konto(4200L, "Steuerfreie Umsatzerlöse",             KontoTyp.ERTRAG, "Erlöse"),
            new Konto(4830L, "Umsatzsteuer 19%",                     KontoTyp.PASSIV, "Steuern"),
            new Konto(4831L, "Umsatzsteuer 7%",                      KontoTyp.PASSIV, "Steuern"),
            new Konto(4880L, "Vorsteuer 19%",                        KontoTyp.AKTIV,  "Steuern"),
            new Konto(4881L, "Vorsteuer 7%",                         KontoTyp.AKTIV,  "Steuern"),
            // === Klasse 5–7: Aufwendungen ===
            new Konto(5000L, "Wareneinkauf",                         KontoTyp.AUFWAND, "Materialaufwand"),
            new Konto(6000L, "Löhne und Gehälter",                   KontoTyp.AUFWAND, "Personalaufwand"),
            new Konto(6200L, "Abschreibungen",                       KontoTyp.AUFWAND, "Abschreibungen"),
            new Konto(6300L, "Büro- und Betriebskosten",             KontoTyp.AUFWAND, "Sonstige Aufwendungen"),
            new Konto(6400L, "IT- und Softwarekosten",               KontoTyp.AUFWAND, "Sonstige Aufwendungen"),
            new Konto(6500L, "Reisekosten",                          KontoTyp.AUFWAND, "Sonstige Aufwendungen"),
            new Konto(6600L, "Marketing und Werbung",                KontoTyp.AUFWAND, "Sonstige Aufwendungen"),
            new Konto(6800L, "Sonstige betriebliche Aufwendungen",   KontoTyp.AUFWAND, "Sonstige Aufwendungen"),
            new Konto(7600L, "Zinsaufwendungen",                     KontoTyp.AUFWAND, "Finanzergebnis"),
            new Konto(7400L, "Zinserträge",                          KontoTyp.ERTRAG,  "Finanzergebnis")
        );

        // Sammelkonten markieren
        int created = 0;
        for (Konto k : standardKonten) {
            if (k.getKontonummer() == 1200L || k.getKontonummer() == 3000L) {
                k.setSammelkonto(true);
            }
            if (!kontoRepository.existsByKontonummer(k.getKontonummer())) {
                kontoRepository.save(k);
                created++;
            }
        }
        logger.info("SKR04 initialisiert: {} neue Konten angelegt", created);
        return created;
    }
}