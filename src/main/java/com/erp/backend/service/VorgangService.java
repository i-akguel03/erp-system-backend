// ===============================================================================================
// VORGANG SERVICE
// ===============================================================================================

package com.erp.backend.service;

import com.erp.backend.domain.*;
import com.erp.backend.repository.VorgangRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class VorgangService {

    private static final Logger logger = LoggerFactory.getLogger(VorgangService.class);

    private final VorgangRepository vorgangRepository;
    private final NumberGeneratorService numberGeneratorService;

    public VorgangService(VorgangRepository vorgangRepository,
                          NumberGeneratorService numberGeneratorService) {
        this.vorgangRepository = vorgangRepository;
        this.numberGeneratorService = numberGeneratorService;
    }

    // ===============================================================================================
    // VORGANG-LIFECYCLE MANAGEMENT
    // ===============================================================================================

    /**
     * Startet einen neuen Vorgang
     */
    public Vorgang starteVorgang(VorgangTyp typ, String titel) {
        return starteVorgang(typ, titel, null, null, false);
    }

    /**
     * Startet einen neuen Vorgang mit Benutzer
     */
    public Vorgang starteVorgang(VorgangTyp typ, String titel, String benutzer) {
        return starteVorgang(typ, titel, null, benutzer, false);
    }

    /**
     * Startet einen neuen automatischen Vorgang
     */
    public Vorgang starteAutomatischenVorgang(VorgangTyp typ, String titel) {
        return starteVorgang(typ, titel, null, "SYSTEM", true);
    }

    /**
     * Vollständiger Vorgang-Start
     */
    public Vorgang starteVorgang(VorgangTyp typ, String titel, String beschreibung,
                                 String benutzer, boolean automatisch) {
        Vorgang vorgang = new Vorgang(typ, titel, beschreibung);
        vorgang.setVorgangsnummer(numberGeneratorService.generateVorgangsnummer());
        vorgang.setAusgeloestVon(benutzer);
        vorgang.setAutomatisch(automatisch);

        vorgang.starten();

        Vorgang gespeicherterVorgang = vorgangRepository.save(vorgang);

        logger.info("Vorgang gestartet: {} - {}", gespeicherterVorgang.getVorgangsnummer(), titel);

        return gespeicherterVorgang;
    }

    /**
     * Schließt einen Vorgang erfolgreich ab
     */
    public void vorgangErfolgreichAbschliessen(UUID vorgangId) {
        vorgangErfolgreichAbschliessen(vorgangId, null, null, null, null);
    }

    /**
     * Schließt einen Vorgang erfolgreich ab mit Statistiken
     */
    public void vorgangErfolgreichAbschliessen(UUID vorgangId, Integer verarbeitet,
                                               Integer erfolgreich, Integer fehler,
                                               java.math.BigDecimal betrag) {
        Vorgang vorgang = vorgangRepository.findById(vorgangId)
                .orElseThrow(() -> new IllegalArgumentException("Vorgang nicht gefunden: " + vorgangId));

        if (verarbeitet != null) {
            vorgang.updateStatistiken(verarbeitet,
                    erfolgreich != null ? erfolgreich : verarbeitet,
                    fehler != null ? fehler : 0);
        }

        if (betrag != null) {
            vorgang.setGesamtbetrag(betrag);
        }

        vorgang.erfolgreichAbschliessen();
        vorgangRepository.save(vorgang);

        logger.info("Vorgang erfolgreich abgeschlossen: {} (Dauer: {} ms)",
                vorgang.getVorgangsnummer(), vorgang.getDauerInMs());
    }

    /**
     * Schließt einen Vorgang mit Fehler ab
     */
    public void vorgangMitFehlerAbschliessen(UUID vorgangId, String fehlerDetails) {
        Vorgang vorgang = vorgangRepository.findById(vorgangId)
                .orElseThrow(() -> new IllegalArgumentException("Vorgang nicht gefunden: " + vorgangId));

        vorgang.mitFehlerAbschliessen(fehlerDetails);
        vorgangRepository.save(vorgang);

        logger.error("Vorgang mit Fehler abgeschlossen: {} - {}",
                vorgang.getVorgangsnummer(), fehlerDetails);
    }

    /**
     * Bricht einen Vorgang ab
     */
    public void vorgangAbbrechen(UUID vorgangId, String grund) {
        Vorgang vorgang = vorgangRepository.findById(vorgangId)
                .orElseThrow(() -> new IllegalArgumentException("Vorgang nicht gefunden: " + vorgangId));

        vorgang.abbrechen(grund);
        vorgangRepository.save(vorgang);

        logger.warn("Vorgang abgebrochen: {} - {}", vorgang.getVorgangsnummer(), grund);
    }

    // ===============================================================================================
    // SPEZIELLE FACTORY-METHODEN FÜR HÄUFIGE VORGÄNGE
    // ===============================================================================================

    /**
     * Startet einen Rechnungslauf-Vorgang
     */
    public Vorgang starteRechnungslauf(String stichtag, String batchId) {
        Vorgang vorgang = Vorgang.rechnungslauf(stichtag, batchId);
        vorgang.setVorgangsnummer(numberGeneratorService.generateVorgangsnummer());

        return vorgangRepository.save(vorgang);
    }

    /**
     * Startet einen Datenimport-Vorgang
     */
    public Vorgang starteDatenImport(String dateityp, String dateiname, String benutzer) {
        Vorgang vorgang = Vorgang.datenImport(dateityp, dateiname);
        vorgang.setVorgangsnummer(numberGeneratorService.generateVorgangsnummer());
        vorgang.setAusgeloestVon(benutzer);

        return vorgangRepository.save(vorgang);
    }

    /**
     * Startet eine Bulk-Operation
     */
    public Vorgang starteBulkOperation(String operation, int anzahlDatensaetze, String benutzer) {
        Vorgang vorgang = Vorgang.bulkOperation(operation, anzahlDatensaetze);
        vorgang.setVorgangsnummer(numberGeneratorService.generateVorgangsnummer());
        vorgang.setAusgeloestVon(benutzer);

        return vorgangRepository.save(vorgang);
    }

    // ===============================================================================================
    // VERKNÜPFUNG MIT ENTITÄTEN
    // ===============================================================================================

    /**
     * Verknüpft eine Rechnung mit einem Vorgang
     */
    public void verknuepfeRechnung(UUID vorgangId, Invoice rechnung) {
        Vorgang vorgang = vorgangRepository.findById(vorgangId)
                .orElseThrow(() -> new IllegalArgumentException("Vorgang nicht gefunden: " + vorgangId));

        vorgang.addRechnung(rechnung);
        vorgangRepository.save(vorgang);
    }

    /**
     * Verknüpft einen OpenItem mit einem Vorgang
     */
    public void verknuepfeOpenItem(UUID vorgangId, OpenItem openItem) {
        Vorgang vorgang = vorgangRepository.findById(vorgangId)
                .orElseThrow(() -> new IllegalArgumentException("Vorgang nicht gefunden: " + vorgangId));

        vorgang.addOpenItem(openItem);
        vorgangRepository.save(vorgang);
    }

    /**
     * Verknüpft eine DueSchedule mit einem Vorgang
     */
    public void verknuepfeDueSchedule(UUID vorgangId, DueSchedule dueSchedule) {
        Vorgang vorgang = vorgangRepository.findById(vorgangId)
                .orElseThrow(() -> new IllegalArgumentException("Vorgang nicht gefunden: " + vorgangId));

        vorgang.addDueSchedule(dueSchedule);
        vorgangRepository.save(vorgang);
    }

    // ===============================================================================================
    // ABFRAGE-METHODEN
    // ===============================================================================================

    @Transactional(readOnly = true)
    public Optional<Vorgang> findById(UUID id) {
        return vorgangRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Vorgang> findByVorgangsnummer(String vorgangsnummer) {
        return vorgangRepository.findByVorgangsnummer(vorgangsnummer);
    }

    @Transactional(readOnly = true)
    public List<Vorgang> findByTyp(VorgangTyp typ) {
        return vorgangRepository.findByTyp(typ);
    }

    @Transactional(readOnly = true)
    public List<Vorgang> findByStatus(VorgangStatus status) {
        return vorgangRepository.findByStatus(status);
    }

    @Transactional(readOnly = true)
    public List<Vorgang> findLaufendeVorgaenge() {
        return vorgangRepository.findByStatusIn(List.of(
                VorgangStatus.GESTARTET,
                VorgangStatus.LAUFEND,
                VorgangStatus.PAUSIERT
        ));
    }

    @Transactional(readOnly = true)
    public List<Vorgang> findRecentRechnungslaeufe(int anzahlTage) {
        LocalDateTime seit = LocalDateTime.now().minusDays(anzahlTage);
        return vorgangRepository.findRecentByTyp(VorgangTyp.RECHNUNGSLAUF, seit);
    }

    @Transactional(readOnly = true)
    public Page<Vorgang> findAllePaginated(Pageable pageable) {
        return vorgangRepository.findAllByOrderByStartZeitpunktDesc(pageable);
    }

    // ===============================================================================================
    // STATISTIK-METHODEN
    // ===============================================================================================

    @Transactional(readOnly = true)
    public VorgangStatistik getVorgangStatistik() {
        List<Object[]> statusStats = vorgangRepository.getStatusStatistiken();
        List<Object[]> typStats = vorgangRepository.getTypStatistiken();

        long gesamt = vorgangRepository.count();
        long laufend = vorgangRepository.countByStatus(VorgangStatus.LAUFEND);
        long erfolgreich = vorgangRepository.countByStatus(VorgangStatus.ERFOLGREICH);
        long fehler = vorgangRepository.countByStatus(VorgangStatus.FEHLER);

        // Letzte 30 Tage
        LocalDateTime vor30Tagen = LocalDateTime.now().minusDays(30);
        long rechnungslaeufeLetzte30Tage = vorgangRepository.findRecentByTyp(VorgangTyp.RECHNUNGSLAUF, vor30Tagen).size();

        return new VorgangStatistik(gesamt, laufend, erfolgreich, fehler,
                rechnungslaeufeLetzte30Tage, statusStats, typStats);
    }

    @Transactional(readOnly = true)
    public List<Vorgang> findLanglaufendeVorgaenge(int minutenSchwellwert) {
        LocalDateTime schwellzeit = LocalDateTime.now().minusMinutes(minutenSchwellwert);
        return vorgangRepository.findLanglaufendeVorgaenge(schwellzeit);
    }

    // ===============================================================================================
    // HILFSMETHODEN
    // ===============================================================================================

    /**
     * Convenience-Methode: Try-with-resources Pattern für Vorgang-Management
     */
    public <T> T executeWithVorgang(VorgangTyp typ, String titel, VorgangExecutor<T> executor) {
        Vorgang vorgang = starteAutomatischenVorgang(typ, titel);

        try {
            T result = executor.execute(vorgang);
            vorgangErfolgreichAbschliessen(vorgang.getId());
            return result;
        } catch (Exception e) {
            vorgangMitFehlerAbschliessen(vorgang.getId(), e.getMessage());
            throw new RuntimeException("Vorgang fehlgeschlagen: " + titel, e);
        }
    }

    /**
     * Functional Interface für Vorgang-Execution
     */
    @FunctionalInterface
    public interface VorgangExecutor<T> {
        T execute(Vorgang vorgang) throws Exception;
    }

    // ===============================================================================================
    // CLEANUP UND WARTUNG
    // ===============================================================================================

    /**
     * Bereinigt alte Vorgänge (älter als X Tage)
     */
    @Transactional
    public int bereinigeAlteVorgaenge(int tageAlt, boolean nurErfolgreicheLoeschen) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(tageAlt);

        List<Vorgang> zuLoeschende;

        if (nurErfolgreicheLoeschen) {
            zuLoeschende = vorgangRepository.findAll().stream()
                    .filter(v -> v.getStartZeitpunkt().isBefore(cutoff))
                    .filter(v -> v.getStatus() == VorgangStatus.ERFOLGREICH)
                    .filter(v -> v.getRechnungen().isEmpty()) // Keine verknüpften Rechnungen
                    .toList();
        } else {
            zuLoeschende = vorgangRepository.findAll().stream()
                    .filter(v -> v.getStartZeitpunkt().isBefore(cutoff))
                    .filter(v -> v.getStatus().istAbgeschlossen())
                    .toList();
        }

        int anzahlGeloescht = zuLoeschende.size();
        vorgangRepository.deleteAll(zuLoeschende);

        logger.info("Bereinigung: {} alte Vorgänge gelöscht (älter als {} Tage)", anzahlGeloescht, tageAlt);

        return anzahlGeloescht;
    }

    /**
     * Korrigiert hängengebliebene Vorgänge
     */
    @Transactional
    public int korrigiereHaengengebliebeneVorgaenge(int stundenSchwellwert) {
        LocalDateTime schwellzeit = LocalDateTime.now().minusHours(stundenSchwellwert);

        List<Vorgang> haengende = vorgangRepository.findLanglaufendeVorgaenge(schwellzeit);

        for (Vorgang vorgang : haengende) {
            vorgang.mitFehlerAbschliessen("Automatisch beendet: Überschritt Zeitlimit von " + stundenSchwellwert + " Stunden");
            vorgangRepository.save(vorgang);
        }

        logger.warn("Korrektur: {} hängengebliebene Vorgänge beendet (länger als {} Stunden)",
                haengende.size(), stundenSchwellwert);

        return haengende.size();
    }

    // ===============================================================================================
    // STATISTIK-DTO
    // ===============================================================================================

    public static class VorgangStatistik {
        private final long gesamtVorgaenge;
        private final long laufendeVorgaenge;
        private final long erfolgreicheVorgaenge;
        private final long fehlerhafteVorgaenge;
        private final long rechnungslaeufeLetzte30Tage;
        private final List<Object[]> statusVerteilung;
        private final List<Object[]> typVerteilung;

        public VorgangStatistik(long gesamt, long laufend, long erfolgreich, long fehler,
                                long rechnungslaeufe, List<Object[]> statusStats, List<Object[]> typStats) {
            this.gesamtVorgaenge = gesamt;
            this.laufendeVorgaenge = laufend;
            this.erfolgreicheVorgaenge = erfolgreich;
            this.fehlerhafteVorgaenge = fehler;
            this.rechnungslaeufeLetzte30Tage = rechnungslaeufe;
            this.statusVerteilung = statusStats;
            this.typVerteilung = typStats;
        }

        // Getters
        public long getGesamtVorgaenge() { return gesamtVorgaenge; }
        public long getLaufendeVorgaenge() { return laufendeVorgaenge; }
        public long getErfolgreicheVorgaenge() { return erfolgreicheVorgaenge; }
        public long getFehlerhafteVorgaenge() { return fehlerhafteVorgaenge; }
        public long getRechnungslaeufeLetzte30Tage() { return rechnungslaeufeLetzte30Tage; }
        public List<Object[]> getStatusVerteilung() { return statusVerteilung; }
        public List<Object[]> getTypVerteilung() { return typVerteilung; }

        public double getErfolgsquote() {
            if (gesamtVorgaenge == 0) return 0.0;
            return (erfolgreicheVorgaenge * 100.0) / gesamtVorgaenge;
        }
    }
}