package com.erp.backend.service;

import com.erp.backend.domain.*;
import com.erp.backend.entity.Role;
import com.erp.backend.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Random;

/**
 * Service zum Initialisieren von Testdaten für das ERP-System.
 *
 * NEUE STANDARD-LOGIK: Alle Entitäten werden standardmäßig auf ACTIVE erstellt!
 * - Verträge: ACTIVE (statt gemischt mit TERMINATED)
 * - Abonnements: ACTIVE (statt gemischt mit CANCELLED)
 * - Rechnungen: ACTIVE (statt DRAFT/SENT/CANCELLED)
 * - DueSchedules: ACTIVE (statt gemischt mit COMPLETED/PAUSED)
 * - OpenItems: OPEN (aktiv, bereit für Zahlung)
 *
 * Variable Steuerung über InitConfig-Parameter für realistische Test-Szenarien.
 */
@Service
@ConditionalOnProperty(name = "app.init.enabled", havingValue = "true")
public class InitDataService  {

    private static final Logger logger = LoggerFactory.getLogger(InitDataService.class);

    /**
     * Konfiguration für die Initialisierung - steuert Status-Verteilungen
     */
    public static class InitConfig {
        // Vertrag-Konfiguration
        private double activeContractRatio = 1.0;  // Standard: 100% ACTIVE
        private double terminatedContractRatio = 0.0;  // Standard: 0% TERMINATED

        // Abonnement-Konfiguration
        private double activeSubscriptionRatio = 1.0;  // Standard: 100% ACTIVE
        private double cancelledSubscriptionRatio = 0.0;  // Standard: 0% CANCELLED
        private double pausedSubscriptionRatio = 0.0;  // Standard: 0% PAUSED

        // Rechnung-Konfiguration
        private double activeInvoiceRatio = 1.0;  // Standard: 100% ACTIVE
        private double draftInvoiceRatio = 0.0;  // Standard: 0% DRAFT
        private double sentInvoiceRatio = 0.0;  // Standard: 0% SENT
        private double cancelledInvoiceRatio = 0.0;  // Standard: 0% CANCELLED

        // DueSchedule-Konfiguration
        private double activeDueScheduleRatio = 1.0;  // Standard: 100% ACTIVE
        private double completedDueScheduleRatio = 0.0;  // Standard: 0% COMPLETED
        private double pausedDueScheduleRatio = 0.0;  // Standard: 0% PAUSED

        // OpenItem-Konfiguration
        private double openOpenItemRatio = 1.0;  // Standard: 100% OPEN
        private double paidOpenItemRatio = 0.0;  // Standard: 0% PAID
        private double partiallyPaidOpenItemRatio = 0.0;  // Standard: 0% PARTIALLY_PAID

        // Getters und Setters
        public double getActiveContractRatio() { return activeContractRatio; }
        public void setActiveContractRatio(double activeContractRatio) { this.activeContractRatio = activeContractRatio; }
        public double getTerminatedContractRatio() { return terminatedContractRatio; }
        public void setTerminatedContractRatio(double terminatedContractRatio) { this.terminatedContractRatio = terminatedContractRatio; }

        public double getActiveSubscriptionRatio() { return activeSubscriptionRatio; }
        public void setActiveSubscriptionRatio(double activeSubscriptionRatio) { this.activeSubscriptionRatio = activeSubscriptionRatio; }
        public double getCancelledSubscriptionRatio() { return cancelledSubscriptionRatio; }
        public void setCancelledSubscriptionRatio(double cancelledSubscriptionRatio) { this.cancelledSubscriptionRatio = cancelledSubscriptionRatio; }
        public double getPausedSubscriptionRatio() { return pausedSubscriptionRatio; }
        public void setPausedSubscriptionRatio(double pausedSubscriptionRatio) { this.pausedSubscriptionRatio = pausedSubscriptionRatio; }

        public double getActiveInvoiceRatio() { return activeInvoiceRatio; }
        public void setActiveInvoiceRatio(double activeInvoiceRatio) { this.activeInvoiceRatio = activeInvoiceRatio; }
        public double getDraftInvoiceRatio() { return draftInvoiceRatio; }
        public void setDraftInvoiceRatio(double draftInvoiceRatio) { this.draftInvoiceRatio = draftInvoiceRatio; }
        public double getSentInvoiceRatio() { return sentInvoiceRatio; }
        public void setSentInvoiceRatio(double sentInvoiceRatio) { this.sentInvoiceRatio = sentInvoiceRatio; }
        public double getCancelledInvoiceRatio() { return cancelledInvoiceRatio; }
        public void setCancelledInvoiceRatio(double cancelledInvoiceRatio) { this.cancelledInvoiceRatio = cancelledInvoiceRatio; }

