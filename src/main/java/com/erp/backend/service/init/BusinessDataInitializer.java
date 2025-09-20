package com.erp.backend.service.init;

import com.erp.backend.domain.*;
import com.erp.backend.repository.ContractRepository;
import com.erp.backend.repository.CustomerRepository;
import com.erp.backend.repository.ProductRepository;
import com.erp.backend.repository.SubscriptionRepository;
import com.erp.backend.service.NumberGeneratorService;
import com.erp.backend.service.VorgangService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Random;

/**
 * INITIALIZER FÜR GESCHÄFTSDATEN
 *
 * Verantwortlich für:
 * - Verträge mit konfigurierbaren Status-Verteilungen
 * - Abonnements mit Bezug zu Verträgen und Produkten
 *
 * Berücksichtigt die InitConfig für realistische Status-Verteilungen.
 */
@Service
@Transactional
public class BusinessDataInitializer {

    private static final Logger logger = LoggerFactory.getLogger(BusinessDataInitializer.class);

    // Repository-Dependencies
    private final ContractRepository contractRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;

    // Service-Dependencies
    private final NumberGeneratorService numberGeneratorService;
    private final VorgangService vorgangService;

    private final Random random = new Random();

    /**
     * KONSTRUKTOR mit Dependency Injection
     */
    public BusinessDataInitializer(ContractRepository contractRepository,
                                   SubscriptionRepository subscriptionRepository,
                                   CustomerRepository customerRepository,
                                   ProductRepository productRepository,
                                   NumberGeneratorService numberGeneratorService,
                                   VorgangService vorgangService) {
        this.contractRepository = contractRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.numberGeneratorService = numberGeneratorService;
        this.vorgangService = vorgangService;
    }

    /**
     * HAUPTMETHODE: Geschäftsdaten initialisieren
     */
    public void initializeBusinessData(InitConfig config) {
        logger.info("Starte Geschäftsdaten-Initialisierung mit Konfiguration...");
        logger.info("Vertrag-Status: {:.0f}% ACTIVE, {:.0f}% TERMINATED",
                config.getActiveContractRatio() * 100,
                config.getTerminatedContractRatio() * 100);
        logger.info("Abo-Status: {:.0f}% ACTIVE, {:.0f}% CANCELLED, {:.0f}% PAUSED",
                config.getActiveSubscriptionRatio() * 100,
                config.getCancelledSubscriptionRatio() * 100,
                config.getPausedSubscriptionRatio() * 100);

        Vorgang vorgang = vorgangService.starteAutomatischenVorgang(
                VorgangTyp.DATENIMPORT, "Geschäftsdaten-Import (Verträge, Abonnements)"
        );

        try {
            int totalOperations = 0;
            int contractCount = 0;
            int subscriptionCount = 0;

            // 1. Verträge erstellen
            if (contractRepository.count() == 0) {
                contractCount = initializeContracts(config);
                totalOperations++;
            } else {
                logger.info("Verträge bereits vorhanden - überspringe Initialisierung");
                contractCount = (int) contractRepository.count();
            }

            // 2. Abonnements erstellen
            if (subscriptionRepository.count() == 0) {
                subscriptionCount = initializeSubscriptions(config);
                totalOperations++;
            } else {
                logger.info("Abonnements bereits vorhanden - überspringe Initialisierung");
                subscriptionCount = (int) subscriptionRepository.count();
            }

            // Vorgang erfolgreich abschließen
            vorgangService.vorgangErfolgreichAbschliessen(vorgang.getId(),
                    totalOperations, totalOperations, 0, null);

            logger.info("✓ Geschäftsdaten-Initialisierung abgeschlossen: {} Verträge, {} Abonnements",
                    contractCount, subscriptionCount);

        } catch (Exception e) {
            logger.error("✗ Fehler bei Geschäftsdaten-Initialisierung", e);
            vorgangService.vorgangMitFehlerAbschliessen(vorgang.getId(), e.getMessage());
            throw e;
        }
    }

