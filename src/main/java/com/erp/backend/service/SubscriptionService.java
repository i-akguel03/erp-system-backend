package com.erp.backend.service;

import com.erp.backend.domain.*;
import com.erp.backend.dto.DueScheduleDto;
import com.erp.backend.repository.ContractRepository;
import com.erp.backend.repository.CustomerRepository;
import com.erp.backend.repository.ProductRepository;
import com.erp.backend.repository.SubscriptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Transactional
public class SubscriptionService {

    private static final Logger logger = LoggerFactory.getLogger(SubscriptionService.class);

    private final SubscriptionRepository subscriptionRepository;
    private final ContractRepository contractRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final Random random = new Random();

    @Autowired
    private DueScheduleService dueScheduleService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    public SubscriptionService(SubscriptionRepository subscriptionRepository,
                               ContractRepository contractRepository,
                               CustomerRepository customerRepository,
                               ProductRepository productRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.contractRepository = contractRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
    }

    public void initTestSubscriptions() {
        if (subscriptionRepository.count() > 0) return; // nur einmal

        List<Contract> contracts = contractRepository.findAll();
        List<Product> products = productRepository.findAll();

        if (contracts.isEmpty() || products.isEmpty()) return;

        for (int i = 1; i <= 30; i++) {
            Contract contract = contracts.get(random.nextInt(contracts.size()));
            Product product = products.get(random.nextInt(products.size()));

            LocalDate startDate = LocalDate.now().minusDays(random.nextInt(60));
            LocalDate endDate = startDate.plusMonths(random.nextInt(12) + 1);

            Subscription subscription = new Subscription();
            subscription.setSubscriptionNumber("SUB-" + String.format("%04d", i));
            subscription.setProductName(product.getName());
            subscription.setMonthlyPrice(product.getPrice());
            subscription.setStartDate(startDate);
            subscription.setEndDate(endDate);
            subscription.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
            subscription.setBillingCycle(BillingCycle.MONTHLY);
            subscription.setContract(contract);

            subscriptionRepository.save(subscription);

            // Contract-Relation pflegen
            contract.addSubscription(subscription);

            // Automatische Erstellung von Fälligkeitsplänen für Test-Daten
            try {
                int monthsToGenerate = (int) startDate.until(endDate).toTotalMonths();
                if (monthsToGenerate > 0) {
                    dueScheduleService.generateDueSchedulesForSubscription(subscription.getId(), monthsToGenerate);
                }
            } catch (Exception e) {
                logger.warn("Fehler beim Erstellen der Fälligkeitspläne für Test-Subscription {}: {}",
                        subscription.getId(), e.getMessage());
            }
        }
    }

    @Transactional(readOnly = true)
    public List<Subscription> getAllSubscriptions() {
        List<Subscription> subscriptions = subscriptionRepository.findAll();
        logger.info("Fetched {} subscriptions", subscriptions.size());
        return subscriptions;
    }

    @Transactional(readOnly = true)
    public Page<Subscription> getAllSubscriptions(Pageable pageable) {
        Page<Subscription> subscriptions = subscriptionRepository.findAll(pageable);
        logger.info("Fetched {} subscriptions (page {}/{})",
                subscriptions.getNumberOfElements(), subscriptions.getNumber() + 1, subscriptions.getTotalPages());
        return subscriptions;
    }

    @Transactional(readOnly = true)
    public Optional<Subscription> getSubscriptionById(UUID id) {
        Optional<Subscription> subscription = subscriptionRepository.findById(id);
        if (subscription.isPresent()) {
            logger.info("Found subscription with id={}", id);
        } else {
            logger.warn("No subscription found with id={}", id);
        }
        return subscription;
    }

    @Transactional(readOnly = true)
    public Optional<Subscription> getSubscriptionByNumber(String subscriptionNumber) {
        Optional<Subscription> subscription = subscriptionRepository.findBySubscriptionNumber(subscriptionNumber);
        logger.info("Search for subscription with number={}: {}", subscriptionNumber, subscription.isPresent() ? "found" : "not found");
        return subscription;
    }