        public double getActiveDueScheduleRatio() { return activeDueScheduleRatio; }
        public void setActiveDueScheduleRatio(double activeDueScheduleRatio) { this.activeDueScheduleRatio = activeDueScheduleRatio; }
        public double getCompletedDueScheduleRatio() { return completedDueScheduleRatio; }
        public void setCompletedDueScheduleRatio(double completedDueScheduleRatio) { this.completedDueScheduleRatio = completedDueScheduleRatio; }
        public double getPausedDueScheduleRatio() { return pausedDueScheduleRatio; }
        public void setPausedDueScheduleRatio(double pausedDueScheduleRatio) { this.pausedDueScheduleRatio = pausedDueScheduleRatio; }

        public double getOpenOpenItemRatio() { return openOpenItemRatio; }
        public void setOpenOpenItemRatio(double openOpenItemRatio) { this.openOpenItemRatio = openOpenItemRatio; }
        public double getPaidOpenItemRatio() { return paidOpenItemRatio; }
        public void setPaidOpenItemRatio(double paidOpenItemRatio) { this.paidOpenItemRatio = paidOpenItemRatio; }
        public double getPartiallyPaidOpenItemRatio() { return partiallyPaidOpenItemRatio; }
        public void setPartiallyPaidOpenItemRatio(double partiallyPaidOpenItemRatio) { this.partiallyPaidOpenItemRatio = partiallyPaidOpenItemRatio; }

        /**
         * Factory-Methoden für verschiedene Szenarien
         */
        public static InitConfig allActive() {
            return new InitConfig(); // Default ist bereits alles ACTIVE
        }

        public static InitConfig realistic() {
            InitConfig config = new InitConfig();
            // Verträge: 80% aktiv, 20% beendet
            config.setActiveContractRatio(0.8);
            config.setTerminatedContractRatio(0.2);

            // Abos: 85% aktiv, 10% storniert, 5% pausiert
            config.setActiveSubscriptionRatio(0.85);
            config.setCancelledSubscriptionRatio(0.10);
            config.setPausedSubscriptionRatio(0.05);

            // Rechnungen: 60% aktiv, 20% versendet, 15% entwurf, 5% storniert
            config.setActiveInvoiceRatio(0.60);
            config.setSentInvoiceRatio(0.20);
            config.setDraftInvoiceRatio(0.15);
            config.setCancelledInvoiceRatio(0.05);

            // DueSchedules: 40% aktiv, 50% abgerechnet, 10% pausiert
            config.setActiveDueScheduleRatio(0.40);
            config.setCompletedDueScheduleRatio(0.50);
            config.setPausedDueScheduleRatio(0.10);

            // OpenItems: 60% offen, 25% bezahlt, 15% teilweise bezahlt
            config.setOpenOpenItemRatio(0.60);
            config.setPaidOpenItemRatio(0.25);
            config.setPartiallyPaidOpenItemRatio(0.15);

            return config;
        }

        public static InitConfig development() {
            InitConfig config = new InitConfig();
            // Für Development: Mehr aktive Daten für Testing
            config.setActiveContractRatio(0.95);
            config.setTerminatedContractRatio(0.05);

            config.setActiveSubscriptionRatio(0.90);
            config.setCancelledSubscriptionRatio(0.05);
            config.setPausedSubscriptionRatio(0.05);

            config.setActiveInvoiceRatio(0.80);
            config.setSentInvoiceRatio(0.15);
            config.setDraftInvoiceRatio(0.05);

            config.setActiveDueScheduleRatio(0.70);
            config.setCompletedDueScheduleRatio(0.25);
            config.setPausedDueScheduleRatio(0.05);

            config.setOpenOpenItemRatio(0.80);
            config.setPaidOpenItemRatio(0.15);
            config.setPartiallyPaidOpenItemRatio(0.05);

            return config;
        }
    }

    /**
     * Enum für verschiedene Initialisierungsmodi
     */
    public enum InitMode {
        BASIC("Nur Stammdaten"),
        CONTRACTS("Bis Verträge und Subscriptions"),
        SCHEDULES("Bis Fälligkeitspläne"),
        INVOICES_MANUAL("Mit manuellen Sample-Rechnungen"),
        FULL("Komplett mit Rechnungslauf"),
        FULL_WITH_BILLING("Komplett mit Rechnungslauf bis Stichtag");

        private final String description;
        InitMode(String description) { this.description = description; }
        public String getDescription() { return description; }
    }

    // Neue Property für automatischen Start
    //@Value("${app.init.auto-run-on-startup:false}")
    private boolean autoRunOnStartup = false;

    // Repository-Abhängigkeiten
    private final AddressRepository addressRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final ContractRepository contractRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final DueScheduleRepository dueScheduleRepository;
    private final OpenItemRepository openItemRepository;
    private final InvoiceRepository invoiceRepository;

