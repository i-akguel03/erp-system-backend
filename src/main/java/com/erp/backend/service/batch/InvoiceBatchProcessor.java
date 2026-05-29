package com.erp.backend.service.batch;

import com.erp.backend.domain.DueSchedule;
import com.erp.backend.domain.Vorgang;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * Koordiniert die Verarbeitung aller Fälligkeiten eines Batches.
 * Die eigentliche Einzel-Verarbeitung delegiert er an InvoiceBatchItemProcessor,
 * der jede Fälligkeit in einer eigenen Transaktion (REQUIRES_NEW) verarbeitet.
 */
@Service
public class InvoiceBatchProcessor {

    private static final Logger logger = LoggerFactory.getLogger(InvoiceBatchProcessor.class);

    // Batch wird abgebrochen wenn nach mindestens MIN_ITEMS_FOR_ABORT Items die Fehlerquote über MAX_ERROR_RATE liegt
    private static final int MIN_ITEMS_FOR_ABORT = 5;
    private static final double MAX_ERROR_RATE = 0.5;

    private final InvoiceBatchItemProcessor itemProcessor;

    public InvoiceBatchProcessor(InvoiceBatchItemProcessor itemProcessor) {
        this.itemProcessor = itemProcessor;
    }

    public InvoiceBatchResult processBatch(InvoiceBatchAnalysis analysis, Vorgang vorgang, LocalDate billingDate) {
        String batchId = "BATCH-" + vorgang.getVorgangsnummer();

        InvoiceBatchResult.Builder resultBuilder = new InvoiceBatchResult.Builder()
                .withBatchId(batchId)
                .withVorgangsnummer(vorgang.getVorgangsnummer());

        logger.info("==================== BATCH-VERARBEITUNG START ====================");

        int successCount = 0;
        int errorCount = 0;

        for (DueSchedule dueSchedule : analysis.getDueSchedules()) {
            try {
                InvoiceBatchItemProcessor.ProcessedItem item =
                        itemProcessor.process(dueSchedule, billingDate, batchId, vorgang);
                resultBuilder
                        .addCreatedInvoice(item.invoice())
                        .addCreatedOpenItem(item.openItem())
                        .addProcessedDueSchedule(item.dueSchedule());
                successCount++;
            } catch (Exception e) {
                String dueNumber = getDueNumberSafe(dueSchedule);
                String error = String.format("Fehler bei Fälligkeit %s: %s", dueNumber, e.getMessage());
                logger.error("✗ {}", error, e);
                resultBuilder.addError(error);
                errorCount++;

                // Frühzeitiger Abbruch bei zu hoher Fehlerquote
                int total = successCount + errorCount;
                if (total >= MIN_ITEMS_FOR_ABORT) {
                    double errorRate = (double) errorCount / total;
                    if (errorRate > MAX_ERROR_RATE) {
                        String abortMsg = String.format(
                                "Batch vorzeitig abgebrochen: Fehlerquote %.0f%% (%d/%d Items) überschreitet Schwellwert von %.0f%%",
                                errorRate * 100, errorCount, total, MAX_ERROR_RATE * 100);
                        logger.error("✗ ABBRUCH — {}", abortMsg);
                        resultBuilder.addError(abortMsg);
                        break;
                    }
                }
            }
        }

        logger.info("==================== BATCH-VERARBEITUNG ENDE ====================");

        InvoiceBatchResult result = resultBuilder.build();
        validateResult(result);
        return result;
    }

    private String getDueNumberSafe(DueSchedule dueSchedule) {
        try {
            return dueSchedule.getDueNumber() != null ? dueSchedule.getDueNumber() : dueSchedule.getId().toString();
        } catch (Exception e) {
            return "[unbekannt]";
        }
    }

    private void validateResult(InvoiceBatchResult result) {
        if (result.getCreatedInvoices() != result.getCreatedOpenItems()) {
            logger.error("✗ DATENKONSISTENZ-ALARM: {} Rechnungen vs {} OpenItems — manuelle Prüfung erforderlich!",
                    result.getCreatedInvoices(), result.getCreatedOpenItems());
        }
        if (result.getProcessedDueSchedules() != result.getCreatedInvoices()) {
            logger.error("✗ DATENKONSISTENZ-ALARM: {} DueSchedules vs {} Rechnungen — manuelle Prüfung erforderlich!",
                    result.getProcessedDueSchedules(), result.getCreatedInvoices());
        }
    }
}
