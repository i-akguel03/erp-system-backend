package com.erp.backend.service;

import com.erp.backend.domain.*;
import com.erp.backend.repository.DueScheduleRepository;
import com.erp.backend.repository.InvoiceRepository;
import com.erp.backend.repository.OpenItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service für den Rechnungslauf.
 *
 * Aufgaben:
 * 1. Alle offenen DueSchedules bis zu einem bestimmten Datum abrufen
 * 2. Nach Subscription gruppieren
 * 3. Für jede Subscription eine Rechnung (Invoice) erzeugen
 * 4. Offene Posten (OpenItem) für jede Rechnung erzeugen
 * 5. DueSchedules als abgerechnet markieren
 */
@Service
public class InvoiceBatchService {

    private final DueScheduleRepository dueScheduleRepository;
    private final InvoiceRepository invoiceRepository;
    private final OpenItemRepository openItemRepository;
    private final InvoiceFactory invoiceFactory;

    // Konstruktor für Dependency Injection
    public InvoiceBatchService(DueScheduleRepository dueScheduleRepository,
                               InvoiceRepository invoiceRepository,
                               OpenItemRepository openItemRepository,
                               InvoiceFactory invoiceFactory) {
        this.dueScheduleRepository = dueScheduleRepository;
        this.invoiceRepository = invoiceRepository;
        this.openItemRepository = openItemRepository;
        this.invoiceFactory = invoiceFactory;
    }

    /**
     * Startet den Rechnungslauf für das angegebene Datum.
     *
     * @param billingRunDate Das Datum, bis zu dem offene DueSchedules abgerechnet werden sollen
     * @return Ergebnis des Rechnungslaufs mit Anzahl erstellter Rechnungen/Fälligkeiten
     */
    @Transactional
    public InvoiceBatchResult runInvoiceBatch(LocalDate billingRunDate) {
        // 1. Alle offenen DueSchedules bis zum angegebenen Datum abrufen
        List<DueSchedule> openDueSchedules = dueScheduleRepository
                .findByStatusAndDueDateLessThanEqual(DueStatus.PENDING, billingRunDate);

        // Keine offenen Fälligkeiten -> Abbruch
        if (openDueSchedules.isEmpty()) {
            return new InvoiceBatchResult(0, 0, "Keine offenen Fälligkeiten gefunden");
        }

        // 2. Nach Subscription gruppieren, damit pro Abonnement eine Rechnung erstellt werden kann
        Map<Subscription, List<DueSchedule>> groupedBySubscription = openDueSchedules
                .stream()
                .collect(Collectors.groupingBy(DueSchedule::getSubscription));

        int createdInvoices = 0;       // Anzahl erstellter Rechnungen
        int processedDueSchedules = 0; // Anzahl verarbeiteter DueSchedules

        // 3. Für jede Subscription Rechnung und offene Posten erzeugen
        for (Map.Entry<Subscription, List<DueSchedule>> entry : groupedBySubscription.entrySet()) {
            Subscription subscription = entry.getKey();
            List<DueSchedule> dueSchedules = entry.getValue();

            try {
                // 3a. Invoice inkl. InvoiceItems erzeugen
                Invoice invoice = invoiceFactory.createInvoice(subscription, dueSchedules, billingRunDate);
                invoiceRepository.save(invoice); // Rechnung speichern

                // 3b. Batch-ID festlegen (final für Lambda)
                final String batchId = invoice.getInvoiceBatchId() != null
                        ? invoice.getInvoiceBatchId()
                        : "BATCH-" + System.currentTimeMillis();
                invoice.setInvoiceBatchId(batchId);

                // 3c. Offene Posten (OpenItem) für jede InvoiceItem erzeugen und speichern
                if (invoice.getInvoiceItems() != null) {
                    invoice.getInvoiceItems().forEach(item -> {
                        OpenItem openItem = invoiceFactory.createOpenItem(invoice, item);
                        openItemRepository.save(openItem);
                    });
                }

                // 3d. DueSchedules als abgerechnet markieren
                dueSchedules.forEach(due -> due.markAsInvoiced(invoice, batchId));

                // 3e. Zähler erhöhen
                createdInvoices++;
                processedDueSchedules += dueSchedules.size();

            } catch (Exception e) {
                // Fehlerbehandlung: Ausgabe in der Konsole
                System.err.println("Fehler beim Erstellen der Rechnung für Subscription "
                        + (subscription != null ? subscription.getId() : "Unknown") + ": " + e.getMessage());
            }
        }

        // 4. Ergebnis zurückgeben
        return new InvoiceBatchResult(createdInvoices, processedDueSchedules,
                "Rechnungslauf abgeschlossen am " + billingRunDate);
    }

    /**
     * DTO für das Ergebnis des Rechnungslaufs
     */
    public static class InvoiceBatchResult {
        private final int createdInvoices;        // Anzahl erstellter Rechnungen
        private final int processedDueSchedules;  // Anzahl verarbeiteter DueSchedules
        private final String message;             // Nachricht / Status

        public InvoiceBatchResult(int createdInvoices, int processedDueSchedules, String message) {
            this.createdInvoices = createdInvoices;
            this.processedDueSchedules = processedDueSchedules;
            this.message = message;
        }

        public int getCreatedInvoices() {
            return createdInvoices;
        }

        public int getProcessedDueSchedules() {
            return processedDueSchedules;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public String toString() {
            return String.format("Rechnungslauf abgeschlossen: %d Rechnungen erstellt, %d Fälligkeiten verarbeitet. %s",
                    createdInvoices, processedDueSchedules, message);
        }
    }
}