    // Service-Abhängigkeiten
    private final InvoiceService invoiceService;
    private final InvoiceBatchService invoiceBatchService;
    private final NumberGeneratorService numberGeneratorService;
    private final UserDetailsServiceImpl userDetailsService;

    private final Random random = new Random();

    public InitDataService(AddressRepository addressRepository,
                           CustomerRepository customerRepository,
                           ProductRepository productRepository,
                           ContractRepository contractRepository,
                           SubscriptionRepository subscriptionRepository,
                           DueScheduleRepository dueScheduleRepository,
                           OpenItemRepository openItemRepository,
                           InvoiceRepository invoiceRepository,
                           InvoiceService invoiceService,
                           InvoiceBatchService invoiceBatchService,
                           NumberGeneratorService numberGeneratorService,
                           UserDetailsServiceImpl userDetailsService) {
        this.addressRepository = addressRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.contractRepository = contractRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.dueScheduleRepository = dueScheduleRepository;
        this.openItemRepository = openItemRepository;
        this.invoiceRepository = invoiceRepository;
        this.invoiceService = invoiceService;
        this.invoiceBatchService = invoiceBatchService;
        this.numberGeneratorService = numberGeneratorService;
        this.userDetailsService = userDetailsService;
    }


//    public void run(ApplicationArguments args) throws Exception {
//        if (!autoRunOnStartup) {
//            logger.info("Auto-run-on-startup is disabled. Skipping automatic data initialization.");
//            logger.info("Use REST endpoints at /api/init/* for manual initialization.");
//            return;
//        }
//
//        logger.info("🚀 Starting automatic data initialization with FULL mode and ALL-ACTIVE standard...");
//
//        try {
//            // Standard: Alle Entitäten auf ACTIVE
//            initData(InitMode.FULL, LocalDate.now(), InitConfig.allActive());
//            logger.info("✅ Data initialization completed successfully - ALL entities set to ACTIVE by default");
//        } catch (Exception e) {
//            logger.error("❌ Data initialization failed", e);
//        }
//    }

    // ===============================================================================================
    // HAUPT-INITIALISIERUNGS-METHODEN
    // ===============================================================================================

    /**
     * Standard-Initialisierung: Alles ACTIVE
     */
    @Transactional
    public void initAllData() {
        initData(InitMode.FULL, null, InitConfig.allActive());
    }

    /**
     * Initialisierung mit Modus
     */
    @Transactional
    public void initData(InitMode mode) {
        initData(mode, null, InitConfig.allActive());
    }

    /**
     * Initialisierung mit Modus und Konfiguration
     */
    @Transactional
    public void initData(InitMode mode, InitConfig config) {
        initData(mode, null, config);
    }

    /**
     * Vollständige Initialisierung mit allen Parametern
     */
    @Transactional
    public void initData(InitMode mode, LocalDate billingDate, InitConfig config) {
        if (config == null) {
            config = InitConfig.allActive();
        }

        logger.info("===========================================");
        logger.info("Starte Testdaten-Initialisierung");
        logger.info("Modus: {} - {}", mode, mode.getDescription());
        logger.info("Konfiguration: {}", getConfigDescription(config));
        if (billingDate != null) {
            logger.info("Rechnungslauf-Stichtag: {}", billingDate);
        }
        logger.info("===========================================");

        // Basis-Daten (immer)
        initUser();
        initAddresses();
        initCustomers();
        initProducts();

        if (mode == InitMode.BASIC) {
            logger.info("Basis-Initialisierung abgeschlossen (BASIC Mode)");
            logCurrentDataStatus();
            return;
        }

        // Verträge und Subscriptions mit Konfiguration
        initContracts(config);
        initSubscriptions(config);

        if (mode == InitMode.CONTRACTS) {
            logger.info("Initialisierung bis Verträge abgeschlossen (CONTRACTS Mode)");
            logCurrentDataStatus();
            return;
        }

        // Fälligkeitspläne mit Konfiguration
        initDueSchedules(config);

        if (mode == InitMode.SCHEDULES) {
            logger.info("Initialisierung bis Fälligkeitspläne abgeschlossen (SCHEDULES Mode)");
            logCurrentDataStatus();
            return;
        }

        // Manuelle Sample-Rechnungen mit Konfiguration
        if (mode == InitMode.INVOICES_MANUAL || mode == InitMode.FULL) {
            createSampleInvoices(config);
            createSampleOpenItems(config);
        }

        if (mode == InitMode.INVOICES_MANUAL) {
            logger.info("Initialisierung mit manuellen Rechnungen abgeschlossen (INVOICES_MANUAL Mode)");
            logCurrentDataStatus();
            return;
        }

        // Rechnungslauf durchführen
        if (mode == InitMode.FULL || mode == InitMode.FULL_WITH_BILLING) {
            LocalDate stichtag = billingDate != null ? billingDate : LocalDate.now();
            runBillingProcess(stichtag);
        }

        logger.info("Vollständige Initialisierung abgeschlossen (FULL Mode)");
        logCurrentDataStatus();
    }

