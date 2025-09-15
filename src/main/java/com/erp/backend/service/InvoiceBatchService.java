package com.erp.backend.service;

import com.erp.backend.domain.*;
import com.erp.backend.repository.DueScheduleRepository;
import com.erp.backend.repository.InvoiceRepository;
import com.erp.backend.repository.OpenItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Service für den automatisierten Rechnungslauf.
 *
 * Workflow:
 * 1. Prüft ALLE aktiven DueSchedules bis zum Stichtag (inkl. überfällige)
 * 2. Erstellt für JEDE noch nicht abgerechnete Fälligkeit eine separate Rechnung
 * 3. Generiert für jede Rechnung einen OpenItem
 * 4. Markiert DueSchedules als abgerechnet
 */
@Service
@Transactional
public class InvoiceBatchService {

    private static final Logger logger = LoggerFactory.getLogger(InvoiceBatchService.class);

    private final DueScheduleRepository dueScheduleRepository;
    private final InvoiceRepository invoiceRepository;
    private final OpenItemRepository openItemRepository;
    private final InvoiceNumberGeneratorService invoiceNumberGenerator;

    public InvoiceBatchService(DueScheduleRepository dueScheduleRepository,
                               InvoiceRepository invoiceRepository,
                               OpenItemRepository openItemRepository,
                               InvoiceNumberGeneratorService invoiceNumberGenerator) {
        this.dueScheduleRepository = dueScheduleRepository;
        this.invoiceRepository = invoiceRepository;
        this.openItemRepository = openItemRepository;
        this.invoiceNumberGenerator = invoiceNumberGenerator;
    }

    /**
     * Hauptmethode für den Rechnungslauf
     * Erstellt für jede fällige DueSchedule eine separate Rechnung
     * WICHTIG: Erfasst ALLE aktiven Fälligkeiten bis zum Stichtag, auch überfällige!
     *
     * @param billingDate Stichtag für die Abrechnung
     * @return Ergebnis des Rechnungslaufs
     */
    @Transactional
    public InvoiceBatchResult runInvoiceBatch(LocalDate billingDate) {
        logger.info("Starte Rechnungslauf für Stichtag: {} (inkl. aller überfälligen Positionen)", billingDate);

        String batchId = generateBatchId();

        // 1. Hole ALLE aktiven, noch nicht abgerechneten Fälligkeiten bis zum Stichtag
        // Das schließt auch überfällige Positionen aus vorherigen Monaten ein
        List<DueSchedule> openDueSchedules = dueScheduleRepository
                .findByStatusAndDueDateLessThanEqual(DueStatus.ACTIVE, billingDate);

        if (openDueSchedules.isEmpty()) {
            logger.info("Keine offenen Fälligkeiten gefunden für Stichtag: {} (inkl. Überfällige)", billingDate);
            return new InvoiceBatchResult(0, 0, BigDecimal.ZERO,
                    "Keine offenen Fälligkeiten für " + billingDate + " (inkl. überfällige Positionen)");
        }

        // Logging für bessere Nachverfolgung
        long overdueCount = openDueSchedules.stream()
                .filter(ds -> ds.getDueDate().isBefore(billingDate))
                .count();

        long currentMonthCount = openDueSchedules.stream()
                .filter(ds -> ds.getDueDate().equals(billingDate) ||
                        (ds.getDueDate().getMonth() == billingDate.getMonth() &&
                                ds.getDueDate().getYear() == billingDate.getYear()))
                .count();

        logger.info("Gefunden: {} Fälligkeiten gesamt (davon {} überfällig, {} aktueller Monat)",
                openDueSchedules.size(), overdueCount, currentMonthCount);

        int createdInvoices = 0;
        int processedDueSchedules = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<String> errors = new ArrayList<>();

        // 2. Erstelle für JEDE Fälligkeit eine separate Rechnung
        for (DueSchedule dueSchedule : openDueSchedules) {
            try {
                // Spezielle Behandlung für überfällige Positionen
                boolean isOverdue = dueSchedule.getDueDate().isBefore(billingDate);

                // Erstelle Rechnung für diese eine Fälligkeit
                Invoice invoice = createInvoiceForDueSchedule(dueSchedule, billingDate, batchId, isOverdue);
                Invoice savedInvoice = invoiceRepository.save(invoice);

                // Erstelle OpenItem für diese Rechnung
                OpenItem openItem = createOpenItemForInvoice(savedInvoice, isOverdue);
                openItemRepository.save(openItem);

                // Markiere DueSchedule als abgerechnet
                dueSchedule.markAsCompleted();
                dueSchedule.setInvoiceId(savedInvoice.getId());
                dueSchedule.setInvoiceBatchId(batchId);
                dueScheduleRepository.save(dueSchedule);

                createdInvoices++;
                processedDueSchedules++;
                totalAmount = totalAmount.add(savedInvoice.getTotalAmount());

                String logMessage = isOverdue ?
                        "Rechnung {} erstellt für ÜBERFÄLLIGE Fälligkeit {} (ursprünglich fällig: {}) - Betrag: {}" :
                        "Rechnung {} erstellt für Fälligkeit {} - Betrag: {}";

                if (isOverdue) {
                    logger.info(logMessage,
                            savedInvoice.getInvoiceNumber(),
                            dueSchedule.getDueNumber(),
                            formatDate(dueSchedule.getDueDate()),
                            savedInvoice.getTotalAmount());
                } else {
                    logger.debug(logMessage,
                            savedInvoice.getInvoiceNumber(),
                            dueSchedule.getDueNumber(),
                            savedInvoice.getTotalAmount());
                }

            } catch (Exception e) {
                String error = String.format("Fehler bei Fälligkeit %s (fällig: %s): %s",
                        dueSchedule.getDueNumber(),
                        formatDate(dueSchedule.getDueDate()),
                        e.getMessage());
                logger.error(error, e);
                errors.add(error);
            }
        }

        String message = String.format(
                "Rechnungslauf abgeschlossen: %d Rechnungen erstellt, %d Fälligkeiten verarbeitet (davon %d überfällig), Gesamt: %.2f EUR",
                createdInvoices, processedDueSchedules, overdueCount, totalAmount
        );

        if (!errors.isEmpty()) {
            message += String.format(" (%d Fehler aufgetreten)", errors.size());
        }

        logger.info(message);
        return new InvoiceBatchResult(createdInvoices, processedDueSchedules, totalAmount, message);
    }

