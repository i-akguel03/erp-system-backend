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
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service für den automatisierten Rechnungslauf mit vollständigem Vorgang-Logging.
 *
 * GARANTIERTE WORKFLOW-KETTE:
 * 1. Startet Vorgang für Rechnungslauf
 * 2. Prüft ALLE aktiven DueSchedules bis zum Stichtag (STANDARD: alle Monate davor)
 * 3. Erstellt für JEDE noch nicht abgerechnete Fälligkeit eine separate Rechnung
 * 4. Generiert für jede Rechnung GARANTIERT einen OpenItem
 * 5. Markiert DueSchedules als abgerechnet (COMPLETED)
 * 6. Verknüpft alle Entitäten mit dem Vorgang
 * 7. Schließt Vorgang ab (erfolgreich oder mit Fehlern)
 * 8. Validiert Konsistenz: DueSchedule → Invoice → OpenItem (1:1:1)
 *
 * NEU: Vollständiges Logging aller Rechnungsläufe als Vorgänge!
 */
@Service
@Transactional
public class InvoiceBatchService {

    private static final Logger logger = LoggerFactory.getLogger(InvoiceBatchService.class);

    private final DueScheduleRepository dueScheduleRepository;
    private final InvoiceRepository invoiceRepository;
    private final OpenItemRepository openItemRepository;
    private final InvoiceNumberGeneratorService invoiceNumberGenerator;
    private final DueScheduleService dueScheduleService;
    private final VorgangService vorgangService;

    public InvoiceBatchService(DueScheduleRepository dueScheduleRepository,
                               InvoiceRepository invoiceRepository,
                               OpenItemRepository openItemRepository,
                               InvoiceNumberGeneratorService invoiceNumberGenerator,
                               DueScheduleService dueScheduleService,
                               VorgangService vorgangService) {
        this.dueScheduleRepository = dueScheduleRepository;
        this.invoiceRepository = invoiceRepository;
        this.openItemRepository = openItemRepository;
        this.invoiceNumberGenerator = invoiceNumberGenerator;
        this.dueScheduleService = dueScheduleService;
        this.vorgangService = vorgangService;
    }

    /**
     * Hauptmethode für den Rechnungslauf - STANDARD: Alle offenen Monate bis Stichtag
     */
    @Transactional
    public InvoiceBatchResult runInvoiceBatch(LocalDate billingDate) {
        return runInvoiceBatch(billingDate, true);
    }