    private String getConfigDescription(InitConfig config) {
        if (config.getActiveContractRatio() == 1.0 &&
                config.getActiveSubscriptionRatio() == 1.0 &&
                config.getActiveInvoiceRatio() == 1.0 &&
                config.getActiveDueScheduleRatio() == 1.0) {
            return "ALL-ACTIVE (Standard)";
        } else {
            return String.format("Custom (Contracts: %.0f%% active, Subscriptions: %.0f%% active, Invoices: %.0f%% active)",
                    config.getActiveContractRatio() * 100,
                    config.getActiveSubscriptionRatio() * 100,
                    config.getActiveInvoiceRatio() * 100);
        }
    }

    // ===============================================================================================
    // CONVENIENCE-METHODEN MIT VERSCHIEDENEN KONFIGURATIONEN
    // ===============================================================================================

    @Transactional
    public void initRealisticTestData() {
        initData(InitMode.FULL, LocalDate.now(), InitConfig.realistic());
    }

    @Transactional
    public void initDevelopmentData() {
        initData(InitMode.FULL, LocalDate.now(), InitConfig.development());
    }

    @Transactional
    public void initAllActiveData() {
        initData(InitMode.FULL, LocalDate.now(), InitConfig.allActive());
    }

    // ===============================================================================================
    // BESTEHENDE INIT-METHODEN (unverändert)
    // ===============================================================================================

    @Transactional
    private void initUser() {
        userDetailsService.createUserSafe("a","a", Role.ROLE_ADMIN);
        userDetailsService.createUserSafe("string","string", Role.ROLE_ADMIN);
    }

    private void initAddresses() {
        if (addressRepository.count() > 0) {
            logger.info("Adressen bereits vorhanden - Überspringe Initialisierung");
            return;
        }

        logger.info("Initialisiere Adressen...");

        String[][] addressData = {
                {"Hauptstraße 12", "10115", "Berlin", "Germany"},
                {"Lindenweg 5", "04109", "Leipzig", "Germany"},
                {"Gartenstraße 8", "80331", "München", "Germany"},
                {"Berliner Allee 20", "30159", "Hannover", "Germany"},
                {"Schillerstraße 15", "50667", "Köln", "Germany"},
                {"Goetheweg 9", "90402", "Nürnberg", "Germany"},
                {"Friedrichstraße 3", "01067", "Dresden", "Germany"},
                {"Rosenweg 18", "28195", "Bremen", "Germany"},
                {"Bahnhofstraße 4", "20095", "Hamburg", "Germany"},
                {"Marktstraße 22", "45127", "Essen", "Germany"},
                {"Am Stadtpark 7", "60311", "Frankfurt", "Germany"},
                {"Kirchgasse 14", "70173", "Stuttgart", "Germany"},
                {"Seestraße 33", "40213", "Düsseldorf", "Germany"},
                {"Waldweg 11", "44135", "Dortmund", "Germany"},
                {"Blumenstraße 6", "24103", "Kiel", "Germany"}
        };

        for (String[] data : addressData) {
            Address address = new Address(data[0], data[1], data[2], data[3]);
            addressRepository.save(address);
        }

        logger.info("✓ {} Adressen erstellt", addressData.length);
    }

    private void initCustomers() {
        if (customerRepository.count() > 0) {
            logger.info("Kunden bereits vorhanden - Überspringe Initialisierung");
            return;
        }

        logger.info("Initialisiere Kunden...");

        List<Address> addresses = addressRepository.findAll();
        if (addresses.isEmpty()) {
            throw new IllegalStateException("Keine Adressen gefunden - Adressen müssen zuerst initialisiert werden");
        }

        String[] firstNames = {"Max", "Anna", "Tom", "Laura", "Paul", "Sophie", "Lukas", "Marie",
                "Felix", "Emma", "Jonas", "Lea", "Ben", "Mia", "Leon", "Hannah",
                "Noah", "Lena", "Finn", "Emilia"};
        String[] lastNames = {"Müller", "Schmidt", "Schneider", "Fischer", "Weber", "Becker",
                "Hoffmann", "Schäfer", "Koch", "Richter", "Klein", "Wolf",
                "Schröder", "Neumann", "Schwarz", "Zimmermann"};

        final int numberOfCustomers = 25;
        for (int i = 1; i <= numberOfCustomers; i++) {
            String firstName = firstNames[random.nextInt(firstNames.length)];
            String lastName = lastNames[random.nextInt(lastNames.length)];
            String email = firstName.toLowerCase() + "." + lastName.toLowerCase() + i + "@test.com";
            String phoneNumber = "+49" + (random.nextInt(900000000) + 100000000);

            Customer customer = new Customer(firstName, lastName, email, phoneNumber);
            customer.setCustomerNumber(numberGeneratorService.generateCustomerNumber());
            customer.setBillingAddress(addresses.get(random.nextInt(addresses.size())));
            customer.setShippingAddress(addresses.get(random.nextInt(addresses.size())));
            customer.setResidentialAddress(addresses.get(random.nextInt(addresses.size())));

            customerRepository.save(customer);
        }

        logger.info("✓ {} Kunden erstellt", numberOfCustomers);
    }

