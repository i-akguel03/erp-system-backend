// ===============================================================================================
// INVOICE BATCH ORCHESTRATOR - Hauptklasse für Rechnungsläufe
// ===============================================================================================

// Package-Definition: Organisiert den Code in logische Gruppen
// "batch" ist ein Unterordner von "service" für alle Batch-Processing Klassen
package com.erp.backend.service.batch;

// Imports: Hier holen wir uns alle Klassen die wir brauchen
import com.erp.backend.domain.*;           // Alle Domain-Klassen (Vorgang, etc.)
import com.erp.backend.service.VorgangService;  // Service für Vorgang-Management
import org.slf4j.Logger;                   // Für Logging (Log-Nachrichten)
import org.slf4j.LoggerFactory;           // Factory um Logger zu erstellen
import org.springframework.stereotype.Service; // Spring-Annotation: Markiert als Service
import org.springframework.transaction.annotation.Transactional; // Für Datenbank-Transaktionen

import java.time.LocalDate;               // Für Datums-Operationen

/**
 * HAUPTORCHESTRATOR FÜR RECHNUNGSLÄUFE
 *
 * Diese Klasse koordiniert den gesamten Rechnungslauf-Prozess.
 * Sie ist wie ein "Dirigent" der andere Services orchestriert.
 *
 * Warum eine separate Orchestrator-Klasse?
 * - Trennung der Verantwortlichkeiten (Single Responsibility Principle)
 * - Der Orchestrator koordiniert nur, macht aber keine Detail-Arbeit
 * - Andere Klassen machen die eigentliche Arbeit (Analyzer, Processor, etc.)
 * - Das macht den Code testbarer und wartbarer
 */
@Service  // Spring-Annotation: Sagt Spring "Das ist ein Service - bitte automatisch erstellen"
@Transactional // Spring-Annotation: Alle Methoden laufen in einer Datenbank-Transaktion
public class InvoiceBatchOrchestrator {

    // Logger: Zum Schreiben von Log-Nachrichten (Info, Error, Debug, etc.)
    // 'static final' = Konstante, die von allen Instanzen geteilt wird
    private static final Logger logger = LoggerFactory.getLogger(InvoiceBatchOrchestrator.class);

    // DEPENDENCY INJECTION: Spring gibt uns automatisch die Services die wir brauchen
    // 'private final' = Diese Services können nach der Initialisierung nicht mehr geändert werden
    private final InvoiceBatchProcessor processor;  // Macht die eigentliche Verarbeitung
    private final InvoiceBatchAnalyzer analyzer;   // Analysiert welche Fälligkeiten zu verarbeiten sind
    private final VorgangService vorgangService;   // Verwaltet Vorgänge (für Logging/Tracking)

    /**
     * KONSTRUKTOR
     *
     * Spring ruft diesen Konstruktor auf und gibt automatisch die Services mit.
     * Das nennt man "Constructor Injection" - eine Form von Dependency Injection.
     *
     * Warum Constructor Injection?
     * - Macht die Abhängigkeiten explizit sichtbar
     * - Garantiert dass alle Services verfügbar sind
     * - Macht die Klasse testbarer (man kann Mock-Services übergeben)
     */
    public InvoiceBatchOrchestrator(InvoiceBatchProcessor processor,
                                    InvoiceBatchAnalyzer analyzer,
                                    VorgangService vorgangService) {
        this.processor = processor;
        this.analyzer = analyzer;
        this.vorgangService = vorgangService;
    }

    /**
     * ÖFFENTLICHE API-METHODE (Standard-Version)
     *
     * Das ist die einfache Version der Methode die von außen aufgerufen wird.
     * Sie ruft intern die erweiterte Version mit Standard-Parametern auf.
     *
     * @param billingDate Das Datum bis zu dem abgerechnet werden soll
     * @return Ergebnis des Rechnungslaufs mit allen Details
     */
    public InvoiceBatchResult runInvoiceBatch(LocalDate billingDate) {
        // Ruft die erweiterte Version auf mit 'true' = alle vorherigen Monate einschließen
        return runInvoiceBatch(billingDate, true);
    }

