package com.erp.backend.service.init;

import com.erp.backend.domain.*;
import com.erp.backend.repository.*;
import com.erp.backend.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Random;

/**
 * INITIALIZER FÜR ABRECHNUNGSDATEN
 *
 * Verantwortlich für:
 * - Fälligkeitspläne (DueSchedules) basierend auf aktiven Abonnements
 * - Sample-Rechnungen für Testing
 * - OpenItems für Zahlungsmanagement
 * - Automatischer Rechnungslauf (je nach InitMode)
 */
@Service
@Transactional
public class BillingDataInitializer {

    private static final Logger logger = LoggerFactory.getLogger(BillingDataInitializer.class);

    // Repository-Dependencies
    private final SubscriptionRepository subscriptionRepository;
    private final DueScheduleRepository dueScheduleRepository;
    private final InvoiceRepository invoiceRepository;
    private final OpenItemRepository openItemRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;

    // Service-Dependencies
    private final VorgangService vorgangService;
    private final NumberGeneratorService numberGeneratorService;
    private final InvoiceService invoiceService;
    private final InvoiceBatchService invoiceBatchService;

    private final Random random = new Random();

    /**
     * KONSTRUKTOR mit Dependency Injection
     */
    public BillingDataInitializer(SubscriptionRepository subscriptionRepository,
                                  DueScheduleRepository dueScheduleRepository,
                                  InvoiceRepository invoiceRepository,
                                  OpenItemRepository openItemRepository,
                                  CustomerRepository customerRepository,
                                  ProductRepository productRepository,
                                  VorgangService vorgangService,
                                  NumberGeneratorService numberGeneratorService,
                                  InvoiceService invoiceService,
                                  InvoiceBatchService invoiceBatchService) {
        this.subscriptionRepository = subscriptionRepository;
        this.dueScheduleRepository = dueScheduleRepository;
        this.invoiceRepository = invoiceRepository;
        this.openItemRepository = openItemRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.vorgangService = vorgangService;
        this.numberGeneratorService = numberGeneratorService;
        this.invoiceService = invoiceService;
        this.invoiceBatchService = invoiceBatchService;
    }

    /**
     * HAUPTMETHODE: Abrechnungsdaten initialisieren
     */
    public void initializeBillingData(InitConfig config, InitMode mode, LocalDate billingDate) {
        logger.info("Starte Abrechnungsdaten-Initialisierung... (Stichtag: {})", billingDate);
        logger.info("Modus: {}", mode.getDescription());

        Vorgang vorgang = vorgangService.starteAutomatischenVorgang(
                VorgangTyp.DATENIMPORT, "Abrechnungsdaten-Import (Fälligkeiten, Rechnungen)"
        );

        try {
            int totalOperations = 0;
            int schedulesCreated = 0;
            int invoicesCreated = 0;
            int openItemsCreated = 0;

            // 1. Fälligkeitspläne erzeugen
            if (mode != InitMode.BASIC && mode != InitMode.CONTRACTS) {
                schedulesCreated = initializeDueSchedules(config);
                totalOperations++;

                // 2. Sample-Rechnungen erstellen (nur bei bestimmten Modi)
                if (mode == InitMode.INVOICES_MANUAL || mode == InitMode.FULL || mode == InitMode.FULL_WITH_BILLING) {
                    invoicesCreated = createSampleInvoices(config);
                    openItemsCreated = createSampleOpenItems(config);
                    totalOperations += 2;
                }

                // 3. Automatischer Rechnungslauf (nur bei FULL Modi)
                if (mode == InitMode.FULL || mode == InitMode.FULL_WITH_BILLING) {
                    LocalDate stichtag = billingDate != null ? billingDate : LocalDate.now();
                    runAutomaticBillingProcess(stichtag);
                    totalOperations++;
                }
            }

            // Vorgang erfolgreich abschließen
            vorgangService.vorgangErfolgreichAbschliessen(vorgang.getId(),
                    totalOperations, totalOperations, 0, null);

            logger.info("✓ Abrechnungsdaten-Initialisierung abgeschlossen:");
            logger.info("   - Fälligkeitspläne: {}", schedulesCreated);
            logger.info("   - Sample-Rechnungen: {}", invoicesCreated);
            logger.info("   - Sample-OpenItems: {}", openItemsCreated);

        } catch (Exception e) {
            logger.error("✗ Fehler bei Abrechnungsdaten-Initialisierung", e);
            vorgangService.vorgangMitFehlerAbschliessen(vorgang.getId(), e.getMessage());
            throw e;
        }
    }