    private void initProducts() {
        if (productRepository.count() > 0) {
            logger.info("Produkte bereits vorhanden - Überspringe Initialisierung");
            return;
        }

        logger.info("Initialisiere Produktkatalog...");

        Object[][] products = {
                // Software-Lizenzen (monatliche Abonnements)
                {"Adobe Creative Cloud", 59.99, "Monat", "Software"},
                {"Microsoft 365 Business Premium", 18.90, "Monat", "Software"},
                {"Salesforce Professional", 75.00, "Monat", "Software"},
                {"Slack Pro", 7.25, "Monat", "Software"},
                {"Zoom Pro", 14.99, "Monat", "Software"},
                {"Dropbox Business", 15.00, "Monat", "Software"},

                // Cloud & Hosting Services (monatliche Abonnements)
                {"AWS Cloud Hosting Standard", 89.90, "Monat", "Cloud"},
                {"Azure Database Service", 125.00, "Monat", "Cloud"},
                {"Google Workspace Business", 12.00, "Monat", "Cloud"},
                {"GitHub Enterprise", 21.00, "Monat", "Cloud"},
                {"Atlassian Confluence", 5.50, "Monat", "Cloud"},
                {"Docker Pro", 9.00, "Monat", "Cloud"},

                // Hardware-Produkte (einmalige Käufe)
                {"Laptop Dell XPS 13", 1200.0, "Stück", "Hardware"},
                {"MacBook Pro 14\"", 2200.0, "Stück", "Hardware"},
                {"Samsung Galaxy S23", 900.0, "Stück", "Hardware"},
                {"iPhone 15 Pro", 1300.0, "Stück", "Hardware"},
                {"iPad Air", 650.0, "Stück", "Hardware"},
                {"Surface Pro 9", 1100.0, "Stück", "Hardware"}
        };

        for (int i = 0; i < products.length; i++) {
            Product product = new Product(
                    (String) products[i][0],
                    BigDecimal.valueOf((Double) products[i][1]),
                    (String) products[i][2]
            );

            product.setProductNumber("PROD-" + String.format("%03d", i + 1));

            if (products[i].length > 3) {
                product.setDescription("Kategorie: " + products[i][3]);
            }

            productRepository.save(product);
        }

        logger.info("✓ {} Produkte erstellt", products.length);
    }

    // ===============================================================================================
    // ANGEPASSTE INIT-METHODEN MIT CONFIG-STEUERUNG
    // ===============================================================================================

    private void initContracts(InitConfig config) {
        if (contractRepository.count() > 0) {
            logger.info("Verträge bereits vorhanden - Überspringe Initialisierung");
            return;
        }

        logger.info("Initialisiere Verträge mit Konfiguration...");
        logger.info("Status-Verteilung: {:.0f}% ACTIVE, {:.0f}% TERMINATED",
                config.getActiveContractRatio() * 100,
                config.getTerminatedContractRatio() * 100);

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

        logger.info("✓ {} Verträge erstellt: {} ACTIVE, {} TERMINATED", numberOfContracts, activeCount, terminatedCount);
    }