    /**
     * Erweiterte Hauptmethode für den Rechnungslauf mit Steuerungsparameter und Vorgang-Logging
     */
    @Transactional
    public InvoiceBatchResult runInvoiceBatch(LocalDate billingDate, boolean includeAllPreviousMonths) {
        // 1. VORGANG STARTEN
        String titel = String.format("Rechnungslauf zum %s (%s)",
                billingDate,
                includeAllPreviousMonths ? "alle offenen Monate" : "nur exakter Stichtag");

        String beschreibung = String.format(
                "Automatischer Rechnungslauf mit Stichtag %s. Modus: %s",
                billingDate,
                includeAllPreviousMonths ? "alle offenen Monate bis Stichtag" : "nur exakter Stichtag"
        );

        Vorgang vorgang = vorgangService.starteAutomatischenVorgang(VorgangTyp.RECHNUNGSLAUF, titel);
        vorgang.setBeschreibung(beschreibung);

        // Batch-ID aus Vorgang ableiten
        String batchId = "BATCH-" + vorgang.getVorgangsnummer();

        try {
            logger.info("==================== RECHNUNGSLAUF START ====================");
            logger.info("Vorgang: {} - {}", vorgang.getVorgangsnummer(), titel);
            logger.info("Stichtag: {}", billingDate);
            logger.info("Batch-ID: {}", batchId);
            logger.info("=============================================================");

            // 2. FÄLLIGKEITEN ERMITTELN
            List<DueSchedule> openDueSchedules = includeAllPreviousMonths ?
                    getAllOpenDueSchedulesUntilDate(billingDate) :
                    getExactDateDueSchedules(billingDate);

            if (openDueSchedules.isEmpty()) {
                String message = String.format("Keine offenen Fälligkeiten gefunden für Stichtag: %s (%s)",
                        billingDate,
                        includeAllPreviousMonths ? "inkl. alle vorherigen Monate" : "nur exakter Stichtag");

                logger.info(message);

                // Vorgang erfolgreich abschließen (auch wenn keine Daten)
                vorgangService.vorgangErfolgreichAbschliessen(vorgang.getId(), 0, 0, 0, BigDecimal.ZERO);

                return new InvoiceBatchResult(0, 0, BigDecimal.ZERO, message, vorgang.getVorgangsnummer());
            }

            // 3. ANALYSE DER FÄLLIGKEITEN
            InvoiceBatchAnalysis analysis = analyzeDueSchedules(openDueSchedules, billingDate);
            logAnalysis(analysis);

            // 4. BATCH-VERARBEITUNG MIT VORGANG-TRACKING
            InvoiceBatchResult result = processBatchWithVorgang(
                    openDueSchedules, billingDate, batchId, analysis, vorgang);

            return result;

        } catch (Exception e) {
            logger.error("KRITISCHER FEHLER im Rechnungslauf", e);

            // Vorgang mit Fehler abschließen
            vorgangService.vorgangMitFehlerAbschliessen(vorgang.getId(),
                    "Kritischer Fehler: " + e.getMessage());

            throw new RuntimeException("Rechnungslauf fehlgeschlagen (Vorgang: " + vorgang.getVorgangsnummer() + ")", e);
        }
    }

