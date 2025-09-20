package com.erp.backend.service.init;

import com.erp.backend.domain.*;
import com.erp.backend.repository.*;
import com.erp.backend.service.InvoiceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * REPORTER FÜR DEN AKTUELLEN DATENSTATUS
 *
 * Gibt nach Initialisierung eine detaillierte Übersicht ins Log.
 * Zeigt Status-Verteilungen und prüft Daten-Konsistenz.
 */
@Service
@Transactional(readOnly = true)
public class DataStatusReporter {

    private static final Logger logger = LoggerFactory.getLogger(DataStatusReporter.class);

    // Repository-Dependencies für Statusabfragen
    private final AddressRepository addressRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final ContractRepository contractRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final DueScheduleRepository dueScheduleRepository;
    private final InvoiceRepository invoiceRepository;
    private final OpenItemRepository openItemRepository;

    // Service-Dependencies
    private final InvoiceService invoiceService;

    /**
     * KONSTRUKTOR mit Dependency Injection
     */
    public DataStatusReporter(AddressRepository addressRepository,
                              CustomerRepository customerRepository,
                              ProductRepository productRepository,
                              ContractRepository contractRepository,
                              SubscriptionRepository subscriptionRepository,
                              DueScheduleRepository dueScheduleRepository,
                              InvoiceRepository invoiceRepository,
                              OpenItemRepository openItemRepository,
                              InvoiceService invoiceService) {
        this.addressRepository = addressRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.contractRepository = contractRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.dueScheduleRepository = dueScheduleRepository;
        this.invoiceRepository = invoiceRepository;
        this.openItemRepository = openItemRepository;
        this.invoiceService = invoiceService;
    }

    /**
     * HAUPTMETHODE: Aktuellen Datenstatus ins Log ausgeben
     */
    public void logCurrentDataStatus() {
        logger.info("===========================================");
        logger.info("AKTUELLER DATENBESTAND");
        logger.info("===========================================");

        logMasterDataStatus();
        logBusinessDataStatus();
        logBillingDataStatus();
        logConsistencyChecks();

        logger.info("===========================================");
    }

    /**
     * PRIVATE METHODE: Stammdaten-Status
     */
    private void logMasterDataStatus() {
        logger.info("📊 STAMMDATEN:");
        logger.info("  - Adressen: {}", addressRepository.count());
        logger.info("  - Kunden: {}", customerRepository.count());
        logger.info("  - Produkte: {}", productRepository.count());
    }

    /**
     * PRIVATE METHODE: Geschäftsdaten-Status
     */
    private void logBusinessDataStatus() {
        logger.info("📋 GESCHÄFTSDATEN:");

        // Verträge
        long totalContracts = contractRepository.count();
        if (totalContracts > 0) {
            long activeContracts = contractRepository.countByContractStatus(ContractStatus.ACTIVE);
            long terminatedContracts = contractRepository.countByContractStatus(ContractStatus.TERMINATED);

            logger.info("  - Verträge gesamt: {} ({}% ACTIVE, {}% TERMINATED)",
                    totalContracts,
                    calculatePercentage(activeContracts, totalContracts),
                    calculatePercentage(terminatedContracts, totalContracts));
        } else {
            logger.info("  - Verträge gesamt: 0");
        }

        // Abonnements
        long totalSubscriptions = subscriptionRepository.count();
        if (totalSubscriptions > 0) {
            long activeSubscriptions = subscriptionRepository.countBySubscriptionStatus(SubscriptionStatus.ACTIVE);
            long cancelledSubscriptions = subscriptionRepository.countBySubscriptionStatus(SubscriptionStatus.CANCELLED);
            long pausedSubscriptions = subscriptionRepository.countBySubscriptionStatus(SubscriptionStatus.PAUSED);

            logger.info("  - Abonnements gesamt: {} ({}% ACTIVE, {}% CANCELLED, {}% PAUSED)",
                    totalSubscriptions,
                    calculatePercentage(activeSubscriptions, totalSubscriptions),
                    calculatePercentage(cancelledSubscriptions, totalSubscriptions),
                    calculatePercentage(pausedSubscriptions, totalSubscriptions));
        } else {
            logger.info("  - Abonnements gesamt: 0");
        }
    }