    /**
     * PRIVATE METHODE: Fälligkeitspläne initialisieren
     */
    private int initializeDueSchedules(InitConfig config) {
        if (dueScheduleRepository.count() > 0) {
            logger.info("Fälligkeitspläne bereits vorhanden - überspringe Initialisierung");
            return (int) dueScheduleRepository.count();
        }

        logger.info("Initialisiere Fälligkeitspläne...");
        logger.info("Status-Verteilung: {:.0f}% ACTIVE, {:.0f}% COMPLETED, {:.0f}% PAUSED",
                config.getActiveDueScheduleRatio() * 100,
                config.getCompletedDueScheduleRatio() * 100,
                config.getPausedDueScheduleRatio() * 100);

        List<Subscription> activeSubscriptions = subscriptionRepository.findBySubscriptionStatus(SubscriptionStatus.ACTIVE);
        if (activeSubscriptions.isEmpty()) {
            logger.warn("Keine aktiven Abonnements für Fälligkeitspläne gefunden");
            return 0;
        }

        int totalSchedulesCreated = 0;
        int activeCount = 0;
        int completedCount = 0;
        int pausedCount = 0;

        for (Subscription subscription : activeSubscriptions) {
            LocalDate subscriptionStart = subscription.getStartDate();

            // 12 Monate ab Abonnement-Start
            for (int month = 0; month < 12; month++) {
                LocalDate periodStart = subscriptionStart.plusMonths(month);
                LocalDate periodEnd = periodStart.plusMonths(1).minusDays(1);
                LocalDate dueDate = periodStart;

                DueSchedule dueSchedule = new DueSchedule(dueDate, periodStart, periodEnd, subscription);
                dueSchedule.setDueNumber(numberGeneratorService.generateDueNumber());

                // Status basierend auf Konfiguration setzen
                double randomValue = random.nextDouble();

                if (dueDate.isBefore(LocalDate.now())) {
                    // Vergangene Fälligkeiten: Konfiguration anwenden
                    if (randomValue < config.getCompletedDueScheduleRatio()) {
                        dueSchedule.markAsCompleted();
                        completedCount++;
                    } else if (randomValue < config.getCompletedDueScheduleRatio() + config.getPausedDueScheduleRatio()) {
                        dueSchedule.pause();
                        pausedCount++;
                    } else {
                        // Bleibt ACTIVE (überfällig)
                        activeCount++;
                    }
                } else {
                    // Zukünftige Fälligkeiten: meist aktiv, aber konfigurierbar
                    if (randomValue < config.getActiveDueScheduleRatio()) {
                        // Bleibt ACTIVE
                        activeCount++;
                    } else {
                        dueSchedule.pause();
                        pausedCount++;
                    }
                }

                dueScheduleRepository.save(dueSchedule);
                totalSchedulesCreated++;
            }
        }

        logger.info("✓ {} Fälligkeitspläne erstellt: {} ACTIVE, {} COMPLETED, {} PAUSED",
                totalSchedulesCreated, activeCount, completedCount, pausedCount);
        return totalSchedulesCreated;
    }