    /**
     * Erstellt eine Rechnung für eine einzelne Fälligkeit
     * Berücksichtigt auch überfällige Positionen
     */
    private Invoice createInvoiceForDueSchedule(DueSchedule dueSchedule,
                                                LocalDate billingDate,
                                                String batchId,
                                                boolean isOverdue) {

        Subscription subscription = dueSchedule.getSubscription();
        Customer customer = subscription.getContract().getCustomer();

        // Erstelle neue Rechnung
        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(invoiceNumberGenerator.generateInvoiceNumber());
        invoice.setCustomer(customer);
        invoice.setSubscription(subscription);
        invoice.setBillingAddress(customer.getBillingAddress());
        invoice.setInvoiceDate(billingDate); // Rechnungsdatum ist immer der Stichtag
        invoice.setDueDate(billingDate.plusDays(14)); // 14 Tage Zahlungsziel ab Rechnungsdatum
        invoice.setStatus(Invoice.InvoiceStatus.DRAFT);
        invoice.setInvoiceType(Invoice.InvoiceType.AUTO_GENERATED);
        invoice.setInvoiceBatchId(batchId);
        invoice.setPaymentTerms("Zahlbar innerhalb von 14 Tagen");
        invoice.setTaxRate(BigDecimal.valueOf(19)); // Standard MwSt

        // Rechnungsposition für Fälligkeit
        InvoiceItem item = createInvoiceItemFromDueSchedule(dueSchedule, isOverdue);
        invoice.addInvoiceItem(item);

        invoice.calculateTotals();

        // Angepasste Notiz je nach Status
        String noteTemplate = isOverdue ?
                "Automatisch erstellt für ÜBERFÄLLIGE Fälligkeit %s (ursprünglich fällig: %s, Periode: %s bis %s)" :
                "Automatisch erstellt für Fälligkeit %s (Periode: %s bis %s)";

        String notes = isOverdue ?
                String.format(noteTemplate,
                        dueSchedule.getDueNumber(),
                        formatDate(dueSchedule.getDueDate()),
                        formatDate(dueSchedule.getPeriodStart()),
                        formatDate(dueSchedule.getPeriodEnd())) :
                String.format(noteTemplate,
                        dueSchedule.getDueNumber(),
                        formatDate(dueSchedule.getPeriodStart()),
                        formatDate(dueSchedule.getPeriodEnd()));

        invoice.setNotes(notes);

        return invoice;
    }