    private void initSubscriptions(InitConfig config) {
        if (subscriptionRepository.count() > 0) {
            logger.info("Abonnements bereits vorhanden - Überspringe Initialisierung");
            return;
        }

        logger.info("Initialisiere Abonnements mit Konfiguration...");
        logger.info("Status-Verteilung: {:.0f}% ACTIVE, {:.0f}% CANCELLED, {:.0f}% PAUSED",
                config.getActiveSubscriptionRatio() * 100,
                config.getCancelledSubscriptionRatio() * 100,
                config.getPausedSubscriptionRatio() * 100);

        List<Contract> contracts = contractRepository.findAll();
        List<Product> products = productRepository.findAll();

        if (contracts.isEmpty()) {
            throw new IllegalStateException("Keine Verträge gefunden - Verträge müssen zuerst initialisiert werden");
        }

        List<Product> subscriptionProducts = products.stream()
                .filter(p -> "Monat".equals(p.getUnit()))
                .toList();

        if (subscriptionProducts.isEmpty()) {
            logger.warn("Keine monatlichen Produkte für Abonnements gefunden");
            return;
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
    }

    private void initDueSchedules(InitConfig config) {
        if (dueScheduleRepository.count() > 0) {
            logger.info("Fälligkeitspläne bereits vorhanden - Überspringe Initialisierung");
            return;
        }

        logger.info("Initialisiere Fälligkeitspläne mit Konfiguration...");
        logger.info("Status-Verteilung: {:.0f}% ACTIVE, {:.0f}% COMPLETED, {:.0f}% PAUSED",
                config.getActiveDueScheduleRatio() * 100,
                config.getCompletedDueScheduleRatio() * 100,
                config.getPausedDueScheduleRatio() * 100);

        List<Subscription> activeSubscriptions = subscriptionRepository.findBySubscriptionStatus(SubscriptionStatus.ACTIVE);
        if (activeSubscriptions.isEmpty()) {
            logger.warn("Keine aktiven Abonnements für Fälligkeitspläne gefunden");
            return;
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
    }

    private void createSampleInvoices(InitConfig config) {
        if (invoiceRepository.count() > 0) {
            logger.info("Rechnungen bereits vorhanden - Überspringe Erstellung");
            return;
        }

        logger.info("Erstelle Sample-Rechnungen mit Konfiguration...");
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
            return;
        }

        final int numberOfInvoices = 30;
        int invoicesCreated = 0;
        int openItemsCreated = 0;
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
                Invoice savedInvoice = invoiceService.createInvoice(invoice);
                invoicesCreated++;

                // OpenItem nur für nicht-stornierte Rechnungen erstellen
                if (savedInvoice.getStatus() != Invoice.InvoiceStatus.CANCELLED &&
                        savedInvoice.getTotalAmount() != null &&
                        savedInvoice.getTotalAmount().compareTo(BigDecimal.ZERO) > 0) {

                    try {
                        OpenItem openItem = new OpenItem(savedInvoice,
                                "Sample-OpenItem für Rechnung " + savedInvoice.getInvoiceNumber(),
                                savedInvoice.getTotalAmount(),
                                savedInvoice.getDueDate());

                        // OpenItem-Status basierend auf Konfiguration setzen
                        applyOpenItemConfig(openItem, config);

                        OpenItem savedOpenItem = openItemRepository.save(openItem);
                        openItemsCreated++;

                        logger.debug("OpenItem erstellt für Sample-Rechnung {}: {} EUR",
                                savedInvoice.getInvoiceNumber(), savedInvoice.getTotalAmount());

                    } catch (Exception e) {
                        logger.error("Fehler beim Erstellen des OpenItems für Sample-Rechnung {}: {}",
                                savedInvoice.getInvoiceNumber(), e.getMessage());
                    }
                }

            } catch (Exception e) {
                logger.error("Fehler beim Erstellen einer Sample-Rechnung: {}", e.getMessage());
            }
        }