    /**
     * PRIVATE METHODE: Sample-Rechnungen erstellen
     */
    private int createSampleInvoices(InitConfig config) {
        if (invoiceRepository.count() > 0) {
            logger.info("Rechnungen bereits vorhanden - überspringe Sample-Erstellung");
            return (int) invoiceRepository.count();
        }

        logger.info("Erstelle Sample-Rechnungen...");
        logger.info("Status-Verteilung: {:.0f}% ACTIVE, {:.0f}% DRAFT, {:.0f}% SENT, {:.0f}% CANCELLED",
                config.getActiveInvoiceRatio() * 100,
                config.getDraftInvoiceRatio() * 100,
                config.getSentInvoiceRatio() * 100,
                config.getCancelledInvoiceRatio() * 100);

        List<Customer> customers = customerRepository.findAll();
        List<Product> products = productRepository.findAll();
        List<Subscription> subscriptions = subscriptionRepository.findAll();

        if (customers.isEmpty() || products.isEmpty()) {
            logger.warn("Keine Kunden oder Produkte für Rechnungen gefunden");
            return 0;
        }

        final int numberOfInvoices = 30;
        int invoicesCreated = 0;
        int activeCount = 0;
        int draftCount = 0;
        int sentCount = 0;
        int cancelledCount = 0;

        for (int i = 1; i <= numberOfInvoices; i++) {
            try {
                Customer customer = customers.get(random.nextInt(customers.size()));
                Product product = products.get(random.nextInt(products.size()));

                Invoice invoice = new Invoice();
                invoice.setCustomer(customer);
                invoice.setBillingAddress(customer.getBillingAddress());
                invoice.setInvoiceDate(LocalDate.now().minusDays(random.nextInt(90)));
                invoice.setDueDate(invoice.getInvoiceDate().plusDays(14 + random.nextInt(16)));

                // Optional: Subscription verknüpfen (50% Chance)
                if (!subscriptions.isEmpty() && random.nextDouble() < 0.5) {
                    List<Subscription> customerSubscriptions = subscriptions.stream()
                            .filter(sub -> sub.getContract().getCustomer().getId().equals(customer.getId()))
                            .toList();

                    if (!customerSubscriptions.isEmpty()) {
                        invoice.setSubscription(customerSubscriptions.get(random.nextInt(customerSubscriptions.size())));
                    }
                }

                // Status basierend auf Konfiguration setzen
                double randomValue = random.nextDouble();
                if (randomValue < config.getActiveInvoiceRatio()) {
                    invoice.setStatus(Invoice.InvoiceStatus.ACTIVE);
                    activeCount++;
                } else if (randomValue < config.getActiveInvoiceRatio() + config.getDraftInvoiceRatio()) {
                    invoice.setStatus(Invoice.InvoiceStatus.DRAFT);
                    draftCount++;
                } else if (randomValue < config.getActiveInvoiceRatio() + config.getDraftInvoiceRatio() + config.getSentInvoiceRatio()) {
                    invoice.setStatus(Invoice.InvoiceStatus.SENT);
                    sentCount++;
                } else {
                    invoice.setStatus(Invoice.InvoiceStatus.CANCELLED);
                    cancelledCount++;
                }

                // InvoiceItem hinzufügen
                InvoiceItem item = new InvoiceItem();
                item.setDescription("Sample-Position: " + product.getName());
                item.setQuantity(BigDecimal.ONE);
                item.setUnitPrice(product.getPrice());
                item.setTaxRate(BigDecimal.valueOf(19));

                if (product.getProductNumber() != null) {
                    item.setProductCode(product.getProductNumber());
                }
                item.setProductName(product.getName());

                invoice.addInvoiceItem(item);

                // Manchmal zweite Position hinzufügen
                if (random.nextDouble() < 0.3) {
                    Product secondProduct = products.get(random.nextInt(products.size()));
                    InvoiceItem secondItem = new InvoiceItem();
                    secondItem.setDescription("Zusätzliche Position: " + secondProduct.getName());
                    secondItem.setQuantity(BigDecimal.ONE);
                    secondItem.setUnitPrice(secondProduct.getPrice());
                    secondItem.setTaxRate(BigDecimal.valueOf(19));
                    secondItem.setProductName(secondProduct.getName());
                    if (secondProduct.getProductNumber() != null) {
                        secondItem.setProductCode(secondProduct.getProductNumber());
                    }

                    invoice.addInvoiceItem(secondItem);
                }

                // Rechnung speichern
                invoiceService.createInvoice(invoice);
                invoicesCreated++;

            } catch (Exception e) {
                logger.error("Fehler beim Erstellen einer Sample-Rechnung: {}", e.getMessage());
            }
        }

        logger.info("✓ {} Sample-Rechnungen erstellt: {} ACTIVE, {} DRAFT, {} SENT, {} CANCELLED",
                invoicesCreated, activeCount, draftCount, sentCount, cancelledCount);
        return invoicesCreated;
    }