    /**
     * HAUPTMETHODE FÜR RECHNUNGSLÄUFE (Erweiterte Version)
     *
     * Das ist das "Herz" der Klasse - koordiniert den gesamten Rechnungslauf.
     *
     * Ablauf:
     * 1. Vorgang starten (für Logging/Tracking)
     * 2. Analysieren was zu verarbeiten ist
     * 3. Verarbeitung durchführen
     * 4. Vorgang abschließen
     * 5. Bei Fehlern: Cleanup und Error-Handling
     *
     * @param billingDate Stichtag für die Abrechnung
     * @param includeAllPreviousMonths true = alle offenen Monate, false = nur exakter Stichtag
     * @return Detailliertes Ergebnis des Rechnungslaufs
     */
    @Transactional // Diese Methode läuft komplett in einer Datenbank-Transaktion
    public InvoiceBatchResult runInvoiceBatch(LocalDate billingDate, boolean includeAllPreviousMonths) {

        // SCHRITT 1: VORGANG STARTEN
        // Wir erstellen einen "Vorgang" um den ganzen Rechnungslauf zu tracken
        // Das ist für Audit-Logs und Monitoring wichtig
        Vorgang vorgang = startVorgang(billingDate, includeAllPreviousMonths);

        try {
            // SCHRITT 2: ANALYSIEREN WAS ZU TUN IST
            // Der Analyzer schaut welche Fälligkeiten abgerechnet werden müssen
            InvoiceBatchAnalysis analysis = analyzer.analyzeBillingScope(billingDate, includeAllPreviousMonths);

            // EARLY EXIT: Wenn nichts zu tun ist, beenden wir früh
            if (analysis.getTotalCount() == 0) {
                String message = "Keine offenen Fälligkeiten gefunden";

                // Vorgang als erfolgreich abschließen (auch wenn nichts passiert ist)
                vorgangService.vorgangErfolgreichAbschliessen(vorgang.getId(), 0, 0, 0, null);

                // Ergebnis-Objekt mit Builder-Pattern erstellen und zurückgeben
                return new InvoiceBatchResult.Builder()
                        .withVorgangsnummer(vorgang.getVorgangsnummer()) // Verknüpfung zum Vorgang
                        .withMessage(message)                            // Beschreibung was passiert ist
                        .build();                                       // Builder fertigstellen
            }

            // SCHRITT 3: VERARBEITUNG DURCHFÜHREN
            // Der Processor macht die eigentliche Arbeit (Rechnungen erstellen, etc.)
            InvoiceBatchResult result = processor.processBatch(analysis, vorgang, billingDate);

            // SCHRITT 4: VORGANG ABSCHLIESSEN
            // Je nach Ergebnis schließen wir den Vorgang erfolgreich oder mit Fehlern ab
            closeVorgang(vorgang, result, analysis);

            // Ergebnis zurückgeben
            return result;

        } catch (Exception e) {
            // FEHLER-BEHANDLUNG
            // Wenn irgendwas schief geht, loggen wir den Fehler und schließen den Vorgang ab

            logger.error("Kritischer Fehler im Rechnungslauf", e);

            // Vorgang als fehlgeschlagen markieren
            vorgangService.vorgangMitFehlerAbschliessen(vorgang.getId(), e.getMessage());

            // Exception weiterwerfen mit zusätzlichen Informationen
            throw new RuntimeException("Rechnungslauf fehlgeschlagen (Vorgang: " + vorgang.getVorgangsnummer() + ")", e);
        }
    }

    /**
     * PRIVATE HILFSMETHODE: VORGANG STARTEN
     *
     * Erstellt einen neuen Vorgang für Tracking und Logging.
     * Ein "Vorgang" ist wie ein Logbuch-Eintrag für alle wichtigen Geschäftsprozesse.
     *
     * @param billingDate Das Abrechnungsdatum
     * @param includeAllPreviousMonths Der Modus (alle Monate vs. nur exakt)
     * @return Der erstellte Vorgang
     */
    private Vorgang startVorgang(LocalDate billingDate, boolean includeAllPreviousMonths) {
        // Aussagekräftigen Titel für den Vorgang erstellen
        String titel = String.format("Rechnungslauf zum %s (%s)",
                billingDate,
                includeAllPreviousMonths ? "alle offenen Monate" : "nur exakter Stichtag");

        // Detaillierte Beschreibung für den Vorgang
        String beschreibung = String.format("Automatischer Rechnungslauf mit Stichtag %s", billingDate);

        // Vorgang über den VorgangService erstellen
        // "starteAutomatischenVorgang" bedeutet: System hat das ausgelöst, nicht ein Benutzer
        Vorgang vorgang = vorgangService.starteAutomatischenVorgang(VorgangTyp.RECHNUNGSLAUF, titel);
        vorgang.setBeschreibung(beschreibung);

        // Log-Ausgaben für bessere Nachverfolgung
        logger.info("==================== RECHNUNGSLAUF START ====================");
        logger.info("Vorgang: {} - {}", vorgang.getVorgangsnummer(), titel);
        logger.info("=============================================================");

        return vorgang;
    }

