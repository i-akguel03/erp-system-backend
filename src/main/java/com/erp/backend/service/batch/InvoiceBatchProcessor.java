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

        for (DueSchedule dueSchedule : analysis.getDueSchedules()) {
            try {
                InvoiceBatchItemProcessor.ProcessedItem item =
                        itemProcessor.process(dueSchedule, billingDate, batchId, vorgang);
                resultBuilder
                        .addCreatedInvoice(item.invoice())
                        .addCreatedOpenItem(item.openItem())
                        .addProcessedDueSchedule(item.dueSchedule());
            } catch (Exception e) {
                String error = String.format("Fehler bei Fälligkeit %s: %s",
                        dueSchedule.getDueNumber(), e.getMessage());
                logger.error("✗ {}", error, e);
                resultBuilder.addError(error);
            }
        }

        logger.info("==================== BATCH-VERARBEITUNG ENDE ====================");

        InvoiceBatchResult result = resultBuilder.build();
        validateResult(result);
        return result;
    }

    private void validateResult(InvoiceBatchResult result) {
        if (result.getCreatedInvoices() != result.getCreatedOpenItems()) {
            logger.error("✗ INKONSISTENZ: {} Rechnungen vs {} OpenItems",
                    result.getCreatedInvoices(), result.getCreatedOpenItems());
        }
        if (result.getProcessedDueSchedules() != result.getCreatedInvoices()) {
            logger.error("✗ INKONSISTENZ: {} DueSchedules vs {} Rechnungen",
                    result.getProcessedDueSchedules(), result.getCreatedInvoices());
        }
    }
}
