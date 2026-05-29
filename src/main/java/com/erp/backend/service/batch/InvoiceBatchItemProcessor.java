package com.erp.backend.service.batch;

import com.erp.backend.domain.*;
import com.erp.backend.repository.DueScheduleRepository;
import com.erp.backend.repository.InvoiceRepository;
import com.erp.backend.repository.OpenItemRepository;
import com.erp.backend.service.CustomerEmailService;
import com.erp.backend.service.DueScheduleService;
import com.erp.backend.service.InvoiceFactory;
import com.erp.backend.service.OpenItemFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Verarbeitet eine einzelne Fälligkeit in einer eigenen Transaktion (REQUIRES_NEW).
 * Bei einem Fehler wird nur diese eine Fälligkeit zurückgerollt — nicht der gesamte Batch.
 */
@Service
public class InvoiceBatchItemProcessor {

    private static final Logger logger = LoggerFactory.getLogger(InvoiceBatchItemProcessor.class);

    @PersistenceContext
    private EntityManager entityManager;

    private final DueScheduleRepository dueScheduleRepository;
    private final InvoiceFactory invoiceFactory;
    private final OpenItemFactory openItemFactory;
    private final DueScheduleService dueScheduleService;
    private final InvoiceRepository invoiceRepository;
    private final OpenItemRepository openItemRepository;
    private final CustomerEmailService customerEmailService;

    public InvoiceBatchItemProcessor(DueScheduleRepository dueScheduleRepository,
                                     InvoiceFactory invoiceFactory,
                                     OpenItemFactory openItemFactory,
                                     DueScheduleService dueScheduleService,
                                     InvoiceRepository invoiceRepository,
                                     OpenItemRepository openItemRepository,
                                     CustomerEmailService customerEmailService) {
        this.dueScheduleRepository = dueScheduleRepository;
        this.invoiceFactory = invoiceFactory;
        this.openItemFactory = openItemFactory;
        this.dueScheduleService = dueScheduleService;
        this.invoiceRepository = invoiceRepository;
        this.openItemRepository = openItemRepository;
        this.customerEmailService = customerEmailService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ProcessedItem process(DueSchedule dueScheduleRef, LocalDate billingDate, String batchId, Vorgang vorgangRef) {
        // Reload in this transaction so all lazy associations are managed — no detached-entity issues
        UUID dueScheduleId = dueScheduleRef.getId();
        DueSchedule dueSchedule = dueScheduleRepository.findById(dueScheduleId)
                .orElseThrow(() -> new IllegalStateException("DueSchedule nicht gefunden: " + dueScheduleId));

        // Schutz gegen Doppelverarbeitung: bereits abgerechnete Fälligkeiten ablehnen
        if (dueSchedule.getInvoiceId() != null) {
            throw new IllegalStateException(
                    "DueSchedule " + dueSchedule.getDueNumber() + " wurde bereits abgerechnet " +
                    "(Rechnung: " + dueSchedule.getInvoiceId() + ", Batch: " + dueSchedule.getInvoiceBatchId() + ")");
        }
        if (!dueSchedule.isActive()) {
            throw new IllegalStateException(
                    "DueSchedule " + dueSchedule.getDueNumber() + " kann nicht verarbeitet werden — Status: " + dueSchedule.getStatus());
        }

        // Get a managed Vorgang reference for FK assignment
        Vorgang vorgang = entityManager.getReference(Vorgang.class, vorgangRef.getId());

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

        // E-Mail-Versand darf die Buchung nicht blockieren oder zurückrollen
        try {
            customerEmailService.sendInvoiceEmail(savedInvoice);
        } catch (Exception emailEx) {
            logger.warn("E-Mail-Versand für Rechnung {} fehlgeschlagen (Buchung bleibt erhalten): {}",
                    savedInvoice.getInvoiceNumber(), emailEx.getMessage());
        }

        dueScheduleService.markAsCompleted(dueSchedule.getId(), savedInvoice.getId(), batchId);

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