    /**
     * Verarbeitet den Batch mit vollständigem Vorgang-Tracking
     */
    private InvoiceBatchResult processBatchWithVorgang(List<DueSchedule> openDueSchedules,
                                                       LocalDate billingDate,
                                                       String batchId,
                                                       InvoiceBatchAnalysis analysis,
                                                       Vorgang vorgang) {

        int createdInvoices = 0;
        int createdOpenItems = 0;
        int processedDueSchedules = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<String> errors = new ArrayList<>();
        List<String> successfullyProcessed = new ArrayList<>();

        logger.info("==================== BATCH-VERARBEITUNG START ====================");

        // Verarbeite jede Fälligkeit mit Vorgang-Verknüpfung
        for (DueSchedule dueSchedule : openDueSchedules) {
            try {
                boolean isOverdue = dueSchedule.getDueDate().isBefore(billingDate);
                String monthYear = dueSchedule.getDueDate().getYear() + "-" +
                        String.format("%02d", dueSchedule.getDueDate().getMonthValue());

                logger.debug("Verarbeite Fälligkeit: {} (fällig: {}, Monat: {})",
                        dueSchedule.getDueNumber(), formatDate(dueSchedule.getDueDate()), monthYear);

                // SCHRITT 1: Rechnung erstellen
                Invoice invoice = createInvoiceForDueSchedule(dueSchedule, billingDate, batchId, isOverdue);
                Invoice savedInvoice = invoiceRepository.save(invoice);

                // WICHTIG: Rechnung mit Vorgang verknüpfen
                savedInvoice.setVorgang(vorgang);
                invoiceRepository.save(savedInvoice);

                createdInvoices++;

                // SCHRITT 2: DueSchedule als abgerechnet markieren
                dueScheduleService.markAsCompleted(dueSchedule.getId(), savedInvoice.getId(), batchId);

                // WICHTIG: DueSchedule mit Vorgang verknüpfen
                dueSchedule.setVorgang(vorgang);
                // DueSchedule wird automatisch durch markAsCompleted gespeichert

                processedDueSchedules++;

                // SCHRITT 3: OpenItem erstellen
                OpenItem openItem = createOpenItemForInvoice(savedInvoice, isOverdue);
                OpenItem savedOpenItem = openItemRepository.save(openItem);

                // WICHTIG: OpenItem mit Vorgang verknüpfen
                savedOpenItem.setVorgang(vorgang);
                openItemRepository.save(savedOpenItem);

                createdOpenItems++;
                totalAmount = totalAmount.add(savedInvoice.getTotalAmount());

                // Erfolgs-Logging mit Monatsangabe
                String status = isOverdue ? "ÜBERFÄLLIG (" + monthYear + ")" : "AKTUELL (" + monthYear + ")";
                String successMsg = String.format("✓ %s (%s) → %s (%.2f€) → OpenItem %s",
                        dueSchedule.getDueNumber(),
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

                // Rollback-Logik
                try {
                    dueScheduleService.rollbackCompleted(dueSchedule.getId(),
                            "Rollback due to billing error: " + e.getMessage());
                    logger.warn("→ DueSchedule {} aufgrund Fehler zurückgesetzt", dueSchedule.getDueNumber());
                } catch (Exception rollbackError) {
                    logger.error("→ ROLLBACK-FEHLER für DueSchedule {}: {}",
                            dueSchedule.getDueNumber(), rollbackError.getMessage());
                    errors.add("Rollback-Fehler: " + rollbackError.getMessage());
                }
            }
        }

        logger.info("==================== BATCH-VERARBEITUNG ENDE ====================");

        // Ergebnis erstellen und Vorgang abschließen
        return createBatchResultAndCloseVorgang(
                createdInvoices, createdOpenItems, processedDueSchedules,
                totalAmount, errors, successfullyProcessed, analysis, batchId, vorgang);
    }

    /**
     * Erstellt das Batch-Ergebnis und schließt den Vorgang ab
     */
    private InvoiceBatchResult createBatchResultAndCloseVorgang(int createdInvoices, int createdOpenItems,
                                                                int processedDueSchedules, BigDecimal totalAmount,
                                                                List<String> errors, List<String> successfullyProcessed,
                                                                InvoiceBatchAnalysis analysis, String batchId,
                                                                Vorgang vorgang) {
        // Konsistenz-Validierung
        logger.info("==================== KONSISTENZ-VALIDIERUNG ====================");

        if (createdInvoices != createdOpenItems) {
            String error = String.format("KRITISCHE INKONSISTENZ: %d Rechnungen vs %d OpenItems erstellt!",
                    createdInvoices, createdOpenItems);
            logger.error("✗ {}", error);
            errors.add(error);
        }

        if (processedDueSchedules != createdInvoices) {
            String error = String.format("KRITISCHE INKONSISTENZ: %d DueSchedules vs %d Rechnungen verarbeitet!",
                    processedDueSchedules, createdInvoices);
            logger.error("✗ {}", error);
            errors.add(error);
        }

        // Erfolgsmeldungen
        if (!successfullyProcessed.isEmpty()) {
            logger.info("✓ ERFOLGREICH VERARBEITETE FÄLLIGKEITEN:");
            successfullyProcessed.forEach(msg -> logger.info("  " + msg));
        }

        // Ergebnis-Zusammenfassung
        long monthsProcessed = analysis.getMonthGroups().size();
        String message = String.format(
                "RECHNUNGSLAUF ABGESCHLOSSEN: %d Monate → %d Fälligkeiten → %d Rechnungen → %d OpenItems (davon %d überfällig), Gesamt: %.2f EUR",
                monthsProcessed, processedDueSchedules, createdInvoices, createdOpenItems,
                analysis.getOverdueCount(), totalAmount
        );

        if (!errors.isEmpty()) {
            message += String.format(" (%d KRITISCHE FEHLER aufgetreten!)", errors.size());
            logger.error("==================== KRITISCHE FEHLER ====================");
            errors.forEach(error -> logger.error("✗ " + error));
            logger.error("=========================================================");
        }

        // Vorgang-Details aktualisieren
        String metadaten = null;//String.format(
//                "{\"billingDate\":\"%s\",\"batchId\":\"%s\",\"monthsProcessed\":%d,\"includeAllPreviousMonths\":%b}",
//                billingDate.toString(), batchId, monthsProcessed, true
//        );
        vorgang.setMetadaten(metadaten);

        // Vorgang abschließen
        if (errors.isEmpty()) {
            logger.info("✓ STATUS: ERFOLGREICH - Alle Fälligkeiten wurden korrekt abgerechnet");
            vorgangService.vorgangErfolgreichAbschliessen(vorgang.getId(),
                    processedDueSchedules, createdInvoices, 0, totalAmount);
        } else {
            logger.error("✗ STATUS: FEHLER - {} Probleme aufgetreten", errors.size());

            // Erstelle Fehlerprotokoll (maximal 5 Fehler für Übersichtlichkeit)
            String fehlerprotokoll = errors.stream()
                    .limit(5)
                    .collect(Collectors.joining("; "));
            if (errors.size() > 5) {
                fehlerprotokoll += String.format("; ... und %d weitere Fehler", errors.size() - 5);
            }

            vorgangService.vorgangMitFehlerAbschliessen(vorgang.getId(), fehlerprotokoll);
        }

        logger.info("==================== RECHNUNGSLAUF ZUSAMMENFASSUNG ====================");
        logger.info(message);
        logger.info("Verarbeitete Monate: {}", String.join(", ", analysis.getMonthGroups().keySet()));
        logger.info("Vorgang: {}", vorgang.getVorgangsnummer());
        logger.info("Batch-ID: {}", batchId);
        logger.info("Verarbeitungszeit: {}", java.time.LocalDateTime.now());
        logger.info("Dauer: {} ms", vorgang.getDauerInMs());
        logger.info("=====================================================================");

        return new InvoiceBatchResult(createdInvoices, processedDueSchedules, totalAmount, message, vorgang.getVorgangsnummer());
    }

    // ===============================================================================================
    // BESTEHENDE METHODEN (unverändert)
    // ===============================================================================================

    /**
     * Holt ALLE offenen DueSchedules bis zum Stichtag (STANDARD-Verhalten)
     */
    private List<DueSchedule> getAllOpenDueSchedulesUntilDate(LocalDate billingDate) {
        List<DueSchedule> schedules = dueScheduleRepository
                .findByStatusAndDueDateLessThanEqual(DueStatus.ACTIVE, billingDate);

        logger.info("STANDARD-Modus: {} offene Fälligkeiten bis {} gefunden",
                schedules.size(), billingDate);
        return schedules;
    }

    /**
     * Holt nur DueSchedules für den exakten Stichtag (opt-in Verhalten)
     */
    private List<DueSchedule> getExactDateDueSchedules(LocalDate billingDate) {
        List<DueSchedule> schedules = dueScheduleRepository
                .findByStatusAndDueDate(DueStatus.ACTIVE, billingDate);

        logger.info("EXAKT-Modus: {} Fälligkeiten für exakten Stichtag {} gefunden",
                schedules.size(), billingDate);
        return schedules;
    }

    /**
     * Analysiert die gefundenen Fälligkeiten nach verschiedenen Kategorien
     */
    private InvoiceBatchAnalysis analyzeDueSchedules(List<DueSchedule> schedules, LocalDate billingDate) {
        long overdueCount = 0;
        long currentMonthCount = 0;
        long futureCount = 0;

        Map<String, List<DueSchedule>> monthGroups = schedules.stream()
                .collect(Collectors.groupingBy(ds ->
                        ds.getDueDate().getYear() + "-" + String.format("%02d", ds.getDueDate().getMonthValue())));

        for (DueSchedule schedule : schedules) {
            LocalDate dueDate = schedule.getDueDate();

            if (dueDate.isBefore(billingDate)) {
                overdueCount++;
            } else if (dueDate.equals(billingDate)) {
                currentMonthCount++;
            } else {
                futureCount++;
            }
        }

        return new InvoiceBatchAnalysis(
                schedules.size(),
                overdueCount,
                currentMonthCount,
                futureCount,
                monthGroups
        );
    }

    /**
     * Loggt die detaillierte Analyse der Fälligkeiten
     */
    private void logAnalysis(InvoiceBatchAnalysis analysis) {
        logger.info("DETAILLIERTE ANALYSE:");
        logger.info("- Gesamt: {} Fälligkeiten", analysis.getTotalCount());
        logger.info("- Überfällig: {} Fälligkeiten", analysis.getOverdueCount());
        logger.info("- Aktueller Stichtag: {} Fälligkeiten", analysis.getCurrentCount());

        if (analysis.getFutureCount() > 0) {
            logger.warn("- WARNUNG: {} zukünftige Fälligkeiten gefunden (sollte nicht passieren)",
                    analysis.getFutureCount());
        }

        logger.info("AUFSCHLÜSSELUNG NACH MONATEN:");
        analysis.getMonthGroups().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry ->
                        logger.info("- {}: {} Fälligkeiten", entry.getKey(), entry.getValue().size()));
    }