        logger.info("✓ {} Sample-Rechnungen erstellt: {} ACTIVE, {} DRAFT, {} SENT, {} CANCELLED",
                invoicesCreated, activeCount, draftCount, sentCount, cancelledCount);
        logger.info("✓ {} Sample-OpenItems erstellt", openItemsCreated);
    }

    private void createSampleOpenItems(InitConfig config) {
        logger.info("Erstelle zusätzliche Sample-OpenItems mit Konfiguration...");

        List<Invoice> invoicesWithoutOpenItems = invoiceRepository.findInvoicesWithoutOpenItems();

        if (invoicesWithoutOpenItems.isEmpty()) {
            logger.warn("Keine Rechnungen für zusätzliche OpenItems gefunden");
            return;
        }

        int openItemsCreated = 0;
        int openCount = 0;
        int paidCount = 0;
        int partiallyPaidCount = 0;

        invoicesWithoutOpenItems = invoicesWithoutOpenItems.stream()
                .filter(invoice -> invoice.getStatus() != Invoice.InvoiceStatus.CANCELLED)
                .filter(invoice -> invoice.getTotalAmount() != null && invoice.getTotalAmount().compareTo(BigDecimal.ZERO) > 0)
                .toList();

        for (Invoice invoice : invoicesWithoutOpenItems) {
            try {
                OpenItem openItem = new OpenItem(invoice,
                        "Nachträglich erstellter OpenItem für Rechnung " + invoice.getInvoiceNumber(),
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

                logger.debug("Zusätzlicher OpenItem erstellt für Rechnung {} mit Status {}",
                        invoice.getInvoiceNumber(), openItem.getStatus());

            } catch (Exception e) {
                logger.error("Fehler beim Erstellen eines zusätzlichen OpenItems für Rechnung {}: {}",
                        invoice.getInvoiceNumber(), e.getMessage());
            }
        }

        logger.info("✓ {} zusätzliche Sample-OpenItems erstellt: {} OPEN/OVERDUE, {} PAID, {} PARTIALLY_PAID",
                openItemsCreated, openCount, paidCount, partiallyPaidCount);
    }

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

    // ===============================================================================================
    // RECHNUNGSLAUF (unverändert)
    // ===============================================================================================

    private void runBillingProcess(LocalDate billingDate) {
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

    // ===============================================================================================
    // STATUS UND MANAGEMENT (angepasst für neue Konfiguration)
    // ===============================================================================================

    @Transactional(readOnly = true)
    public void logCurrentDataStatus() {
        logger.info("===========================================");
        logger.info("AKTUELLER DATENBESTAND");
        logger.info("===========================================");
        logger.info("Stammdaten:");
        logger.info("  - Adressen: {}", addressRepository.count());
        logger.info("  - Kunden: {}", customerRepository.count());
        logger.info("  - Produkte: {}", productRepository.count());

        logger.info("Verträge & Abos:");
        long totalContracts = contractRepository.count();
        long activeContracts = contractRepository.countByContractStatus(ContractStatus.ACTIVE);
        long terminatedContracts = contractRepository.countByContractStatus(ContractStatus.TERMINATED);
        logger.info("  - Verträge gesamt: {} ({}% ACTIVE, {}% TERMINATED)", totalContracts,
                totalContracts > 0 ? (activeContracts * 100 / totalContracts) : 0,
                totalContracts > 0 ? (terminatedContracts * 100 / totalContracts) : 0);

        long totalSubscriptions = subscriptionRepository.count();
        long activeSubscriptions = subscriptionRepository.countBySubscriptionStatus(SubscriptionStatus.ACTIVE);
        long cancelledSubscriptions = subscriptionRepository.countBySubscriptionStatus(SubscriptionStatus.CANCELLED);
        long pausedSubscriptions = subscriptionRepository.countBySubscriptionStatus(SubscriptionStatus.PAUSED);
        logger.info("  - Abonnements gesamt: {} ({}% ACTIVE, {}% CANCELLED, {}% PAUSED)", totalSubscriptions,
                totalSubscriptions > 0 ? (activeSubscriptions * 100 / totalSubscriptions) : 0,
                totalSubscriptions > 0 ? (cancelledSubscriptions * 100 / totalSubscriptions) : 0,
                totalSubscriptions > 0 ? (pausedSubscriptions * 100 / totalSubscriptions) : 0);

        logger.info("Fälligkeiten:");
        long activeSchedules = dueScheduleRepository.countByStatus(DueStatus.ACTIVE);
        long pausedSchedules = dueScheduleRepository.countByStatus(DueStatus.PAUSED);
        long completedSchedules = dueScheduleRepository.countByStatus(DueStatus.COMPLETED);
        long totalSchedules = dueScheduleRepository.count();
        logger.info("  - Gesamt: {} ({}% ACTIVE, {}% COMPLETED, {}% PAUSED)", totalSchedules,
                totalSchedules > 0 ? (activeSchedules * 100 / totalSchedules) : 0,
                totalSchedules > 0 ? (completedSchedules * 100 / totalSchedules) : 0,
                totalSchedules > 0 ? (pausedSchedules * 100 / totalSchedules) : 0);

        logger.info("Rechnungen:");
        long totalInvoices = invoiceRepository.count();
        long activeInvoices = invoiceService.getInvoiceCountByStatus(Invoice.InvoiceStatus.ACTIVE);
        long draftInvoices = invoiceService.getInvoiceCountByStatus(Invoice.InvoiceStatus.DRAFT);
        long sentInvoices = invoiceService.getInvoiceCountByStatus(Invoice.InvoiceStatus.SENT);
        long cancelledInvoices = invoiceService.getInvoiceCountByStatus(Invoice.InvoiceStatus.CANCELLED);
        logger.info("  - Gesamt: {} ({}% ACTIVE, {}% DRAFT, {}% SENT, {}% CANCELLED)", totalInvoices,
                totalInvoices > 0 ? (activeInvoices * 100 / totalInvoices) : 0,
                totalInvoices > 0 ? (draftInvoices * 100 / totalInvoices) : 0,
                totalInvoices > 0 ? (sentInvoices * 100 / totalInvoices) : 0,
                totalInvoices > 0 ? (cancelledInvoices * 100 / totalInvoices) : 0);

        logger.info("Offene Posten:");
        long totalOpenItems = openItemRepository.count();
        long openOpenItems = openItemRepository.countByStatus(OpenItem.OpenItemStatus.OPEN);
        long partiallyPaidOpenItems = openItemRepository.countByStatus(OpenItem.OpenItemStatus.PARTIALLY_PAID);
        long paidOpenItems = openItemRepository.countByStatus(OpenItem.OpenItemStatus.PAID);
        long overdueOpenItems = openItemRepository.countByStatus(OpenItem.OpenItemStatus.OVERDUE);
        long cancelledOpenItems = openItemRepository.countByStatus(OpenItem.OpenItemStatus.CANCELLED);

        logger.info("  - Gesamt: {} ({}% OPEN, {}% PAID, {}% PARTIALLY_PAID, {}% OVERDUE, {}% CANCELLED)", totalOpenItems,
                totalOpenItems > 0 ? (openOpenItems * 100 / totalOpenItems) : 0,
                totalOpenItems > 0 ? (paidOpenItems * 100 / totalOpenItems) : 0,
                totalOpenItems > 0 ? (partiallyPaidOpenItems * 100 / totalOpenItems) : 0,
                totalOpenItems > 0 ? (overdueOpenItems * 100 / totalOpenItems) : 0,
                totalOpenItems > 0 ? (cancelledOpenItems * 100 / totalOpenItems) : 0);

        // Konsistenz-Prüfung
        logger.info("Konsistenz-Prüfung:");
        long invoicesWithoutOpenItems = countInvoicesWithoutOpenItems();
        long openItemsWithoutInvoices = openItemRepository.findAll().stream()
                .filter(openItem -> openItem.getInvoice() == null)
                .count();

        logger.info("  - Rechnungen ohne OpenItems: {}", invoicesWithoutOpenItems);
        logger.info("  - OpenItems ohne Rechnungen: {}", openItemsWithoutInvoices);

        if (invoicesWithoutOpenItems == 0 && openItemsWithoutInvoices == 0) {
            logger.info("  ✓ Konsistenz-Prüfung erfolgreich!");
        } else {
            logger.warn("  ⚠ Konsistenz-Probleme gefunden!");
        }

        logger.info("===========================================");
    }

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

    @Transactional
    public void clearAllTestData() {
        logger.warn("WARNUNG: Lösche alle Testdaten...");

        try {
            openItemRepository.deleteAll();
            logger.info("OpenItems gelöscht");

            invoiceRepository.deleteAll();
            logger.info("Invoices gelöscht");

            dueScheduleRepository.deleteAll();
            logger.info("DueSchedules gelöscht");

            subscriptionRepository.deleteAll();
            logger.info("Subscriptions gelöscht");

            contractRepository.deleteAll();
            logger.info("Contracts gelöscht");

            productRepository.deleteAll();
            logger.info("Products gelöscht");

            customerRepository.deleteAll();
            logger.info("Customers gelöscht");

            addressRepository.deleteAll();
            logger.info("Addresses gelöscht");

            logger.info("✓ Alle Testdaten wurden erfolgreich gelöscht.");

        } catch (Exception e) {
            logger.error("Fehler beim Löschen der Testdaten: {}", e.getMessage(), e);
            throw new RuntimeException("Fehler beim Löschen der Testdaten", e);
        }
    }

    @Transactional
    public void repairDataConsistency() {
        logger.info("Starte Daten-Konsistenz-Reparatur...");

        try {
            createMissingOpenItems();
            cleanupOrphanedOpenItems();
            updateOverdueOpenItems();
            logger.info("Daten-Konsistenz-Reparatur abgeschlossen");
        } catch (Exception e) {
            logger.error("Fehler bei der Daten-Konsistenz-Reparatur: {}", e.getMessage(), e);
        }
    }

    private void cleanupOrphanedOpenItems() {
        List<OpenItem> orphanedItems = openItemRepository.findAll().stream()
                .filter(openItem -> openItem.getInvoice() == null)
                .toList();

        if (!orphanedItems.isEmpty()) {
            openItemRepository.deleteAll(orphanedItems);
            logger.info("✓ {} verwaiste OpenItems entfernt", orphanedItems.size());
        }
    }

    private void updateOverdueOpenItems() {
        List<OpenItem> openItems = openItemRepository.findAll().stream()
                .filter(oi -> oi.getStatus() == OpenItem.OpenItemStatus.OPEN)
                .filter(oi -> oi.getDueDate() != null && oi.getDueDate().isBefore(LocalDate.now()))
                .toList();

        for (OpenItem item : openItems) {
            item.setStatus(OpenItem.OpenItemStatus.OVERDUE);
            openItemRepository.save(item);
        }

        if (!openItems.isEmpty()) {
            logger.info("✓ {} OpenItems auf OVERDUE aktualisiert", openItems.size());
        }
    }
}