    /**
     * PRIVATE HILFSMETHODE: VORGANG ABSCHLIESSEN
     *
     * Schließt den Vorgang basierend auf dem Ergebnis ab (erfolgreich oder mit Fehlern).
     * Setzt auch Metadaten für spätere Auswertungen.
     *
     * @param vorgang Der zu schließende Vorgang
     * @param result Das Ergebnis der Verarbeitung
     * @param analysis Die ursprüngliche Analyse
     */
    private void closeVorgang(Vorgang vorgang, InvoiceBatchResult result, InvoiceBatchAnalysis analysis) {
        // METADATEN SETZEN
        // Metadaten sind zusätzliche Informationen die für Reports und Monitoring genutzt werden
        // Hier als JSON-String gespeichert
        String metadaten = String.format(
                "{\"billingDate\":\"%s\",\"batchId\":\"%s\",\"monthsProcessed\":%d}",
                analysis.getBillingDate(),    // Abrechnungsdatum
                result.getBatchId(),          // Eindeutige Batch-ID
                analysis.getMonthCount()      // Anzahl verarbeiteter Monate
        );
        vorgang.setMetadaten(metadaten);

        // VORGANG ABSCHLIESSEN basierend auf Ergebnis
        if (result.hasErrors()) {
            // Es gab Fehler - Vorgang als fehlgeschlagen markieren

            // Nur die ersten 3 Fehler in das Fehlerprotokoll schreiben (für Übersichtlichkeit)
            String errorSummary = String.join("; ",
                    result.getErrors().subList(0, Math.min(3, result.getErrors().size())));

            vorgangService.vorgangMitFehlerAbschliessen(vorgang.getId(), errorSummary);

        } else {
            // Alles lief gut - Vorgang als erfolgreich markieren
            vorgangService.vorgangErfolgreichAbschliessen(
                    vorgang.getId(),                        // Welcher Vorgang
                    result.getProcessedDueSchedules(),      // Anzahl verarbeiteter Fälligkeiten
                    result.getCreatedInvoices(),            // Anzahl erstellter Rechnungen
                    0,                                      // Anzahl Fehler (hier: 0)
                    result.getTotalAmount()                 // Gesamtbetrag
            );
        }

        // Abschluss-Logs für bessere Nachverfolgung
        logger.info("Vorgang {} abgeschlossen: {}", vorgang.getVorgangsnummer(), result.getMessage());
        logger.info("Dauer: {} ms", vorgang.getDauerInMs()); // Wie lange hat es gedauert?
    }
}

/*
 * SPRING BOOT KONZEPTE
 *
 *
 *
 * 1. Constructor Injection:
 *    - Spring gibt automatisch die benötigten Services in den Konstruktor
 *    - Macht die Abhängigkeiten explizit sichtbar
 *    - Besser als @Autowired auf Feldern
 *
 * 2. Logger:
 *    - SLF4J ist das Standard-Logging-Framework
 *    - logger.info() für normale Meldungen
 *    - logger.error() für Fehler
 *    - logger.debug() für Detail-Informationen
 *
 * 3. Builder Pattern:
 *    - Für komplexe Objekterstellung
 *    - Macht den Code lesbarer
 *    - Erlaubt optionale Parameter
 *
 * WARUM DIESE ARCHITEKTUR?
 *
 * - Separation of Concerns: Jede Klasse hat eine klare Aufgabe
 * - Testability: Jede Klasse kann isoliert getestet werden
 * - Maintainability: Änderungen sind lokalisiert
 * - Scalability: Neue Features können einfach hinzugefügt werden
 */