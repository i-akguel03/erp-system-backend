package com.erp.backend.service.batch;

import com.erp.backend.domain.*;
import com.erp.backend.event.InvoiceBatchCompletedEvent;
import com.erp.backend.service.VorgangService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class InvoiceBatchOrchestrator {

    private static final Logger logger = LoggerFactory.getLogger(InvoiceBatchOrchestrator.class);

    private final InvoiceBatchProcessor processor;
    private final InvoiceBatchAnalyzer analyzer;
    private final VorgangService vorgangService;
    private final ApplicationEventPublisher eventPublisher;

    public InvoiceBatchOrchestrator(InvoiceBatchProcessor processor,
                                    InvoiceBatchAnalyzer analyzer,
                                    VorgangService vorgangService,
                                    ApplicationEventPublisher eventPublisher) {
        this.processor = processor;
        this.analyzer = analyzer;
        this.vorgangService = vorgangService;
        this.eventPublisher = eventPublisher;
    }

    public InvoiceBatchResult runInvoiceBatch(LocalDate billingDate) {
        return runInvoiceBatch(billingDate, true, null);
    }

    public InvoiceBatchResult runInvoiceBatch(LocalDate billingDate, boolean includeAllPreviousMonths) {
        return runInvoiceBatch(billingDate, includeAllPreviousMonths, null);
    }

    public InvoiceBatchResult runInvoiceBatch(LocalDate billingDate, boolean includeAllPreviousMonths, String benutzer) {
        // Verhindert gleichzeitige Läufe — z.B. wenn zwei Admins parallel auslösen
        if (vorgangService.hatLaufendenRechnungslauf()) {
            throw new IllegalStateException(
                    "Es läuft bereits ein Rechnungslauf. Bitte warten Sie bis dieser abgeschlossen ist.");
        }

        Vorgang vorgang = startVorgang(billingDate, includeAllPreviousMonths, benutzer);

        try {
            InvoiceBatchAnalysis analysis = analyzer.analyzeBillingScope(billingDate, includeAllPreviousMonths);

            if (analysis.getTotalCount() == 0) {
                vorgangService.vorgangErfolgreichAbschliessen(vorgang.getId(), 0, 0, 0, null);
                return new InvoiceBatchResult.Builder()
                        .withVorgangsnummer(vorgang.getVorgangsnummer())
                        .withMessage("Keine offenen Fälligkeiten gefunden")
                        .build();
            }

            InvoiceBatchResult result = processor.processBatch(analysis, vorgang, billingDate);

            // closeVorgang in eigenem try-catch: ein Fehler beim Abschluss darf das Batch-Ergebnis nicht verdecken
            try {
                closeVorgang(vorgang, result, analysis);
            } catch (Exception closeEx) {
                logger.error("Fehler beim Abschließen des Vorgangs {} — Batch-Ergebnis bleibt gültig: {}",
                        vorgang.getVorgangsnummer(), closeEx.getMessage(), closeEx);
            }

            return result;

        } catch (IllegalStateException e) {
            // Fachliche Fehler (z.B. Validierungsprobleme) — Vorgang als fehlerhaft markieren und weiterwerfen
            logger.error("Fachlicher Fehler im Rechnungslauf (Vorgang: {}): {}", vorgang.getVorgangsnummer(), e.getMessage());
            safeMarkVorgangAsFailed(vorgang, e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Kritischer Fehler im Rechnungslauf (Vorgang: {})", vorgang.getVorgangsnummer(), e);
            safeMarkVorgangAsFailed(vorgang, e.getMessage());
            throw new RuntimeException("Rechnungslauf fehlgeschlagen (Vorgang: " + vorgang.getVorgangsnummer() + ")", e);
        }
    }

    private void safeMarkVorgangAsFailed(Vorgang vorgang, String fehlerDetails) {
        try {
            vorgangService.vorgangMitFehlerAbschliessen(vorgang.getId(), fehlerDetails);
        } catch (Exception ex) {
            logger.error("Vorgang {} konnte nicht als fehlerhaft markiert werden: {}",
                    vorgang.getVorgangsnummer(), ex.getMessage());
        }
    }

    private Vorgang startVorgang(LocalDate billingDate, boolean includeAllPreviousMonths, String benutzer) {
        String titel = String.format("Rechnungslauf zum %s (%s)",
                billingDate,
                includeAllPreviousMonths ? "alle offenen Monate" : "nur exakter Stichtag");

        String ausgeloestVon = benutzer != null ? benutzer : "SYSTEM";
        boolean automatisch = benutzer == null;
        Vorgang vorgang = vorgangService.starteVorgang(VorgangTyp.RECHNUNGSLAUF, titel, null, ausgeloestVon, automatisch);
        vorgang.setBeschreibung(String.format("Rechnungslauf mit Stichtag %s", billingDate));

        logger.info("==================== RECHNUNGSLAUF START ====================");
        logger.info("Vorgang: {} - {}", vorgang.getVorgangsnummer(), titel);
        logger.info("=============================================================");

        return vorgang;
    }

    private void closeVorgang(Vorgang vorgang, InvoiceBatchResult result, InvoiceBatchAnalysis analysis) {
        String metadaten = String.format(
                "{\"billingDate\":\"%s\",\"batchId\":\"%s\",\"monthsProcessed\":%d}",
                analysis.getBillingDate(), result.getBatchId(), analysis.getMonthCount());
        vorgangService.updateMetadaten(vorgang.getId(), metadaten);

        int gesamtVersuche = result.getProcessedDueSchedules() + result.getErrorCount();
        String errorSummary = null;

        if (result.hasErrors()) {
            errorSummary = String.join("; ",
                    result.getErrors().subList(0, Math.min(3, result.getErrors().size())));
            // Statistiken auch bei Fehlern setzen, damit der Vorgang aussagekräftig bleibt
            vorgangService.vorgangMitFehlerAbschliessen(
                    vorgang.getId(),
                    gesamtVersuche,
                    result.getProcessedDueSchedules(),
                    result.getErrorCount(),
                    result.getTotalAmount(),
                    errorSummary);
        } else {
            vorgangService.vorgangErfolgreichAbschliessen(
                    vorgang.getId(),
                    result.getProcessedDueSchedules(),
                    result.getCreatedInvoices(),
                    0,
                    result.getTotalAmount());
        }

        eventPublisher.publishEvent(new InvoiceBatchCompletedEvent(
                this, vorgang.getVorgangsnummer(), result.getCreatedInvoices(),
                result.hasErrors(), errorSummary));

        logger.info("Vorgang {} abgeschlossen: {}", vorgang.getVorgangsnummer(), result.getMessage());
        logger.info("Dauer: {} ms", vorgang.getDauerInMs());
    }
}