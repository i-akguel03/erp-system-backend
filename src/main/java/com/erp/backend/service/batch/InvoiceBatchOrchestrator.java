package com.erp.backend.service.batch;

import com.erp.backend.domain.*;
import com.erp.backend.service.VorgangService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class InvoiceBatchOrchestrator {

    private static final Logger logger = LoggerFactory.getLogger(InvoiceBatchOrchestrator.class);

    private final InvoiceBatchProcessor processor;
    private final InvoiceBatchAnalyzer analyzer;
    private final VorgangService vorgangService;

    public InvoiceBatchOrchestrator(InvoiceBatchProcessor processor,
                                    InvoiceBatchAnalyzer analyzer,
                                    VorgangService vorgangService) {
        this.processor = processor;
        this.analyzer = analyzer;
        this.vorgangService = vorgangService;
    }

    public InvoiceBatchResult runInvoiceBatch(LocalDate billingDate) {
        return runInvoiceBatch(billingDate, true, null);
    }

    public InvoiceBatchResult runInvoiceBatch(LocalDate billingDate, boolean includeAllPreviousMonths) {
        return runInvoiceBatch(billingDate, includeAllPreviousMonths, null);
    }

    public InvoiceBatchResult runInvoiceBatch(LocalDate billingDate, boolean includeAllPreviousMonths, String benutzer) {
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
            closeVorgang(vorgang, result, analysis);
            return result;

        } catch (Exception e) {
            logger.error("Kritischer Fehler im Rechnungslauf", e);
            vorgangService.vorgangMitFehlerAbschliessen(vorgang.getId(), e.getMessage());
            throw new RuntimeException("Rechnungslauf fehlgeschlagen (Vorgang: " + vorgang.getVorgangsnummer() + ")", e);
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

        if (result.hasErrors()) {
            String errorSummary = String.join("; ",
                    result.getErrors().subList(0, Math.min(3, result.getErrors().size())));
            vorgangService.vorgangMitFehlerAbschliessen(vorgang.getId(), errorSummary);
        } else {
            vorgangService.vorgangErfolgreichAbschliessen(
                    vorgang.getId(),
                    result.getProcessedDueSchedules(),
                    result.getCreatedInvoices(),
                    0,
                    result.getTotalAmount());
        }

        logger.info("Vorgang {} abgeschlossen: {}", vorgang.getVorgangsnummer(), result.getMessage());
        logger.info("Dauer: {} ms", vorgang.getDauerInMs());
    }
}