    /**
     * PRIVATE METHODE: Abrechnungsdaten-Status
     */
    private void logBillingDataStatus() {
        logger.info("💰 ABRECHNUNGSDATEN:");

        // Fälligkeitspläne
        long totalSchedules = dueScheduleRepository.count();
        if (totalSchedules > 0) {
            long activeSchedules = dueScheduleRepository.countByStatus(DueStatus.ACTIVE);
            long completedSchedules = dueScheduleRepository.countByStatus(DueStatus.COMPLETED);
            long pausedSchedules = dueScheduleRepository.countByStatus(DueStatus.PAUSED);

            logger.info("  - Fälligkeitspläne: {} ({}% ACTIVE, {}% COMPLETED, {}% PAUSED)",
                    totalSchedules,
                    calculatePercentage(activeSchedules, totalSchedules),
                    calculatePercentage(completedSchedules, totalSchedules),
                    calculatePercentage(pausedSchedules, totalSchedules));

            // Zusätzliche Fälligkeits-Details
            long overdueDueSchedules = dueScheduleRepository.countOverdueDueSchedules(LocalDate.now());
            if (overdueDueSchedules > 0) {
                logger.info("    └─ Davon überfällig: {}", overdueDueSchedules);
            }
        } else {
            logger.info("  - Fälligkeitspläne: 0");
        }

        // Rechnungen
        long totalInvoices = invoiceRepository.count();
        if (totalInvoices > 0) {
            long activeInvoices = invoiceService.getInvoiceCountByStatus(Invoice.InvoiceStatus.ACTIVE);
            long draftInvoices = invoiceService.getInvoiceCountByStatus(Invoice.InvoiceStatus.DRAFT);
            long sentInvoices = invoiceService.getInvoiceCountByStatus(Invoice.InvoiceStatus.SENT);
            long cancelledInvoices = invoiceService.getInvoiceCountByStatus(Invoice.InvoiceStatus.CANCELLED);

            logger.info("  - Rechnungen gesamt: {} ({}% ACTIVE, {}% DRAFT, {}% SENT, {}% CANCELLED)",
                    totalInvoices,
                    calculatePercentage(activeInvoices, totalInvoices),
                    calculatePercentage(draftInvoices, totalInvoices),
                    calculatePercentage(sentInvoices, totalInvoices),
                    calculatePercentage(cancelledInvoices, totalInvoices));

            // Rechnungsbeträge
            BigDecimal totalInvoiceAmount = invoiceRepository.sumTotalAmountByStatus(Invoice.InvoiceStatus.ACTIVE);
            if (totalInvoiceAmount != null && totalInvoiceAmount.compareTo(BigDecimal.ZERO) > 0) {
                logger.info("    └─ Gesamtwert aktiver Rechnungen: {} EUR", totalInvoiceAmount);
            }
        } else {
            logger.info("  - Rechnungen gesamt: 0");
        }

        // Offene Posten
        long totalOpenItems = openItemRepository.count();
        if (totalOpenItems > 0) {
            long openOpenItems = openItemRepository.countByStatus(OpenItem.OpenItemStatus.OPEN);
            long paidOpenItems = openItemRepository.countByStatus(OpenItem.OpenItemStatus.PAID);
            long partiallyPaidOpenItems = openItemRepository.countByStatus(OpenItem.OpenItemStatus.PARTIALLY_PAID);
            long overdueOpenItems = openItemRepository.countByStatus(OpenItem.OpenItemStatus.OVERDUE);
            long cancelledOpenItems = openItemRepository.countByStatus(OpenItem.OpenItemStatus.CANCELLED);

            logger.info("  - Offene Posten: {} ({}% OPEN, {}% PAID, {}% PARTIALLY_PAID, {}% OVERDUE, {}% CANCELLED)",
                    totalOpenItems,
                    calculatePercentage(openOpenItems, totalOpenItems),
                    calculatePercentage(paidOpenItems, totalOpenItems),
                    calculatePercentage(partiallyPaidOpenItems, totalOpenItems),
                    calculatePercentage(overdueOpenItems, totalOpenItems),
                    calculatePercentage(cancelledOpenItems, totalOpenItems));

            // Offene Beträge
            BigDecimal totalOpenAmount = openItemRepository.sumAmountByStatus(OpenItem.OpenItemStatus.OPEN);
            BigDecimal totalOverdueAmount = openItemRepository.sumAmountByStatus(OpenItem.OpenItemStatus.OVERDUE);

            if (totalOpenAmount != null && totalOpenAmount.compareTo(BigDecimal.ZERO) > 0) {
                logger.info("    └─ Offener Betrag: {} EUR", totalOpenAmount);
            }
            if (totalOverdueAmount != null && totalOverdueAmount.compareTo(BigDecimal.ZERO) > 0) {
                logger.info("    └─ Überfälliger Betrag: {} EUR", totalOverdueAmount);
            }
        } else {
            logger.info("  - Offene Posten: 0");
        }
    }

