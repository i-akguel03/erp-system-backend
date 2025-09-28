package com.erp.backend.service.batch;

import com.erp.backend.domain.*;
import com.erp.backend.repository.InvoiceRepository;
import com.erp.backend.repository.OpenItemRepository;
import com.erp.backend.service.DueScheduleService;
import com.erp.backend.service.InvoiceFactory;
import com.erp.backend.service.OpenItemFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Verarbeitet die eigentliche Rechnungserstellung.
 * Verantwortlich für: Invoice/OpenItem-Erstellung, DueSchedule-Updates, Entitäten-Verknüpfungen
 */
@Service
@Transactional
public class InvoiceBatchProcessor {

    private static final Logger logger = LoggerFactory.getLogger(InvoiceBatchProcessor.class);

    private final InvoiceFactory invoiceFactory;
    private final OpenItemFactory openItemFactory;
    private final DueScheduleService dueScheduleService;
    private final InvoiceRepository invoiceRepository;
    private final OpenItemRepository openItemRepository;

    public InvoiceBatchProcessor(InvoiceFactory invoiceFactory,
                                 OpenItemFactory openItemFactory,
                                 DueScheduleService dueScheduleService,
                                 InvoiceRepository invoiceRepository,
                                 OpenItemRepository openItemRepository) {
        this.invoiceFactory = invoiceFactory;
        this.openItemFactory = openItemFactory;
        this.dueScheduleService = dueScheduleService;
        this.invoiceRepository = invoiceRepository;
        this.openItemRepository = openItemRepository;
    }

    public InvoiceBatchResult processBatch(InvoiceBatchAnalysis analysis, Vorgang vorgang, LocalDate billingDate) {
        String batchId = "BATCH-" + vorgang.getVorgangsnummer();

        InvoiceBatchResult.Builder resultBuilder = new InvoiceBatchResult.Builder()
                .withBatchId(batchId)
                .withVorgangsnummer(vorgang.getVorgangsnummer());

        logger.info("==================== BATCH-VERARBEITUNG START ====================");

        // Jede Fälligkeit verarbeiten
        for (DueSchedule dueSchedule : analysis.getDueSchedules()) {
            try {
                processingleFälligkeit(dueSchedule, billingDate, batchId, vorgang, resultBuilder);

            } catch (Exception e) {
                handleProcessingError(dueSchedule, e, resultBuilder);
            }
        }

        logger.info("==================== BATCH-VERARBEITUNG ENDE ====================");

        InvoiceBatchResult result = resultBuilder.build();
        validateResult(result);

        return result;
    }

    private void processingleFälligkeit(DueSchedule dueSchedule, LocalDate billingDate,
                                        String batchId, Vorgang vorgang,
                                        InvoiceBatchResult.Builder resultBuilder) {

        boolean isOverdue = dueSchedule.getDueDate().isBefore(billingDate);

        // WICHTIG: Subscription aus DueSchedule holen
        Subscription subscription = dueSchedule.getSubscription();
        if (subscription == null) {
            throw new IllegalStateException("DueSchedule " + dueSchedule.getDueNumber() +
                    " hat keine Subscription zugeordnet");
        }

        // 1. Rechnung erstellen
        Invoice invoice = invoiceFactory.createInvoiceForDueSchedule(dueSchedule, billingDate, batchId, isOverdue);
        invoice.setVorgang(vorgang);

        // KRITISCH: subscription_id explizit setzen
        invoice.setSubscriptionId(subscription.getId());

        Invoice savedInvoice = invoiceRepository.save(invoice);
        resultBuilder.addCreatedInvoice(savedInvoice);

        // 2. DueSchedule aktualisieren
        dueScheduleService.markAsCompleted(dueSchedule.getId(), savedInvoice.getId(), batchId);
        dueSchedule.setVorgang(vorgang);
        resultBuilder.addProcessedDueSchedule(dueSchedule);

        // 3. OpenItem erstellen - jetzt mit korrekter subscription_id
        OpenItem openItem = openItemFactory.createOpenItemForInvoice(savedInvoice, isOverdue);
        openItem.setVorgang(vorgang);

        OpenItem savedOpenItem = openItemRepository.save(openItem);
        resultBuilder.addCreatedOpenItem(savedOpenItem);

        // Success-Logging
        String status = isOverdue ? "ÜBERFÄLLIG" : "AKTUELL";
        logger.info("✓ {} ({}) → {} (%.2f€) → OpenItem {} (subscription: {})",
                dueSchedule.getDueNumber(), status, savedInvoice.getInvoiceNumber(),
                savedInvoice.getTotalAmount(), savedOpenItem.getId(), subscription.getId());
    }

    private void handleProcessingError(DueSchedule dueSchedule, Exception e,
                                       InvoiceBatchResult.Builder resultBuilder) {
        String error = String.format("Fehler bei Fälligkeit %s: %s",
                dueSchedule.getDueNumber(), e.getMessage());
        logger.error("✗ {}", error, e);
        resultBuilder.addError(error);

        // Rollback versuchen
        try {
            dueScheduleService.rollbackCompleted(dueSchedule.getId(),
                    "Rollback due to billing error: " + e.getMessage());
        } catch (Exception rollbackError) {
            logger.error("→ ROLLBACK-FEHLER für DueSchedule {}: {}",
                    dueSchedule.getDueNumber(), rollbackError.getMessage());
        }
    }

    private void validateResult(InvoiceBatchResult result) {
        if (result.getCreatedInvoices() != result.getCreatedOpenItems()) {
            String error = String.format("INKONSISTENZ: %d Rechnungen vs %d OpenItems",
                    result.getCreatedInvoices(), result.getCreatedOpenItems());
            logger.error("✗ {}", error);
        }

        if (result.getProcessedDueSchedules() != result.getCreatedInvoices()) {
            String error = String.format("INKONSISTENZ: %d DueSchedules vs %d Rechnungen",
                    result.getProcessedDueSchedules(), result.getCreatedInvoices());
            logger.error("✗ {}", error);
        }
    }
}