    @Transactional(readOnly = true)
    public List<Subscription> getSubscriptionsByContract(UUID contractId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("Contract not found with ID: " + contractId));
        List<Subscription> subscriptions = subscriptionRepository.findByContract(contract);
        logger.info("Found {} subscriptions for contract {}", subscriptions.size(), contractId);
        return subscriptions;
    }

    @Transactional(readOnly = true)
    public List<Subscription> getActiveSubscriptionsByContract(UUID contractId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("Contract not found with ID: " + contractId));
        List<Subscription> subscriptions = subscriptionRepository.findByContractAndSubscriptionStatus(contract, SubscriptionStatus.ACTIVE);
        logger.info("Found {} active subscriptions for contract {}", subscriptions.size(), contractId);
        return subscriptions;
    }

    @Transactional(readOnly = true)
    public List<Subscription> getSubscriptionsByCustomer(UUID customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with ID: " + customerId));
        List<Subscription> subscriptions = subscriptionRepository.findByCustomer(customer);
        logger.info("Found {} subscriptions for customer {}", subscriptions.size(), customerId);
        return subscriptions;
    }

    @Transactional(readOnly = true)
    public List<Subscription> getActiveSubscriptionsByCustomer(UUID customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with ID: " + customerId));
        List<Subscription> subscriptions = subscriptionRepository.findActiveSubscriptionsByCustomer(customer);
        logger.info("Found {} active subscriptions for customer {}", subscriptions.size(), customerId);
        return subscriptions;
    }

    @Transactional(readOnly = true)
    public List<Subscription> getSubscriptionsByStatus(SubscriptionStatus status) {
        List<Subscription> subscriptions = subscriptionRepository.findBySubscriptionStatus(status);
        logger.info("Found {} subscriptions with status {}", subscriptions.size(), status);
        return subscriptions;
    }

    @Transactional(readOnly = true)
    public List<Subscription> getSubscriptionsExpiringInDays(int days) {
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(days);
        List<Subscription> subscriptions = subscriptionRepository.findSubscriptionsExpiringBetween(startDate, endDate);
        logger.info("Found {} subscriptions expiring in next {} days", subscriptions.size(), days);
        return subscriptions;
    }

    @Transactional(readOnly = true)
    public List<Subscription> getSubscriptionsForAutoRenewal(int days) {
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(days);
        List<Subscription> subscriptions = subscriptionRepository.findSubscriptionsForAutoRenewal(startDate, endDate);
        logger.info("Found {} subscriptions for auto renewal in next {} days", subscriptions.size(), days);
        return subscriptions;
    }

    @Transactional(readOnly = true)
    public List<Subscription> searchSubscriptionsByProduct(String productName) {
        List<Subscription> subscriptions = subscriptionRepository.findByProductNameContainingIgnoreCase(productName);
        logger.info("Found {} subscriptions matching product search: '{}'", subscriptions.size(), productName);
        return subscriptions;
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalActiveRevenue() {
        BigDecimal revenue = subscriptionRepository.calculateTotalActiveRevenue();
        logger.info("Total active subscription revenue: {}", revenue);
        return revenue != null ? revenue : BigDecimal.ZERO;
    }

    @Transactional(readOnly = true)
    public BigDecimal getActiveRevenueByCustomer(UUID customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with ID: " + customerId));
        BigDecimal revenue = subscriptionRepository.calculateActiveRevenueByCustomer(customer);
        logger.info("Active subscription revenue for customer {}: {}", customerId, revenue);
        return revenue != null ? revenue : BigDecimal.ZERO;
    }

    /**
     * Erstellt ein neues Abonnement mit automatischer Generierung von Fälligkeitsplänen
     */
    public Subscription createSubscription(Subscription subscription) {
        validateSubscriptionForCreation(subscription);

        // Keine ID setzen, wird von DB generiert
        subscription.setId(null);

        // Abo-Nummer generieren
        subscription.setSubscriptionNumber(generateSubscriptionNumber());

        // Status setzen falls nicht vorhanden
        if (subscription.getSubscriptionStatus() == null) {
            subscription.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
        }

        // Billing Cycle setzen falls nicht vorhanden
        if (subscription.getBillingCycle() == null) {
            subscription.setBillingCycle(BillingCycle.MONTHLY);
        }

        // Auto-Renewal Standard-Wert
        if (subscription.getAutoRenewal() == null) {
            subscription.setAutoRenewal(true);
        }

        // End-Date setzen falls nicht vorhanden (Standard: 12 Monate)
        if (subscription.getEndDate() == null) {
            subscription.setEndDate(subscription.getStartDate().plusYears(1));
        }

        Subscription saved = subscriptionRepository.save(subscription);
        logger.info("Created new subscription: id={}, subscriptionNumber={}, product={}, contract={}",
                saved.getId(), saved.getSubscriptionNumber(), saved.getProductName(), saved.getContract().getId());

        // Automatische Erstellung von Fälligkeitsplänen
        try {
            int monthsToGenerate = calculateMonthsForDueScheduleGeneration(saved);
            if (monthsToGenerate > 0) {
                dueScheduleService.generateDueSchedulesForSubscription(saved.getId(), monthsToGenerate);
                logger.info("Generated {} months of due schedules for subscription {}", monthsToGenerate, saved.getId());
            }
        } catch (Exception e) {
            logger.error("Fehler beim Erstellen der Fälligkeitspläne für Subscription {}: {}", saved.getId(), e.getMessage());
            // Subscription trotzdem zurückgeben, Fälligkeitspläne können später manuell erstellt werden
        }

        return saved;
    }

    /**
     * Aktualisiert ein Abonnement und synchronisiert Fälligkeitspläne bei Bedarf
     */
    public Subscription updateSubscription(Subscription subscription) {
        if (subscription.getId() == null) {
            throw new IllegalArgumentException("Subscription ID cannot be null for update");
        }

        Subscription existing = subscriptionRepository.findById(subscription.getId())
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found with ID: " + subscription.getId()));

        // Prüfung auf relevante Änderungen für Fälligkeitspläne
        boolean priceChanged = !existing.getMonthlyPrice().equals(subscription.getMonthlyPrice());
        boolean billingCycleChanged = existing.getBillingCycle() != subscription.getBillingCycle();
        boolean endDateChanged = !existing.getEndDate().equals(subscription.getEndDate());

        Subscription saved = subscriptionRepository.save(subscription);
        logger.info("Updated subscription: id={}, subscriptionNumber={}", saved.getId(), saved.getSubscriptionNumber());

        // Synchronisierung der Fälligkeitspläne bei relevanten Änderungen
        if (priceChanged || billingCycleChanged || endDateChanged) {
            try {
                synchronizeDueSchedules(saved, existing, priceChanged, billingCycleChanged, endDateChanged);
            } catch (Exception e) {
                logger.error("Fehler beim Synchronisieren der Fälligkeitspläne für Subscription {}: {}", saved.getId(), e.getMessage());
            }
        }

        return saved;
    }

    public Subscription activateSubscription(UUID subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found with ID: " + subscriptionId));

        subscription.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
        if (subscription.getStartDate() == null) {
            subscription.setStartDate(LocalDate.now());
        }

        Subscription saved = subscriptionRepository.save(subscription);
        logger.info("Activated subscription: id={}, subscriptionNumber={}", saved.getId(), saved.getSubscriptionNumber());

        // Bei Aktivierung fehlende Fälligkeitspläne erstellen
        try {
            ensureDueSchedulesExist(saved);
        } catch (Exception e) {
            logger.error("Fehler beim Erstellen fehlender Fälligkeitspläne für aktivierte Subscription {}: {}",
                    saved.getId(), e.getMessage());
        }

        return saved;
    }

    /**
     * Kündigt ein Abonnement und behandelt offene Fälligkeitspläne
     */
    public Subscription cancelSubscription(UUID subscriptionId, LocalDate cancellationDate) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found with ID: " + subscriptionId));

        LocalDate effectiveCancellationDate = cancellationDate != null ? cancellationDate : LocalDate.now();

        subscription.setSubscriptionStatus(SubscriptionStatus.CANCELLED);
        subscription.setEndDate(effectiveCancellationDate);
        subscription.setAutoRenewal(false);

        Subscription saved = subscriptionRepository.save(subscription);
        logger.info("Cancelled subscription: id={}, subscriptionNumber={}, endDate={}",
                saved.getId(), saved.getSubscriptionNumber(), saved.getEndDate());

        // Behandlung der Fälligkeitspläne nach Kündigung
        try {
            handleDueSchedulesOnCancellation(saved, effectiveCancellationDate);
        } catch (Exception e) {
            logger.error("Fehler beim Behandeln der Fälligkeitspläne nach Kündigung für Subscription {}: {}",
                    saved.getId(), e.getMessage());
        }

        return saved;
    }

    public Subscription pauseSubscription(UUID subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found with ID: " + subscriptionId));

        subscription.setSubscriptionStatus(SubscriptionStatus.PAUSED);

        Subscription saved = subscriptionRepository.save(subscription);
        logger.info("Paused subscription: id={}, subscriptionNumber={}", saved.getId(), saved.getSubscriptionNumber());
        return saved;
    }

    /**
     * Verlängert ein Abonnement und erstellt neue Fälligkeitspläne
     */
    public Subscription renewSubscription(UUID subscriptionId, LocalDate newEndDate) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found with ID: " + subscriptionId));

        LocalDate oldEndDate = subscription.getEndDate();
        subscription.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
        subscription.setEndDate(newEndDate);

        Subscription saved = subscriptionRepository.save(subscription);
        logger.info("Renewed subscription: id={}, subscriptionNumber={}, newEndDate={}",
                saved.getId(), saved.getSubscriptionNumber(), saved.getEndDate());

        // Neue Fälligkeitspläne für Verlängerung erstellen
        try {
            int additionalMonths = (int) oldEndDate.until(newEndDate).toTotalMonths();
            if (additionalMonths > 0) {
                dueScheduleService.generateDueSchedulesForSubscription(saved.getId(), additionalMonths);
                logger.info("Generated {} additional due schedules for renewed subscription {}",
                        additionalMonths, saved.getId());
            }
        } catch (Exception e) {
            logger.error("Fehler beim Erstellen zusätzlicher Fälligkeitspläne für verlängerte Subscription {}: {}",
                    saved.getId(), e.getMessage());
        }

        return saved;
    }

    @Transactional
    public void deleteSubscription(UUID id) {
        Subscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found with ID: " + id));

        // Prüfen, ob der übergeordnete Vertrag aktiv ist
        Contract contract = subscription.getContract();
        if (contract != null && contract.getContractStatus().equals(ContractStatus.ACTIVE)) { // oder contract.getContractStatus() != TERMINATED
            throw new IllegalStateException(
                    "Cannot delete subscription because the parent contract is active (subscriptionId=" + id + ")"
            );
        }

        // Fälligkeitspläne löschen, falls nicht bezahlt
        try {
            List<DueScheduleDto> dueSchedules = dueScheduleService.getDueSchedulesBySubscription(id);
            for (DueScheduleDto schedule : dueSchedules) {
                if (schedule.getStatus() != DueStatus.PAID) {
                    dueScheduleService.deleteDueSchedule(schedule.getId()); // hier Soft Delete möglich, falls implementiert
                }
            }
        } catch (Exception e) {
            logger.warn("Fehler beim Löschen der Fälligkeitspläne für Subscription {}: {}", id, e.getMessage());
        }

        // Soft Delete auf Subscription (vorausgesetzt @SQLDelete in Subscription Entity)
        subscriptionRepository.delete(subscription);

        logger.info("Soft-deleted subscription with id={}", id);
    }


    @Transactional(readOnly = true)
    public long getTotalSubscriptionCount() {
        long count = subscriptionRepository.count();
        logger.info("Total subscription count: {}", count);
        return count;
    }

    @Transactional(readOnly = true)
    public Long getSubscriptionCountByStatus(SubscriptionStatus status) {
        Long count = subscriptionRepository.countBySubscriptionStatus(status);
        logger.info("Subscription count for status {}: {}", status, count);
        return count;
    }

    @Transactional(readOnly = true)
    public List<Object[]> getTopProductsByActiveSubscriptions() {
        List<Object[]> topProducts = subscriptionRepository.findTopProductsByActiveSubscriptions();
        logger.info("Retrieved top products by active subscriptions count");
        return topProducts;
    }

    @Transactional(readOnly = true)
    public List<Subscription> getTopSubscriptionsByPrice(int limit) {
        Pageable pageable = Pageable.ofSize(limit);
        List<Subscription> topSubscriptions = subscriptionRepository.findTopSubscriptionsByPrice(pageable);
        logger.info("Retrieved top {} subscriptions by price", limit);
        return topSubscriptions;
    }

    /**
     * Verarbeitet automatische Verlängerungen und erstellt neue Fälligkeitspläne
     */
    public List<Subscription> processAutoRenewals() {
        LocalDate today = LocalDate.now();
        LocalDate nextWeek = today.plusDays(7);

        List<Subscription> subscriptionsToRenew = subscriptionRepository.findSubscriptionsForAutoRenewal(today, nextWeek);

        for (Subscription subscription : subscriptionsToRenew) {
            LocalDate oldEndDate = subscription.getEndDate();
            LocalDate newEndDate = calculateNewEndDate(subscription);

            subscription.setEndDate(newEndDate);
            subscription.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
            subscriptionRepository.save(subscription);

            // Neue Fälligkeitspläne für Verlängerung erstellen
            try {
                int additionalMonths = (int) oldEndDate.until(newEndDate).toTotalMonths();
                if (additionalMonths > 0) {
                    dueScheduleService.generateDueSchedulesForSubscription(subscription.getId(), additionalMonths);
                }
            } catch (Exception e) {
                logger.error("Fehler beim Erstellen der Fälligkeitspläne für auto-verlängerte Subscription {}: {}",
                        subscription.getId(), e.getMessage());
            }

            logger.info("Auto-renewed subscription: id={}, subscriptionNumber={}, newEndDate={}",
                    subscription.getId(), subscription.getSubscriptionNumber(), newEndDate);
        }

        logger.info("Processed {} auto-renewals", subscriptionsToRenew.size());
        return subscriptionsToRenew;
    }

    public List<Subscription> processExpiredSubscriptions() {
        LocalDate today = LocalDate.now();
        List<Subscription> expiredSubscriptions = subscriptionRepository.findExpiredSubscriptionsWithoutAutoRenewal(today);

        for (Subscription subscription : expiredSubscriptions) {
            subscription.setSubscriptionStatus(SubscriptionStatus.EXPIRED);
            subscriptionRepository.save(subscription);

            // Offene Fälligkeitspläne nach Ablauf behandeln
            try {
                handleDueSchedulesOnExpiration(subscription);
            } catch (Exception e) {
                logger.error("Fehler beim Behandeln der Fälligkeitspläne für abgelaufene Subscription {}: {}",
                        subscription.getId(), e.getMessage());
            }

            logger.info("Expired subscription: id={}, subscriptionNumber={}",
                    subscription.getId(), subscription.getSubscriptionNumber());
        }

        logger.info("Processed {} expired subscriptions", expiredSubscriptions.size());
        return expiredSubscriptions;
    }

    // ========================================================================
    // Private Hilfsmethoden
    // ========================================================================

    private void validateSubscriptionForCreation(Subscription subscription) {
        if (subscription.getProductName() == null || subscription.getProductName().trim().isEmpty()) {
            throw new IllegalArgumentException("Product name is required");
        }
        if (subscription.getMonthlyPrice() == null || subscription.getMonthlyPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Valid monthly price is required");
        }
        if (subscription.getStartDate() == null) {
            throw new IllegalArgumentException("Start date is required");
        }
        if (subscription.getContract() == null || subscription.getContract().getId() == null) {
            throw new IllegalArgumentException("Contract is required");
        }

        // Contract muss existieren
        if (!contractRepository.existsById(subscription.getContract().getId())) {
            throw new IllegalArgumentException("Contract not found with ID: " + subscription.getContract().getId());
        }
    }

    private String generateSubscriptionNumber() {
        String prefix = "SUB";
        String year = String.valueOf(LocalDate.now().getYear());
        String randomPart;
        String subscriptionNumber;

        do {
            int number = (int) (Math.random() * 999999) + 1;
            randomPart = String.format("%06d", number);
            subscriptionNumber = prefix + year + randomPart;
        } while (subscriptionRepository.findBySubscriptionNumber(subscriptionNumber).isPresent());

        return subscriptionNumber;
    }

    private LocalDate calculateNewEndDate(Subscription subscription) {
        LocalDate currentEndDate = subscription.getEndDate();
        if (currentEndDate == null) {
            currentEndDate = LocalDate.now();
        }

        return switch (subscription.getBillingCycle()) {
            case MONTHLY -> currentEndDate.plusMonths(1);
            case QUARTERLY -> currentEndDate.plusMonths(3);
            case SEMI_ANNUALLY -> currentEndDate.plusMonths(6);
            case ANNUALLY -> currentEndDate.plusYears(1);
        };
    }

    /**
     * Berechnet die Anzahl der Monate für die initiale Fälligkeitsplan-Generierung
     */
    private int calculateMonthsForDueScheduleGeneration(Subscription subscription) {
        if (subscription.getEndDate() == null) {
            // Standard: 12 Monate wenn kein Enddatum gesetzt
            return 12;
        }

        LocalDate startDate = subscription.getStartDate();
        LocalDate endDate = subscription.getEndDate();

        long totalMonths = startDate.until(endDate).toTotalMonths();
        return Math.max(1, (int) totalMonths);
    }

    /**
     * Stellt sicher, dass Fälligkeitspläne für ein Abonnement existieren
     */
    private void ensureDueSchedulesExist(Subscription subscription) {
        try {
            List<DueScheduleDto> existing = dueScheduleService.getDueSchedulesBySubscription(subscription.getId());
            if (existing.isEmpty()) {
                int monthsToGenerate = calculateMonthsForDueScheduleGeneration(subscription);
                dueScheduleService.generateDueSchedulesForSubscription(subscription.getId(), monthsToGenerate);
                logger.info("Created missing due schedules for subscription {}", subscription.getId());
            }
        } catch (Exception e) {
            logger.error("Fehler beim Prüfen/Erstellen der Fälligkeitspläne für Subscription {}: {}",
                    subscription.getId(), e.getMessage());
        }
    }

    /**
     * Synchronisiert Fälligkeitspläne nach Subscription-Änderungen
     */
    private void synchronizeDueSchedules(Subscription updated, Subscription original,
                                         boolean priceChanged, boolean billingCycleChanged, boolean endDateChanged) {

        List<DueScheduleDto> existingSchedules = dueScheduleService.getDueSchedulesBySubscription(updated.getId());

        for (DueScheduleDto schedule : existingSchedules) {
            // Nur zukünftige, noch nicht bezahlte Pläne anpassen
            if (schedule.getDueDate().isAfter(LocalDate.now()) &&
                    schedule.getStatus() == DueStatus.PENDING) {

                boolean scheduleUpdated = false;

                // Preis aktualisieren wenn geändert
                if (priceChanged) {
                    schedule.setAmount(updated.getMonthlyPrice());
                    scheduleUpdated = true;
                }

                if (scheduleUpdated) {
                    try {
                        dueScheduleService.updateDueSchedule(schedule.getId(), schedule);
                    } catch (Exception e) {
                        logger.error("Fehler beim Aktualisieren des Fälligkeitsplans {}: {}",
                                schedule.getId(), e.getMessage());
                    }
                }
            }
        }

        // Bei Enddatum-Änderung: zusätzliche oder zu löschende Pläne behandeln
        if (endDateChanged) {
            handleEndDateChange(updated, original);
        }
    }

    /**
     * Behandelt Änderungen am Enddatum
     */
    private void handleEndDateChange(Subscription updated, Subscription original) {
        if (updated.getEndDate().isAfter(original.getEndDate())) {
            // Verlängerung: zusätzliche Fälligkeitspläne erstellen
            try {
                int additionalMonths = (int) original.getEndDate().until(updated.getEndDate()).toTotalMonths();
                if (additionalMonths > 0) {
                    dueScheduleService.generateDueSchedulesForSubscription(updated.getId(), additionalMonths);
                }
            } catch (Exception e) {
                logger.error("Fehler beim Erstellen zusätzlicher Fälligkeitspläne: {}", e.getMessage());
            }
        } else if (updated.getEndDate().isBefore(original.getEndDate())) {
            // Verkürzung: zukünftige Fälligkeitspläne nach neuem Enddatum löschen/stornieren
            try {
                cancelDueSchedulesAfterDate(updated.getId(), updated.getEndDate());
            } catch (Exception e) {
                logger.error("Fehler beim Stornieren überschüssiger Fälligkeitspläne: {}", e.getMessage());
            }
        }
    }

    /**
     * Behandelt Fälligkeitspläne bei Kündigung
     */
    private void handleDueSchedulesOnCancellation(Subscription subscription, LocalDate cancellationDate) {
        List<DueScheduleDto> schedules = dueScheduleService.getDueSchedulesBySubscription(subscription.getId());

        for (DueScheduleDto schedule : schedules) {
            // Zukünftige, unbezahlte Pläne stornieren
            if (schedule.getDueDate().isAfter(cancellationDate) &&
                    schedule.getStatus() == DueStatus.PENDING) {
                try {
                    dueScheduleService.cancelDueSchedule(schedule.getId());
                } catch (Exception e) {
                    logger.error("Fehler beim Stornieren des Fälligkeitsplans {}: {}",
                            schedule.getId(), e.getMessage());
                }
            }
        }
    }

    /**
     * Behandelt Fälligkeitspläne bei Ablauf ohne Verlängerung
     */
    private void handleDueSchedulesOnExpiration(Subscription subscription) {
        // Ähnliche Logik wie bei Kündigung
        handleDueSchedulesOnCancellation(subscription, subscription.getEndDate());
    }

    /**
     * Storniert alle Fälligkeitspläne nach einem bestimmten Datum
     */
    private void cancelDueSchedulesAfterDate(UUID subscriptionId, LocalDate cutoffDate) {
        List<DueScheduleDto> schedules = dueScheduleService.getDueSchedulesBySubscription(subscriptionId);

        for (DueScheduleDto schedule : schedules) {
            if (schedule.getDueDate().isAfter(cutoffDate) &&
                    schedule.getStatus() == DueStatus.PENDING) {
                try {
                    dueScheduleService.cancelDueSchedule(schedule.getId());
                } catch (Exception e) {
                    logger.error("Fehler beim Stornieren des Fälligkeitsplans {}: {}",
                            schedule.getId(), e.getMessage());
                }
            }
        }
    }
}