    /**
     * Erstellt eine Rechnungsposition aus einer einzelnen Fälligkeit
     * Kennzeichnet überfällige Positionen in der Beschreibung
     */
    private InvoiceItem createInvoiceItemFromDueSchedule(DueSchedule dueSchedule, boolean isOverdue) {
        Subscription subscription = dueSchedule.getSubscription();

        InvoiceItem item = new InvoiceItem();

        // Hole Preis aus Subscription oder Product
        BigDecimal unitPrice = getPrice(subscription);

        // Erstelle aussagekräftige Beschreibung (mit Überfällig-Kennzeichnung)
        String description = createItemDescription(subscription, dueSchedule, isOverdue);

        item.setDescription(description);
        item.setQuantity(BigDecimal.ONE);
        item.setUnitPrice(unitPrice);
        item.setItemType(InvoiceItem.InvoiceItemType.SUBSCRIPTION);
        item.setTaxRate(BigDecimal.valueOf(19));

        // Setze Periode
        item.setPeriodStart(dueSchedule.getPeriodStart());
        item.setPeriodEnd(dueSchedule.getPeriodEnd());

        // Setze Produkt-Referenzen falls vorhanden
        if (subscription.getProduct() != null) {
            item.setProductCode(subscription.getProduct().getProductNumber());
            item.setProductName(subscription.getProduct().getName());
        } else {
            item.setProductName(subscription.getProductName());
        }

        item.calculateLineTotal();

        return item;
    }

    /**
     * Ermittelt den Preis aus Subscription oder Product
     */
    private BigDecimal getPrice(Subscription subscription) {
        // Priorität: 1. Subscription.monthlyPrice, 2. Product.price
        if (subscription.getMonthlyPrice() != null &&
                subscription.getMonthlyPrice().compareTo(BigDecimal.ZERO) > 0) {
            return subscription.getMonthlyPrice();
        }

        if (subscription.getProduct() != null &&
                subscription.getProduct().getPrice() != null) {
            return subscription.getProduct().getPrice();
        }

        logger.warn("Kein Preis gefunden für Subscription {}. Verwende 0.00",
                subscription.getSubscriptionNumber());
        return BigDecimal.ZERO;
    }

    /**
     * Erstellt Beschreibung für die Rechnungsposition
     * Kennzeichnet überfällige Positionen
     */
    private String createItemDescription(Subscription subscription, DueSchedule dueSchedule, boolean isOverdue) {
        String productName = subscription.getProductName() != null ?
                subscription.getProductName() :
                (subscription.getProduct() != null ? subscription.getProduct().getName() : "Abonnement");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

        String baseDescription = String.format("%s für Periode %s bis %s",
                productName,
                dueSchedule.getPeriodStart().format(formatter),
                dueSchedule.getPeriodEnd().format(formatter));

        // Füge Überfällig-Kennzeichnung hinzu
        if (isOverdue) {
            return String.format("%s (ÜBERFÄLLIG seit %s)",
                    baseDescription,
                    dueSchedule.getDueDate().format(formatter));
        }

        return baseDescription;
    }

    /**
     * Erstellt einen OpenItem für die Rechnung
     * Berücksichtigt überfällige Status
     */
    private OpenItem createOpenItemForInvoice(Invoice invoice, boolean wasOverdue) {
        OpenItem openItem = new OpenItem();
        openItem.setInvoice(invoice);
        openItem.setDescription(String.format("Offener Posten für Rechnung %s%s",
                invoice.getInvoiceNumber(),
                wasOverdue ? " (aus überfälliger Position)" : ""));
        openItem.setAmount(invoice.getTotalAmount());
        openItem.setDueDate(invoice.getDueDate());
        openItem.setStatus(OpenItem.OpenItemStatus.OPEN);

        // Prüfe ob bereits überfällig (basierend auf neuem Fälligkeitsdatum)
        if (invoice.getDueDate().isBefore(LocalDate.now())) {
            openItem.setStatus(OpenItem.OpenItemStatus.OVERDUE);
        }

        return openItem;
    }

    /**
     * Generiert eine eindeutige Batch-ID für den Rechnungslauf
     */
    private String generateBatchId() {
        return String.format("BATCH-%s-%05d",
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")),
                System.currentTimeMillis() % 100000);
    }

    /**
     * Formatiert ein Datum für die Ausgabe
     */
    private String formatDate(LocalDate date) {
        if (date == null) return "N/A";
        return date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
    }

    /**
     * Ergebnis-DTO für den Rechnungslauf
     */
    public static class InvoiceBatchResult {
        private final int createdInvoices;
        private final int processedDueSchedules;
        private final BigDecimal totalAmount;
        private final String message;

        public InvoiceBatchResult(int createdInvoices,
                                  int processedDueSchedules,
                                  BigDecimal totalAmount,
                                  String message) {
            this.createdInvoices = createdInvoices;
            this.processedDueSchedules = processedDueSchedules;
            this.totalAmount = totalAmount;
            this.message = message;
        }

        // Getter
        public int getCreatedInvoices() { return createdInvoices; }
        public int getProcessedDueSchedules() { return processedDueSchedules; }
        public BigDecimal getTotalAmount() { return totalAmount; }
        public String getMessage() { return message; }

        @Override
        public String toString() {
            return message;
        }
    }
}