    /**
     * PRIVATE METHODE: Konsistenz-Prüfungen
     */
    private void logConsistencyChecks() {
        logger.info("🔍 KONSISTENZ-PRÜFUNG:");

        boolean allConsistent = true;

        // 1. Rechnungen ohne OpenItems
        long invoicesWithoutOpenItems = countInvoicesWithoutOpenItems();
        if (invoicesWithoutOpenItems > 0) {
            logger.warn("  ⚠ Rechnungen ohne OpenItems: {}", invoicesWithoutOpenItems);
            allConsistent = false;
        } else {
            logger.info("  ✓ Alle Rechnungen haben OpenItems");
        }

        // 2. OpenItems ohne Rechnungen
        long openItemsWithoutInvoices = openItemRepository.findAll().stream()
                .filter(openItem -> openItem.getInvoice() == null)
                .count();
        if (openItemsWithoutInvoices > 0) {
            logger.warn("  ⚠ OpenItems ohne Rechnungen: {}", openItemsWithoutInvoices);
            allConsistent = false;
        } else {
            logger.info("  ✓ Alle OpenItems sind mit Rechnungen verknüpft");
        }

        // 3. Verträge und Abonnements
        long contractsWithoutSubscriptions = contractRepository.countActiveContractsWithoutActiveSubscriptions();
        if (contractsWithoutSubscriptions > 0) {
            logger.info("  ℹ Aktive Verträge ohne aktive Abonnements: {}", contractsWithoutSubscriptions);
        }

        // 4. Abonnements und DueSchedules
        long subscriptionsWithoutSchedules = subscriptionRepository.countActiveSubscriptionsWithoutDueSchedules();
        if (subscriptionsWithoutSchedules > 0) {
            logger.warn("  ⚠ Aktive Abonnements ohne Fälligkeitspläne: {}", subscriptionsWithoutSchedules);
            allConsistent = false;
        } else {
            logger.info("  ✓ Alle aktiven Abonnements haben Fälligkeitspläne");
        }

        // Gesamtfazit
        if (allConsistent) {
            logger.info("  ✅ KONSISTENZ-PRÜFUNG ERFOLGREICH: Alle Daten sind konsistent!");
        } else {
            logger.warn("  ⚠ KONSISTENZ-PROBLEME GEFUNDEN: Siehe Details oben");
        }
    }

    /**
     * HILFSMETHODE: Rechnungen ohne OpenItems zählen
     */
    private long countInvoicesWithoutOpenItems() {
        try {
            List<Invoice> invoicesWithoutOpenItems = invoiceRepository.findInvoicesWithoutOpenItems();
            return invoicesWithoutOpenItems.stream()
                    .filter(invoice -> invoice.getStatus() != Invoice.InvoiceStatus.CANCELLED)
                    .filter(invoice -> invoice.getTotalAmount() != null && invoice.getTotalAmount().compareTo(BigDecimal.ZERO) > 0)
                    .count();
        } catch (Exception e) {
            logger.error("Fehler beim Zählen der Rechnungen ohne OpenItems: {}", e.getMessage());
            return -1;
        }
    }

    /**
     * HILFSMETHODE: Prozentsatz berechnen
     */
    private int calculatePercentage(long part, long total) {
        if (total == 0) return 0;
        return (int) Math.round((double) part * 100.0 / (double) total);
    }

    /**
     * ZUSÄTZLICHE METHODE: Detailstatus für spezifische Entität
     */
    public void logDetailedStatusFor(Class<?> entityClass) {
        logger.info("=== Detailstatus für {} ===", entityClass.getSimpleName());

        if (entityClass == Contract.class) {
            logContractDetails();
        } else if (entityClass == Subscription.class) {
            logSubscriptionDetails();
        } else if (entityClass == Invoice.class) {
            logInvoiceDetails();
        } else if (entityClass == OpenItem.class) {
            logOpenItemDetails();
        } else {
            logger.warn("Detailstatus für {} nicht implementiert", entityClass.getSimpleName());
        }
    }

    private void logContractDetails() {
        List<Contract> recentContracts = contractRepository.findTop5ByOrderByCreatedAtDesc();
        logger.info("Letzte 5 Verträge:");
        recentContracts.forEach(contract ->
                logger.info("  - {} ({}): {} - {}",
                        contract.getContractNumber(),
                        contract.getContractStatus(),
                        contract.getCustomer().getFirstName() + " " + contract.getCustomer().getLastName(),
                        contract.getStartDate())
        );
    }

    private void logSubscriptionDetails() {
        List<Subscription> recentSubscriptions = subscriptionRepository.findTop5ByOrderByCreatedAtDesc();
        logger.info("Letzte 5 Abonnements:");
        recentSubscriptions.forEach(subscription ->
                logger.info("  - {} ({}): {} - {} EUR",
                        subscription.getSubscriptionNumber(),
                        subscription.getSubscriptionStatus(),
                        subscription.getProductName(),
                        subscription.getMonthlyPrice())
        );
    }

    private void logInvoiceDetails() {
        List<Invoice> recentInvoices = invoiceRepository.findTop5ByOrderByInvoiceDateDesc();
        logger.info("Letzte 5 Rechnungen:");
        recentInvoices.forEach(invoice ->
                logger.info("  - {} ({}): {} - {} EUR",
                        invoice.getInvoiceNumber(),
                        invoice.getStatus(),
                        invoice.getCustomer().getFirstName() + " " + invoice.getCustomer().getLastName(),
                        invoice.getTotalAmount())
        );
    }

    private void logOpenItemDetails() {
        List<OpenItem> overdueItems = openItemRepository.findByStatusAndDueDateBefore(
                OpenItem.OpenItemStatus.OVERDUE, LocalDate.now());
        logger.info("Überfällige OpenItems ({} Stück):", overdueItems.size());
        overdueItems.stream().limit(5).forEach(item ->
                logger.info("  - {}: {} EUR (Fällig: {})",
                        item.getDescription(),
                        item.getAmount(),
                        item.getDueDate())
        );
    }
}