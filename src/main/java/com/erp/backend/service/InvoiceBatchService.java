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
 * GARANTIERTE WORKFLOW-KETTE:
 * 1. Prüft ALLE aktiven DueSchedules bis zum Stichtag (inkl. überfällige)
 * 2. Erstellt für JEDE noch nicht abgerechnete Fälligkeit eine separate Rechnung
 * 3. Generiert für jede Rechnung GARANTIERT einen OpenItem
 * 4. Markiert DueSchedules als abgerechnet (COMPLETED)
 * 5. Validiert Konsistenz: DueSchedule → Invoice → OpenItem (1:1:1)
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
     * GARANTIERT: Für jede abgerechnete DueSchedule wird eine Rechnung UND ein OpenItem erstellt
     * WICHTIG: Erfasst ALLE aktiven Fälligkeiten bis zum Stichtag, auch überfällige!
     *
     * @param billingDate Stichtag für die Abrechnung
     * @return Ergebnis des Rechnungslaufs
     */
    @Transactional
    public InvoiceBatchResult runInvoiceBatch(LocalDate billingDate) {
        logger.info("==================== RECHNUNGSLAUF START ====================");
        logger.info("Stichtag: {} (inkl. aller überfälligen Positionen)", billingDate);
        logger.info("=============================================================");

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

        // Analysiere gefundene Fälligkeiten
        long overdueCount = openDueSchedules.stream()
                .filter(ds -> ds.getDueDate().isBefore(billingDate))
                .count();

        long currentMonthCount = openDueSchedules.stream()
                .filter(ds -> ds.getDueDate().equals(billingDate) ||
                        (ds.getDueDate().getMonth() == billingDate.getMonth() &&
                                ds.getDueDate().getYear() == billingDate.getYear()))
                .count();

        logger.info("ANALYSE: {} Fälligkeiten gesamt (davon {} überfällig, {} aktueller Monat)",
                openDueSchedules.size(), overdueCount, currentMonthCount);

        // Batch-Verarbeitung
        int createdInvoices = 0;
        int createdOpenItems = 0;
        int processedDueSchedules = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<String> errors = new ArrayList<>();
        List<String> successfullyProcessed = new ArrayList<>();

        logger.info("==================== BATCH-VERARBEITUNG START ====================");

        // 2. Erstelle für JEDE Fälligkeit GARANTIERT eine Rechnung UND einen OpenItem
        for (DueSchedule dueSchedule : openDueSchedules) {
            try {
                logger.debug("Verarbeite Fälligkeit: {} (fällig: {})",
                        dueSchedule.getDueNumber(), formatDate(dueSchedule.getDueDate()));

                // SCHRITT 1: DueSchedule sofort als abgerechnet markieren
                dueSchedule.markAsCompleted();
                dueSchedule.setInvoiceBatchId(batchId);
                DueSchedule savedDueSchedule = dueScheduleRepository.save(dueSchedule);
                processedDueSchedules++;

                // SCHRITT 2: Rechnung erstellen (GARANTIERT)
                boolean isOverdue = dueSchedule.getDueDate().isBefore(billingDate);
                Invoice invoice = createInvoiceForDueSchedule(savedDueSchedule, billingDate, batchId, isOverdue);
                Invoice savedInvoice = invoiceRepository.save(invoice);
                createdInvoices++;

                // SCHRITT 3: DueSchedule mit Rechnung verknüpfen
                savedDueSchedule.setInvoiceId(savedInvoice.getId());
                dueScheduleRepository.save(savedDueSchedule);

                // SCHRITT 4: OpenItem erstellen (GARANTIERT)
                OpenItem openItem = createOpenItemForInvoice(savedInvoice, isOverdue);
                OpenItem savedOpenItem = openItemRepository.save(openItem);
                createdOpenItems++;

                totalAmount = totalAmount.add(savedInvoice.getTotalAmount());

                // Erfolgs-Logging
                String status = isOverdue ? "ÜBERFÄLLIG" : "AKTUELL";
                String successMsg = String.format("✓ %s (%s) → %s (%.2f€) → OpenItem %s",
                        savedDueSchedule.getDueNumber(),
                        status,
                        savedInvoice.getInvoiceNumber(),
                        savedInvoice.getTotalAmount(),
                        savedOpenItem.getId());

                successfullyProcessed.add(successMsg);
                logger.info(successMsg);

            } catch (Exception e) {
                String error = String.format("✗ KRITISCHER FEHLER bei Fälligkeit %s (fällig: %s): %s",
                        dueSchedule.getDueNumber(),
                        formatDate(dueSchedule.getDueDate()),
                        e.getMessage());
                logger.error(error, e);
                errors.add(error);

                // Bei Fehlern: DueSchedule wieder auf ACTIVE setzen (Rollback)
                try {
                    dueSchedule.setStatus(DueStatus.ACTIVE);
                    dueSchedule.setInvoiceBatchId(null);
                    dueSchedule.setInvoiceId(null);
                    dueScheduleRepository.save(dueSchedule);
                    logger.warn("→ DueSchedule {} aufgrund Fehler wieder auf ACTIVE zurückgesetzt",
                            dueSchedule.getDueNumber());
                } catch (Exception rollbackError) {
                    logger.error("→ ROLLBACK-FEHLER für DueSchedule {}: {}",
                            dueSchedule.getDueNumber(), rollbackError.getMessage());
                }
            }
        }

        logger.info("==================== BATCH-VERARBEITUNG ENDE ====================");

        // 3. KRITISCHE VALIDIERUNG: Stelle sicher dass alle Zahlen stimmen
        logger.info("==================== KONSISTENZ-VALIDIERUNG ====================");

        if (createdInvoices != createdOpenItems) {
            logger.error("✗ KRITISCHE INKONSISTENZ: {} Rechnungen vs {} OpenItems erstellt!",
                    createdInvoices, createdOpenItems);
            errors.add("Inkonsistenz: " + createdInvoices + " Rechnungen vs " + createdOpenItems + " OpenItems");
        }

        if (processedDueSchedules != createdInvoices) {
            logger.error("✗ KRITISCHE INKONSISTENZ: {} DueSchedules vs {} Rechnungen verarbeitet!",
                    processedDueSchedules, createdInvoices);
            errors.add("Inkonsistenz: " + processedDueSchedules + " DueSchedules vs " + createdInvoices + " Rechnungen");
        }

        // Erfolgsmeldungen zusammenfassen
        if (!successfullyProcessed.isEmpty()) {
            logger.info("✓ ERFOLGREICH VERARBEITETE FÄLLIGKEITEN:");
            successfullyProcessed.forEach(msg -> logger.info("  " + msg));
        }

        // 4. Ergebnis-Zusammenfassung
        String message = String.format(
                "RECHNUNGSLAUF ABGESCHLOSSEN: %d Fälligkeiten → %d Rechnungen → %d OpenItems (davon %d überfällig), Gesamt: %.2f EUR",
                processedDueSchedules, createdInvoices, createdOpenItems, overdueCount, totalAmount
        );

        if (!errors.isEmpty()) {
            message += String.format(" (%d KRITISCHE FEHLER aufgetreten!)", errors.size());
            logger.error("==================== KRITISCHE FEHLER ====================");
            errors.forEach(error -> logger.error("✗ " + error));
            logger.error("=========================================================");
        }

        logger.info("==================== RECHNUNGSLAUF ZUSAMMENFASSUNG ====================");
        logger.info(message);
        logger.info("Batch-ID: {}", batchId);
        logger.info("Verarbeitungszeit: {}", java.time.LocalDateTime.now());

        if (errors.isEmpty()) {
            logger.info("✓ STATUS: ERFOLGREICH - Alle Fälligkeiten wurden korrekt abgerechnet");
        } else {
            logger.error("✗ STATUS: FEHLER - {} Probleme aufgetreten", errors.size());
        }
        logger.info("=====================================================================");

        return new InvoiceBatchResult(createdInvoices, processedDueSchedules, totalAmount, message);
    }

    /**
     * Erstellt eine Rechnung für eine einzelne Fälligkeit
     * Berücksichtigt auch überfällige Positionen mit spezieller Kennzeichnung
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

        // Angepasste Notiz je nach Status (überfällig vs. normal)
        String noteTemplate = isOverdue ?
                "Automatisch erstellt am %s für ÜBERFÄLLIGE Fälligkeit %s (ursprünglich fällig: %s, Periode: %s bis %s)" :
                "Automatisch erstellt am %s für Fälligkeit %s (Periode: %s bis %s)";

        String notes = isOverdue ?
                String.format(noteTemplate,
                        formatDate(billingDate),
                        dueSchedule.getDueNumber(),
                        formatDate(dueSchedule.getDueDate()),
                        formatDate(dueSchedule.getPeriodStart()),
                        formatDate(dueSchedule.getPeriodEnd())) :
                String.format(noteTemplate,
                        formatDate(billingDate),
                        dueSchedule.getDueNumber(),
                        formatDate(dueSchedule.getPeriodStart()),
                        formatDate(dueSchedule.getPeriodEnd()));

        invoice.setNotes(notes);

        return invoice;
    }

    /**
     * Erstellt eine Rechnungsposition aus einer einzelnen Fälligkeit
     * Kennzeichnet überfällige Positionen deutlich in der Beschreibung
     */
    private InvoiceItem createInvoiceItemFromDueSchedule(DueSchedule dueSchedule, boolean isOverdue) {
        Subscription subscription = dueSchedule.getSubscription();

        InvoiceItem item = new InvoiceItem();

        // Hole Preis aus Subscription oder Product (mit Fallback)
        BigDecimal unitPrice = getPrice(subscription);

        // Erstelle aussagekräftige Beschreibung mit Überfällig-Kennzeichnung
        String description = createItemDescription(subscription, dueSchedule, isOverdue);

        item.setDescription(description);
        item.setQuantity(BigDecimal.ONE);
        item.setUnitPrice(unitPrice);
        item.setItemType(InvoiceItem.InvoiceItemType.SUBSCRIPTION);
        item.setTaxRate(BigDecimal.valueOf(19));

        // Setze Periode für nachvollziehbare Abrechnung
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
     * Ermittelt den Preis aus Subscription oder Product mit Fallback-Strategie
     */
    private BigDecimal getPrice(Subscription subscription) {
        // Priorität: 1. Subscription.monthlyPrice, 2. Product.price, 3. Fallback 0.00
        if (subscription.getMonthlyPrice() != null &&
                subscription.getMonthlyPrice().compareTo(BigDecimal.ZERO) > 0) {
            return subscription.getMonthlyPrice();
        }

        if (subscription.getProduct() != null &&
                subscription.getProduct().getPrice() != null &&
                subscription.getProduct().getPrice().compareTo(BigDecimal.ZERO) > 0) {
            return subscription.getProduct().getPrice();
        }

        logger.warn("WARNUNG: Kein Preis gefunden für Subscription {}. Verwende 0.00 EUR",
                subscription.getSubscriptionNumber());
        return BigDecimal.ZERO;
    }

    /**
     * Erstellt eine aussagekräftige Beschreibung für die Rechnungsposition
     * Kennzeichnet überfällige Positionen deutlich für bessere Nachverfolgung
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

        // Füge deutliche Überfällig-Kennzeichnung hinzu
        if (isOverdue) {
            return String.format("%s ⚠ ÜBERFÄLLIG seit %s ⚠",
                    baseDescription,
                    dueSchedule.getDueDate().format(formatter));
        }

        return baseDescription;
    }

    /**
     * Erstellt einen OpenItem für die Rechnung
     * Berücksichtigt überfällige Status und setzt korrekte Anfangswerte
     */
    private OpenItem createOpenItemForInvoice(Invoice invoice, boolean wasOverdue) {
        OpenItem openItem = new OpenItem();
        openItem.setInvoice(invoice);
        openItem.setDescription(String.format("Offener Posten für Rechnung %s%s",
                invoice.getInvoiceNumber(),
                wasOverdue ? " (aus überfälliger Fälligkeit)" : ""));
        openItem.setAmount(invoice.getTotalAmount());
        openItem.setDueDate(invoice.getDueDate());
        openItem.setStatus(OpenItem.OpenItemStatus.OPEN);
        openItem.setPaidAmount(BigDecimal.ZERO);

        // Prüfe ob bereits bei Erstellung überfällig (basierend auf neuem Fälligkeitsdatum)
        if (invoice.getDueDate().isBefore(LocalDate.now())) {
            openItem.setStatus(OpenItem.OpenItemStatus.OVERDUE);
            logger.debug("OpenItem für Rechnung {} direkt als OVERDUE markiert (Fälligkeitsdatum in Vergangenheit)",
                    invoice.getInvoiceNumber());
        }

        return openItem;
    }

    /**
     * Generiert eine eindeutige Batch-ID für den Rechnungslauf
     * Format: BATCH-YYYYMMDD-XXXXX
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

    // ===============================================================================================
    // ZUSÄTZLICHE SERVICE-METHODEN
    // ===============================================================================================

    /**
     * Prüft ob ein Rechnungslauf für den Stichtag möglich ist
     */
    @Transactional(readOnly = true)
    public boolean canRunBillingBatch(LocalDate billingDate) {
        long activeDueSchedules = dueScheduleRepository
                .countByStatusAndDueDateLessThanEqual(DueStatus.ACTIVE, billingDate);
        return activeDueSchedules > 0;
    }

    /**
     * Vorschau: Zeigt was ein Rechnungslauf verarbeiten würde (ohne Ausführung)
     */
    @Transactional(readOnly = true)
    public InvoiceBatchPreview previewBillingBatch(LocalDate billingDate) {
        List<DueSchedule> openDueSchedules = dueScheduleRepository
                .findByStatusAndDueDateLessThanEqual(DueStatus.ACTIVE, billingDate);

        long overdueCount = openDueSchedules.stream()
                .filter(ds -> ds.getDueDate().isBefore(billingDate))
                .count();

        BigDecimal estimatedTotal = openDueSchedules.stream()
                .map(ds -> getPrice(ds.getSubscription()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new InvoiceBatchPreview(
                openDueSchedules.size(),
                overdueCount,
                openDueSchedules.size() - overdueCount,
                estimatedTotal,
                billingDate
        );
    }

    // ===============================================================================================
    // RESULT-KLASSEN
    // ===============================================================================================

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

    /**
     * Vorschau-DTO für den geplanten Rechnungslauf
     */
    public static class InvoiceBatchPreview {
        private final int totalDueSchedules;
        private final long overdueCount;
        private final long currentCount;
        private final BigDecimal estimatedAmount;
        private final LocalDate billingDate;

        public InvoiceBatchPreview(int totalDueSchedules,
                                   long overdueCount,
                                   long currentCount,
                                   BigDecimal estimatedAmount,
                                   LocalDate billingDate) {
            this.totalDueSchedules = totalDueSchedules;
            this.overdueCount = overdueCount;
            this.currentCount = currentCount;
            this.estimatedAmount = estimatedAmount;
            this.billingDate = billingDate;
        }

        // Getter
        public int getTotalDueSchedules() { return totalDueSchedules; }
        public long getOverdueCount() { return overdueCount; }
        public long getCurrentCount() { return currentCount; }
        public BigDecimal getEstimatedAmount() { return estimatedAmount; }
        public LocalDate getBillingDate() { return billingDate; }

        @Override
        public String toString() {
            return String.format("Rechnungslauf-Vorschau für %s: %d Fälligkeiten (%d überfällig, %d aktuell), geschätzt %.2f EUR",
                    billingDate, totalDueSchedules, overdueCount, currentCount, estimatedAmount);
        }
    }
}