    /**
     * PRIVATE METHODE: Verträge initialisieren
     */
    private int initializeContracts(InitConfig config) {
        logger.info("Initialisiere Verträge...");

        List<Customer> customers = customerRepository.findAll();
        if (customers.isEmpty()) {
            throw new IllegalStateException("Keine Kunden gefunden - Kunden müssen zuerst initialisiert werden");
        }

        final int numberOfContracts = 20;
        int activeCount = 0;
        int terminatedCount = 0;

        for (int i = 1; i <= numberOfContracts; i++) {
            Customer customer = customers.get(random.nextInt(customers.size()));
            LocalDate startDate = LocalDate.now().minusDays(random.nextInt(365));

            Contract contract = new Contract("Servicevertrag " + i, startDate, customer);
            contract.setId(null);
            contract.setContractNumber(numberGeneratorService.generateContractNumber());

            // Status basierend auf Konfiguration setzen
            double randomValue = random.nextDouble();
            if (randomValue < config.getActiveContractRatio()) {
                contract.setContractStatus(ContractStatus.ACTIVE);
                // Wenige haben Enddatum (30% Chance)
                if (random.nextDouble() < 0.3) {
                    contract.setEndDate(startDate.plusYears(1 + random.nextInt(2)));
                }
                activeCount++;
            } else {
                contract.setContractStatus(ContractStatus.TERMINATED);
                contract.setEndDate(startDate.plusDays(random.nextInt(300)));
                terminatedCount++;
            }

            contractRepository.save(contract);
        }

        logger.info("✓ {} Verträge erstellt: {} ACTIVE, {} TERMINATED",
                numberOfContracts, activeCount, terminatedCount);
        return numberOfContracts;
    }

    /**
     * PRIVATE METHODE: Abonnements initialisieren
     */
    private int initializeSubscriptions(InitConfig config) {
        logger.info("Initialisiere Abonnements...");

        List<Contract> contracts = contractRepository.findAll();
        List<Product> products = productRepository.findAll();

        if (contracts.isEmpty()) {
            throw new IllegalStateException("Keine Verträge gefunden - Verträge müssen zuerst initialisiert werden");
        }

        // Nur monatliche Produkte für Abonnements verwenden
        List<Product> subscriptionProducts = products.stream()
                .filter(p -> "Monat".equals(p.getUnit()))
                .toList();

        if (subscriptionProducts.isEmpty()) {
            logger.warn("Keine monatlichen Produkte für Abonnements gefunden");
            return 0;
        }

        final int numberOfSubscriptions = 35;
        int activeCount = 0;
        int cancelledCount = 0;
        int pausedCount = 0;

        for (int i = 1; i <= numberOfSubscriptions; i++) {
            Contract contract = contracts.get(random.nextInt(contracts.size()));
            Product product = subscriptionProducts.get(random.nextInt(subscriptionProducts.size()));

            LocalDate subscriptionStart = contract.getStartDate().plusDays(random.nextInt(30));

            Subscription subscription = new Subscription(
                    product.getName(),
                    product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO,
                    subscriptionStart,
                    contract
            );

            subscription.setProduct(product);
            subscription.setSubscriptionNumber(numberGeneratorService.generateSubscriptionNumber());
            subscription.setDescription("Monatliches Abonnement für " + product.getName());

            // Zufälligen Abrechnungszyklus setzen
            BillingCycle[] cycles = BillingCycle.values();
            subscription.setBillingCycle(cycles[random.nextInt(cycles.length)]);
            subscription.setAutoRenewal(random.nextBoolean());

            // Status basierend auf Konfiguration UND Contract-Status setzen
            double randomValue = random.nextDouble();

            if (contract.getContractStatus() == ContractStatus.TERMINATED) {
                // Wenn Contract beendet ist, muss Subscription auch beendet/storniert sein
                subscription.setSubscriptionStatus(SubscriptionStatus.CANCELLED);
                subscription.setEndDate(contract.getEndDate());
                cancelledCount++;
            } else {
                // Contract ist aktiv - verwende Konfiguration
                if (randomValue < config.getActiveSubscriptionRatio()) {
                    subscription.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
                    activeCount++;
                } else if (randomValue < config.getActiveSubscriptionRatio() + config.getCancelledSubscriptionRatio()) {
                    subscription.setSubscriptionStatus(SubscriptionStatus.CANCELLED);
                    subscription.setEndDate(subscriptionStart.plusDays(random.nextInt(200)));
                    cancelledCount++;
                } else {
                    subscription.setSubscriptionStatus(SubscriptionStatus.PAUSED);
                    pausedCount++;
                }
            }

            subscriptionRepository.save(subscription);
        }

        logger.info("✓ {} Abonnements erstellt: {} ACTIVE, {} CANCELLED, {} PAUSED",
                numberOfSubscriptions, activeCount, cancelledCount, pausedCount);
        return numberOfSubscriptions;
    }
}