    // Alle anderen bestehenden Methoden bleiben unverändert...
    // (createInvoiceForDueSchedule, createInvoiceItemFromDueSchedule, getPrice,
    //  createItemDescription, createOpenItemForInvoice, formatDate, etc.)

    private Invoice createInvoiceForDueSchedule(DueSchedule dueSchedule, LocalDate billingDate, String batchId, boolean isOverdue) {
        Subscription subscription = dueSchedule.getSubscription();
        Customer customer = subscription.getContract().getCustomer();

        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(invoiceNumberGenerator.generateInvoiceNumber());
        invoice.setCustomer(customer);
        invoice.setSubscription(subscription);
        invoice.setBillingAddress(customer.getBillingAddress());
        invoice.setInvoiceDate(billingDate);
        invoice.setDueDate(billingDate.plusDays(14));
        invoice.setStatus(Invoice.InvoiceStatus.ACTIVE);
        invoice.setInvoiceType(Invoice.InvoiceType.AUTO_GENERATED);
        invoice.setInvoiceBatchId(batchId);
        invoice.setPaymentTerms("Zahlbar innerhalb von 14 Tagen");
        invoice.setTaxRate(BigDecimal.valueOf(19));

        InvoiceItem item = createInvoiceItemFromDueSchedule(dueSchedule, isOverdue);
        invoice.addInvoiceItem(item);
        invoice.calculateTotals();

        String noteTemplate = isOverdue ?
                "Automatisch erstellt am %s für ÜBERFÄLLIGE Fälligkeit %s (ursprünglich fällig: %s, Periode: %s bis %s)" :
                "Automatisch erstellt am %s für Fälligkeit %s (Periode: %s bis %s)";

        String notes = isOverdue ?
                String.format(noteTemplate, formatDate(billingDate), dueSchedule.getDueNumber(),
                        formatDate(dueSchedule.getDueDate()), formatDate(dueSchedule.getPeriodStart()),
                        formatDate(dueSchedule.getPeriodEnd())) :
                String.format(noteTemplate, formatDate(billingDate), dueSchedule.getDueNumber(),
                        formatDate(dueSchedule.getPeriodStart()), formatDate(dueSchedule.getPeriodEnd()));

        invoice.setNotes(notes);
        return invoice;
    }

