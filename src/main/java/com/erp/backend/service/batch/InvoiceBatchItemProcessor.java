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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Verarbeitet eine einzelne Fälligkeit in einer eigenen Transaktion (REQUIRES_NEW).
 * Bei einem Fehler wird nur diese eine Fälligkeit zurückgerollt — nicht der gesamte Batch.
 */
@Service
public class InvoiceBatchItemProcessor {

    private static final Logger logger = LoggerFactory.getLogger(InvoiceBatchItemProcessor.class);

    private final InvoiceFactory invoiceFactory;
    private final OpenItemFactory openItemFactory;
    private final DueScheduleService dueScheduleService;
    private final InvoiceRepository invoiceRepository;
    private final OpenItemRepository openItemRepository;

    public InvoiceBatchItemProcessor(InvoiceFactory invoiceFactory,
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

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ProcessedItem process(DueSchedule dueSchedule, LocalDate billingDate, String batchId, Vorgang vorgang) {
        boolean isOverdue = dueSchedule.getDueDate().isBefore(billingDate);

        Subscription subscription = dueSchedule.getSubscription();
        if (subscription == null) {
            throw new IllegalStateException(
                    "DueSchedule " + dueSchedule.getDueNumber() + " hat keine Subscription");
        }

        Invoice invoice = invoiceFactory.createInvoiceForDueSchedule(dueSchedule, billingDate, batchId, isOverdue);
        invoice.setVorgang(vorgang);
        invoice.setSubscriptionId(subscription.getId());
        Invoice savedInvoice = invoiceRepository.save(invoice);

        dueScheduleService.markAsCompleted(dueSchedule.getId(), savedInvoice.getId(), batchId);
        dueSchedule.setVorgang(vorgang);

        OpenItem openItem = openItemFactory.createOpenItemForInvoice(savedInvoice, isOverdue);
        openItem.setVorgang(vorgang);
        OpenItem savedOpenItem = openItemRepository.save(openItem);

        String status = isOverdue ? "ÜBERFÄLLIG" : "AKTUELL";
        logger.info("✓ {} ({}) → {} ({} EUR) → OpenItem {}",
                dueSchedule.getDueNumber(), status, savedInvoice.getInvoiceNumber(),
                savedInvoice.getTotalAmount(), savedOpenItem.getId());

        return new ProcessedItem(savedInvoice, savedOpenItem, dueSchedule);
    }

    public record ProcessedItem(Invoice invoice, OpenItem openItem, DueSchedule dueSchedule) {}
}