    /**
     * PRIVATE METHODE: Sample-OpenItems erstellen
     */
    private int createSampleOpenItems(InitConfig config) {
        logger.info("Erstelle Sample-OpenItems...");

        List<Invoice> invoicesWithoutOpenItems = invoiceRepository.findInvoicesWithoutOpenItems();
        if (invoicesWithoutOpenItems.isEmpty()) {
            logger.warn("Keine Rechnungen für OpenItems gefunden");
            return 0;
        }

        int openItemsCreated = 0;
        int openCount = 0;
        int paidCount = 0;
        int partiallyPaidCount = 0;

        // Nur nicht-stornierte Rechnungen mit Betrag > 0
        invoicesWithoutOpenItems = invoicesWithoutOpenItems.stream()
                .filter(invoice -> invoice.getStatus() != Invoice.InvoiceStatus.CANCELLED)
                .filter(invoice -> invoice.getTotalAmount() != null && invoice.getTotalAmount().compareTo(BigDecimal.ZERO) > 0)
                .toList();

        for (Invoice invoice : invoicesWithoutOpenItems) {
            try {
                OpenItem openItem = new OpenItem(invoice,
                        "Sample-OpenItem für Rechnung " + invoice.getInvoiceNumber(),
                        invoice.getTotalAmount(),
                        invoice.getDueDate());

                // Status basierend auf Konfiguration setzen
                applyOpenItemConfig(openItem, config);

                // Zähle für Statistik
                switch (openItem.getStatus()) {
                    case OPEN, OVERDUE -> openCount++;
                    case PAID -> paidCount++;
                    case PARTIALLY_PAID -> partiallyPaidCount++;
                }

                openItemRepository.save(openItem);
                openItemsCreated++;

            } catch (Exception e) {
                logger.error("Fehler beim Erstellen eines OpenItems für Rechnung {}: {}",
                        invoice.getInvoiceNumber(), e.getMessage());
            }
        }

        logger.info("✓ {} Sample-OpenItems erstellt: {} OPEN/OVERDUE, {} PAID, {} PARTIALLY_PAID",
                openItemsCreated, openCount, paidCount, partiallyPaidCount);
        return openItemsCreated;
    }

    /**
     * HILFSMETHODE: OpenItem-Konfiguration anwenden
     */
    private void applyOpenItemConfig(OpenItem openItem, InitConfig config) {
        double randomValue = random.nextDouble();

        if (randomValue < config.getPaidOpenItemRatio()) {
            // Vollständig bezahlt
            openItem.recordPayment(openItem.getAmount(),
                    "Überweisung",
                    "REF-" + System.currentTimeMillis());
        } else if (randomValue < config.getPaidOpenItemRatio() + config.getPartiallyPaidOpenItemRatio()) {
            // Teilweise bezahlt
            BigDecimal partialAmount = openItem.getAmount()
                    .multiply(BigDecimal.valueOf(0.3 + random.nextDouble() * 0.5));
            openItem.recordPayment(partialAmount,
                    "Teilzahlung",
                    "REF-PARTIAL-" + System.currentTimeMillis());
        }
        // Sonst bleibt es OPEN (Standard)

        // Prüfe auf Überfälligkeit
        if (openItem.getDueDate().isBefore(LocalDate.now()) && openItem.getStatus() == OpenItem.OpenItemStatus.OPEN) {
            openItem.setStatus(OpenItem.OpenItemStatus.OVERDUE);
        }

        // Manchmal Mahnungen hinzufügen (bei überfälligen)
        if (openItem.getStatus() == OpenItem.OpenItemStatus.OVERDUE && random.nextDouble() < 0.5) {
            int reminders = 1 + random.nextInt(3);
            for (int r = 0; r < reminders; r++) {
                openItem.addReminder();
            }
        }
    }