    private InvoiceItem createInvoiceItemFromDueSchedule(DueSchedule dueSchedule, boolean isOverdue) {
        Subscription subscription = dueSchedule.getSubscription();
        InvoiceItem item = new InvoiceItem();

        BigDecimal unitPrice = getPrice(subscription);
        String description = createItemDescription(subscription, dueSchedule, isOverdue);

        item.setDescription(description);
        item.setQuantity(BigDecimal.ONE);
        item.setUnitPrice(unitPrice);
        item.setItemType(InvoiceItem.InvoiceItemType.SUBSCRIPTION);
        item.setTaxRate(BigDecimal.valueOf(19));
        item.setPeriodStart(dueSchedule.getPeriodStart());
        item.setPeriodEnd(dueSchedule.getPeriodEnd());

        if (subscription.getProduct() != null) {
            item.setProductCode(subscription.getProduct().getProductNumber());
            item.setProductName(subscription.getProduct().getName());
        } else {
            item.setProductName(subscription.getProductName());
        }

        item.calculateLineTotal();
        return item;
    }

    private BigDecimal getPrice(Subscription subscription) {
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

    private String createItemDescription(Subscription subscription, DueSchedule dueSchedule, boolean isOverdue) {
        String productName = subscription.getProductName() != null ?
                subscription.getProductName() :
                (subscription.getProduct() != null ? subscription.getProduct().getName() : "Abonnement");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

        String baseDescription = String.format("%s für Periode %s bis %s",
                productName,
                dueSchedule.getPeriodStart().format(formatter),
                dueSchedule.getPeriodEnd().format(formatter));

        if (isOverdue) {
            return String.format("%s ⚠ ÜBERFÄLLIG seit %s ⚠",
                    baseDescription,
                    dueSchedule.getDueDate().format(formatter));
        }

        return baseDescription;
    }

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

        if (invoice.getDueDate().isBefore(LocalDate.now())) {
            openItem.setStatus(OpenItem.OpenItemStatus.OVERDUE);
            logger.debug("OpenItem für Rechnung {} direkt als OVERDUE markiert",
                    invoice.getInvoiceNumber());
        }

        return openItem;
    }

    private String formatDate(LocalDate date) {
        if (date == null) return "N/A";
        return date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
    }

    // ===============================================================================================
    // VORSCHAU-METHODEN (erweitert für Vorgang-Info)
    // ===============================================================================================

    @Transactional(readOnly = true)
    public boolean canRunBillingBatch(LocalDate billingDate) {
        return canRunBillingBatch(billingDate, true);
    }

    @Transactional(readOnly = true)
    public boolean canRunBillingBatch(LocalDate billingDate, boolean includeAllPreviousMonths) {
        long activeDueSchedules = includeAllPreviousMonths ?
                dueScheduleRepository.countByStatusAndDueDateLessThanEqual(DueStatus.ACTIVE, billingDate) :
                dueScheduleRepository.countByStatusAndDueDateLessThanEqual(DueStatus.ACTIVE, billingDate);
        return activeDueSchedules > 0;
    }

    @Transactional(readOnly = true)
    public InvoiceBatchPreview previewBillingBatch(LocalDate billingDate) {
        return previewBillingBatch(billingDate, true);
    }

    @Transactional(readOnly = true)
    public InvoiceBatchPreview previewBillingBatch(LocalDate billingDate, boolean includeAllPreviousMonths) {
        List<DueSchedule> openDueSchedules = includeAllPreviousMonths ?
                getAllOpenDueSchedulesUntilDate(billingDate) :
                getExactDateDueSchedules(billingDate);

        InvoiceBatchAnalysis analysis = analyzeDueSchedules(openDueSchedules, billingDate);

        BigDecimal estimatedTotal = openDueSchedules.stream()
                .map(ds -> getPrice(ds.getSubscription()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new InvoiceBatchPreview(
                analysis.getTotalCount(),
                analysis.getOverdueCount(),
                analysis.getCurrentCount(),
                estimatedTotal,
                billingDate,
                includeAllPreviousMonths,
                analysis.getMonthGroups()
        );
    }

    // ===============================================================================================
    // DTO-KLASSEN (erweitert mit Vorgang-Info)
    // ===============================================================================================

    private static class InvoiceBatchAnalysis {
        private final int totalCount;
        private final long overdueCount;
        private final long currentCount;
        private final long futureCount;
        private final Map<String, List<DueSchedule>> monthGroups;

        public InvoiceBatchAnalysis(int totalCount, long overdueCount, long currentCount,
                                    long futureCount, Map<String, List<DueSchedule>> monthGroups) {
            this.totalCount = totalCount;
            this.overdueCount = overdueCount;
            this.currentCount = currentCount;
            this.futureCount = futureCount;
            this.monthGroups = monthGroups;
        }

        public int getTotalCount() { return totalCount; }
        public long getOverdueCount() { return overdueCount; }
        public long getCurrentCount() { return currentCount; }
        public long getFutureCount() { return futureCount; }
        public Map<String, List<DueSchedule>> getMonthGroups() { return monthGroups; }
    }

    public static class InvoiceBatchPreview {
        private final int totalDueSchedules;
        private final long overdueCount;
        private final long currentCount;
        private final BigDecimal estimatedAmount;
        private final LocalDate billingDate;
        private final boolean includeAllPreviousMonths;
        private final Map<String, List<DueSchedule>> monthGroups;

        public InvoiceBatchPreview(int totalDueSchedules, long overdueCount, long currentCount,
                                   BigDecimal estimatedAmount, LocalDate billingDate,
                                   boolean includeAllPreviousMonths,
                                   Map<String, List<DueSchedule>> monthGroups) {
            this.totalDueSchedules = totalDueSchedules;
            this.overdueCount = overdueCount;
            this.currentCount = currentCount;
            this.estimatedAmount = estimatedAmount;
            this.billingDate = billingDate;
            this.includeAllPreviousMonths = includeAllPreviousMonths;
            this.monthGroups = monthGroups;
        }

        public int getTotalDueSchedules() { return totalDueSchedules; }
        public long getOverdueCount() { return overdueCount; }
        public long getCurrentCount() { return currentCount; }
        public BigDecimal getEstimatedAmount() { return estimatedAmount; }
        public LocalDate getBillingDate() { return billingDate; }
        public boolean isIncludeAllPreviousMonths() { return includeAllPreviousMonths; }
        public Map<String, List<DueSchedule>> getMonthGroups() { return monthGroups; }
        public int getMonthCount() { return monthGroups.size(); }

        @Override
        public String toString() {
            String mode = includeAllPreviousMonths ? "alle offenen Monate" : "nur exakter Stichtag";
            return String.format("Rechnungslauf-Vorschau für %s (%s): %d Monate, %d Fälligkeiten (%d überfällig, %d aktuell), geschätzt %.2f EUR",
                    billingDate, mode, getMonthCount(), totalDueSchedules, overdueCount, currentCount, estimatedAmount);
        }
    }

    /**
     * Erweiterte Result-Klasse mit Vorgang-Information
     */
    public static class InvoiceBatchResult {
        private final int createdInvoices;
        private final int processedDueSchedules;
        private final BigDecimal totalAmount;
        private final String message;
        private final String vorgangsnummer;

        public InvoiceBatchResult(int createdInvoices, int processedDueSchedules,
                                  BigDecimal totalAmount, String message, String vorgangsnummer) {
            this.createdInvoices = createdInvoices;
            this.processedDueSchedules = processedDueSchedules;
            this.totalAmount = totalAmount;
            this.message = message;
            this.vorgangsnummer = vorgangsnummer;
        }

        // Rückwärtskompatibilitäts-Konstruktor (ohne Vorgangsnummer)
        public InvoiceBatchResult(int createdInvoices, int processedDueSchedules,
                                  BigDecimal totalAmount, String message) {
            this(createdInvoices, processedDueSchedules, totalAmount, message, null);
        }

        public int getCreatedInvoices() { return createdInvoices; }
        public int getProcessedDueSchedules() { return processedDueSchedules; }
        public BigDecimal getTotalAmount() { return totalAmount; }
        public String getMessage() { return message; }
        public String getVorgangsnummer() { return vorgangsnummer; }

        /**
         * Prüft ob ein Vorgang mit diesem Rechnungslauf verknüpft ist
         */
        public boolean hasVorgang() {
            return vorgangsnummer != null && !vorgangsnummer.trim().isEmpty();
        }

        @Override
        public String toString() {
            if (hasVorgang()) {
                return message + " (Vorgang: " + vorgangsnummer + ")";
            }
            return message;
        }
    }
}