    /**
     * PRIVATE METHODE: Automatischen Rechnungslauf durchführen
     */
    private void runAutomaticBillingProcess(LocalDate billingDate) {
        logger.info("===========================================");
        logger.info("Starte automatischen Rechnungslauf");
        logger.info("Stichtag: {}", billingDate);
        logger.info("===========================================");

        try {
            // Status VOR dem Rechnungslauf
            long activeDueSchedulesBefore = dueScheduleRepository.countByStatus(DueStatus.ACTIVE);
            long invoicesBefore = invoiceRepository.count();
            long openItemsBefore = openItemRepository.count();

            logger.info("Status VOR Rechnungslauf:");
            logger.info("  - Aktive Fälligkeiten: {}", activeDueSchedulesBefore);
            logger.info("  - Rechnungen: {}", invoicesBefore);
            logger.info("  - OpenItems: {}", openItemsBefore);

            // Rechnungslauf durchführen
            InvoiceBatchService.InvoiceBatchResult result = invoiceBatchService.runInvoiceBatch(billingDate);

            // Status NACH dem Rechnungslauf
            long activeDueSchedulesAfter = dueScheduleRepository.countByStatus(DueStatus.ACTIVE);
            long completedDueSchedulesAfter = dueScheduleRepository.countByStatus(DueStatus.COMPLETED);
            long invoicesAfter = invoiceRepository.count();
            long openItemsAfter = openItemRepository.count();

            logger.info("Status NACH Rechnungslauf:");
            logger.info("  - Aktive Fälligkeiten: {} (war: {})", activeDueSchedulesAfter, activeDueSchedulesBefore);
            logger.info("  - Abgerechnete Fälligkeiten: {}", completedDueSchedulesAfter);
            logger.info("  - Rechnungen: {} (war: {}, neu: {})", invoicesAfter, invoicesBefore, invoicesAfter - invoicesBefore);
            logger.info("  - OpenItems: {} (war: {}, neu: {})", openItemsAfter, openItemsBefore, openItemsAfter - openItemsBefore);

            // Validierung der Konsistenz
            long processedDueSchedules = activeDueSchedulesBefore - activeDueSchedulesAfter;
            long newInvoices = invoicesAfter - invoicesBefore;
            long newOpenItems = openItemsAfter - openItemsBefore;

            logger.info("Rechnungslauf-Ergebnis:");
            logger.info("  - Verarbeitete Fälligkeiten: {} (erwartet: {})", processedDueSchedules, result.getProcessedDueSchedules());
            logger.info("  - Erstellte Rechnungen: {} (erwartet: {})", newInvoices, result.getCreatedInvoices());
            logger.info("  - Neue OpenItems: {}", newOpenItems);
            logger.info("  - Gesamtbetrag: {} EUR", result.getTotalAmount());
            logger.info("  - Status: {}", result.getMessage());

            // Konsistenz-Prüfung
            boolean consistent = (processedDueSchedules == newInvoices) && (newInvoices == newOpenItems);

            if (consistent) {
                logger.info("✓ KONSISTENZ-PRÜFUNG ERFOLGREICH: Alle Zahlen stimmen überein!");
            } else {
                logger.error("✗ KONSISTENZ-FEHLER!");
                logger.error("  Verarbeitete Fälligkeiten: {} | Neue Rechnungen: {} | Neue OpenItems: {}",
                        processedDueSchedules, newInvoices, newOpenItems);

                logger.info("Versuche fehlende OpenItems automatisch zu erstellen...");
                createMissingOpenItems();
            }

        } catch (Exception e) {
            logger.error("KRITISCHER FEHLER beim Rechnungslauf: {}", e.getMessage(), e);
            throw new RuntimeException("Rechnungslauf fehlgeschlagen", e);
        }
    }

    /**
     * HILFSMETHODE: Fehlende OpenItems nachträglich erstellen
     */
    private void createMissingOpenItems() {
        try {
            List<Invoice> invoicesWithoutOpenItems = invoiceRepository.findAll().stream()
                    .filter(invoice -> invoice.getOpenItems().isEmpty())
                    .filter(invoice -> invoice.getTotalAmount() != null && invoice.getTotalAmount().compareTo(BigDecimal.ZERO) > 0)
                    .toList();

            int created = 0;
            for (Invoice invoice : invoicesWithoutOpenItems) {
                try {
                    OpenItem openItem = new OpenItem(invoice,
                            "Nachträglich erstellter offener Posten für Rechnung " + invoice.getInvoiceNumber(),
                            invoice.getTotalAmount(),
                            invoice.getDueDate());

                    openItemRepository.save(openItem);
                    created++;

                    logger.info("✓ OpenItem nachträglich erstellt für Rechnung {}", invoice.getInvoiceNumber());
                } catch (Exception e) {
                    logger.error("Fehler beim nachträglichen Erstellen des OpenItems für Rechnung {}: {}",
                            invoice.getInvoiceNumber(), e.getMessage());
                }
            }

            if (created > 0) {
                logger.info("✓ {} fehlende OpenItems nachträglich erstellt", created);
            } else {
                logger.info("Keine fehlenden OpenItems gefunden");
            }

        } catch (Exception e) {
            logger.error("Fehler beim nachträglichen Erstellen fehlender OpenItems: {}", e.getMessage());
        }
    }
}