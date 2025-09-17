package com.erp.backend.service;

import com.erp.backend.domain.*;
import com.erp.backend.entity.Role;
import com.erp.backend.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * Flexible Initialisierung mit verschiedenen Modi:
 * - BASIC: Nur Stammdaten (Adressen, Kunden, Produkte)
 * - CONTRACTS: Bis Verträge und Subscriptions
 * - SCHEDULES: Bis DueSchedules (ohne Rechnungen)
 * - INVOICES_MANUAL: + manuell erstellte Sample-Rechnungen
 * - FULL: Komplette Initialisierung inkl. Rechnungslauf
 */
@Service
@ConditionalOnProperty(name = "app.init.enabled", havingValue = "true")
public class InitDataService implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(InitDataService.class);

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

        InitMode(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

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
                           NumberGeneratorService numberGeneratorService, UserDetailsServiceImpl userDetailsService) {
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

    @Override
    public void run(ApplicationArguments args) throws Exception {
        logger.info("🚀 Starting automatic data initialization with FULL mode...");

        try {
            // Deine bestehende initData Methode mit InitMode.FULL aufrufen
            initData(InitMode.FULL, LocalDate.now());

            logger.info("✅ Data initialization completed successfully");
        } catch (Exception e) {
            logger.error("❌ Data initialization failed", e);
            // Optional: Exception weiterwerfen falls App nicht starten soll bei Fehler
            // throw e;
        }
    }

    /**
     * Hauptmethode mit Standard-Modus (FULL)
     */
    @Transactional
    public void initAllData() {
        initData(InitMode.FULL, null);
    }

    /**
     * Flexible Initialisierung mit Modus-Auswahl
     *
     * @param mode Initialisierungsmodus
     */
    @Transactional
    public void initData(InitMode mode) {
        initData(mode, null);
    }

    /**
     * Flexible Initialisierung mit Modus und optionalem Billing-Datum
     *
     * @param mode Initialisierungsmodus
     * @param billingDate Stichtag für Rechnungslauf (optional)
     */
    @Transactional
    public void initData(InitMode mode, LocalDate billingDate) {
        logger.info("===========================================");
        logger.info("Starte Testdaten-Initialisierung");
        logger.info("Modus: {} - {}", mode, mode.getDescription());
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

        // Verträge und Subscriptions
        initContracts();
        initSubscriptions();

        if (mode == InitMode.CONTRACTS) {
            logger.info("Initialisierung bis Verträge abgeschlossen (CONTRACTS Mode)");
            logCurrentDataStatus();
            return;
        }

        // Fälligkeitspläne
        initDueSchedules();

        if (mode == InitMode.SCHEDULES) {
            logger.info("Initialisierung bis Fälligkeitspläne abgeschlossen (SCHEDULES Mode)");
            logCurrentDataStatus();
            return;
        }

        // Manuelle Sample-Rechnungen
        if (mode == InitMode.INVOICES_MANUAL || mode == InitMode.FULL) {
            createSampleInvoices();
            createSampleOpenItems();
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

    @Transactional
    private void initUser() {
        userDetailsService.createUserSafe("a","a", Role.ROLE_ADMIN);
    }

    /**
     * Convenience-Methode: Initialisierung bis DueSchedules
     */
    @Transactional
    public void initUpToSchedules() {
        initData(InitMode.SCHEDULES);
    }

    /**
     * Convenience-Methode: Komplette Initialisierung mit Rechnungslauf heute
     */
    @Transactional
    public void initWithBillingToday() {
        initData(InitMode.FULL_WITH_BILLING, LocalDate.now());
    }

    /**
     * Convenience-Methode: Komplette Initialisierung mit Rechnungslauf zum Stichtag
     */
    @Transactional
    public void initWithBilling(LocalDate billingDate) {
        initData(InitMode.FULL_WITH_BILLING, billingDate);
    }

    // ===============================================================================================
    // Die einzelnen Init-Methoden
    // ===============================================================================================

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

    private void initContracts() {
        if (contractRepository.count() > 0) {
            logger.info("Verträge bereits vorhanden - Überspringe Initialisierung");
            return;
        }

        logger.info("Initialisiere Verträge...");

        List<Customer> customers = customerRepository.findAll();
        if (customers.isEmpty()) {
            throw new IllegalStateException("Keine Kunden gefunden - Kunden müssen zuerst initialisiert werden");
        }

        final int numberOfContracts = 20;
        for (int i = 1; i <= numberOfContracts; i++) {
            Customer customer = customers.get(random.nextInt(customers.size()));
            LocalDate startDate = LocalDate.now().minusDays(random.nextInt(365));

            Contract contract = new Contract("Servicevertrag " + i, startDate, customer);
            contract.setId(null);
            contract.setContractNumber(numberGeneratorService.generateContractNumber());

            if (random.nextDouble() < 0.8) {
                contract.setContractStatus(ContractStatus.ACTIVE);
                if (random.nextDouble() < 0.3) {
                    contract.setEndDate(startDate.plusYears(1 + random.nextInt(2)));
                }
            } else {
                contract.setContractStatus(ContractStatus.TERMINATED);
                contract.setEndDate(startDate.plusDays(random.nextInt(300)));
            }

            contractRepository.save(contract);
        }

        logger.info("✓ {} Verträge erstellt", numberOfContracts);
    }

    // In initSubscriptions() method, remove this line:
    // contract.addSubscription(subscription);

    // The subscription already has the contract reference set, so this line is redundant
    // and causes the LazyInitializationException

    private void initSubscriptions() {
        if (subscriptionRepository.count() > 0) {
            logger.info("Abonnements bereits vorhanden - Überspringe Initialisierung");
            return;
        }

        logger.info("Initialisiere Abonnements...");

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

            if (contract.getContractStatus() == ContractStatus.ACTIVE) {
                subscription.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
            } else {
                subscription.setSubscriptionStatus(SubscriptionStatus.CANCELLED);
                subscription.setEndDate(contract.getEndDate());
            }

            subscription.setAutoRenewal(random.nextBoolean());

            // REMOVE THIS LINE - it causes LazyInitializationException:
            // contract.addSubscription(subscription);

            subscriptionRepository.save(subscription);
        }

        logger.info("✓ {} Abonnements erstellt", numberOfSubscriptions);
    }

    private void initDueSchedules() {
        if (dueScheduleRepository.count() > 0) {
            logger.info("Fälligkeitspläne bereits vorhanden - Überspringe Initialisierung");
            return;
        }

        logger.info("Initialisiere Fälligkeitspläne (nur Termine)...");

        List<Subscription> activeSubscriptions = subscriptionRepository.findBySubscriptionStatus(SubscriptionStatus.ACTIVE);
        if (activeSubscriptions.isEmpty()) {
            logger.warn("Keine aktiven Abonnements für Fälligkeitspläne gefunden");
            return;
        }

        int totalSchedulesCreated = 0;

        for (Subscription subscription : activeSubscriptions) {
            LocalDate subscriptionStart = subscription.getStartDate();

            // 12 Monate ab Abonnement-Start - nur Terminpläne
            for (int month = 0; month < 12; month++) {
                LocalDate periodStart = subscriptionStart.plusMonths(month);
                LocalDate periodEnd = periodStart.plusMonths(1).minusDays(1);
                LocalDate dueDate = periodStart;

                DueSchedule dueSchedule = new DueSchedule(dueDate, periodStart, periodEnd, subscription);
                dueSchedule.setDueNumber(numberGeneratorService.generateDueNumber());

                // Nur Status setzen - keine Beträge oder Rechnungsreferenzen!
                if (dueDate.isBefore(LocalDate.now())) {
                    // Vergangene Fälligkeiten: 70% completed, 30% active
                    if (random.nextDouble() < 0.7) {
                        dueSchedule.markAsCompleted();
                    } else {
                        // Bleibt ACTIVE (überfällig)
                    }
                } else {
                    // Zukünftige Fälligkeiten: meistens aktiv, wenige pausiert
                    if (random.nextDouble() < 0.9) {
                        // Bleibt ACTIVE
                    } else {
                        dueSchedule.pause();
                    }
                }

                dueScheduleRepository.save(dueSchedule);
                totalSchedulesCreated++;
            }
        }

        logger.info("✓ {} Fälligkeitspläne erstellt (nur Termine)", totalSchedulesCreated);
    }

    // ===============================================================================================
    // SAMPLE RECHNUNGEN MIT GARANTIERTEN OPENITEMS
    // ===============================================================================================

    private void createSampleInvoices() {
        if (invoiceRepository.count() > 0) {
            logger.info("Rechnungen bereits vorhanden - Überspringe Erstellung");
            return;
        }

        logger.info("Erstelle Sample-Rechnungen...");

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
                    // Finde Subscription des Kunden
                    List<Subscription> customerSubscriptions = subscriptions.stream()
                            .filter(sub -> sub.getContract().getCustomer().getId().equals(customer.getId()))
                            .toList();

                    if (!customerSubscriptions.isEmpty()) {
                        invoice.setSubscription(customerSubscriptions.get(random.nextInt(customerSubscriptions.size())));
                    }
                }

                // Zufälligen Status setzen
                Invoice.InvoiceStatus[] statuses = {
                        Invoice.InvoiceStatus.DRAFT,
                        Invoice.InvoiceStatus.SENT,
                        Invoice.InvoiceStatus.CANCELLED
                };
                invoice.setStatus(statuses[random.nextInt(statuses.length)]);

                // InvoiceItem hinzufügen
                InvoiceItem item = new InvoiceItem();
                item.setDescription("Sample-Position: " + product.getName());
                item.setQuantity(BigDecimal.ONE);
                item.setUnitPrice(product.getPrice());
                item.setTaxRate(BigDecimal.valueOf(19));

                // Product-Referenzen setzen
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

                // GARANTIERT: OpenItem für jede Rechnung erstellen (außer CANCELLED)
                if (savedInvoice.getStatus() != Invoice.InvoiceStatus.CANCELLED &&
                        savedInvoice.getTotalAmount() != null &&
                        savedInvoice.getTotalAmount().compareTo(BigDecimal.ZERO) > 0) {

                    try {
                        OpenItem openItem = new OpenItem(savedInvoice,
                                "Sample-OpenItem für Rechnung " + savedInvoice.getInvoiceNumber(),
                                savedInvoice.getTotalAmount(),
                                savedInvoice.getDueDate());

                        // Simuliere verschiedene Zahlungsstatus
                        double paymentChance = random.nextDouble();
                        if (paymentChance < 0.3) {
                            // 30% vollständig bezahlt
                            openItem.recordPayment(savedInvoice.getTotalAmount(),
                                    "Überweisung",
                                    "SAMPLE-REF-" + System.currentTimeMillis());
                        } else if (paymentChance < 0.5) {
                            // 20% teilweise bezahlt
                            BigDecimal partialAmount = savedInvoice.getTotalAmount()
                                    .multiply(BigDecimal.valueOf(0.3 + random.nextDouble() * 0.4));
                            openItem.recordPayment(partialAmount,
                                    "Teilzahlung",
                                    "SAMPLE-PARTIAL-" + System.currentTimeMillis());
                        }
                        // 50% bleiben offen

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

        logger.info("✓ {} Sample-Rechnungen erstellt", invoicesCreated);
        logger.info("✓ {} Sample-OpenItems erstellt", openItemsCreated);
    }

    /**
     * Erstellt Sample-OpenItems für bestehende Rechnungen
     * Diese Methode wird nur aufgerufen wenn createSampleInvoices() nicht läuft
     */
    private void createSampleOpenItems() {
        logger.info("Erstelle zusätzliche Sample-OpenItems...");

        List<Invoice> invoicesWithoutOpenItems = invoiceRepository.findInvoicesWithoutOpenItems();


        if (invoicesWithoutOpenItems.isEmpty()) {
            logger.warn("Keine Rechnungen für zusätzliche OpenItems gefunden");
            return;
        }

        int openItemsCreated = 0;

        // Filter for non-cancelled and positive amounts
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

                // Zahlungsstatus simulieren
                double paymentChance = random.nextDouble();

                if (paymentChance < 0.4) {
                    // 40% vollständig bezahlt
                    openItem.recordPayment(invoice.getTotalAmount(),
                            "Überweisung",
                            "REF-" + System.currentTimeMillis());

                } else if (paymentChance < 0.65) {
                    // 25% teilweise bezahlt
                    BigDecimal partialAmount = invoice.getTotalAmount()
                            .multiply(BigDecimal.valueOf(0.3 + random.nextDouble() * 0.5)); // 30-80%
                    openItem.recordPayment(partialAmount,
                            "Teilzahlung",
                            "REF-" + System.currentTimeMillis());
                }
                // 35% bleiben offen

                // Manchmal Mahnungen hinzufügen (bei überfälligen)
                if (openItem.isOverdue() && random.nextDouble() < 0.5) {
                    int reminders = 1 + random.nextInt(3);
                    for (int r = 0; r < reminders; r++) {
                        openItem.addReminder();
                    }
                }

                openItemRepository.save(openItem);
                openItemsCreated++;

                logger.debug("Zusätzlicher OpenItem erstellt für Rechnung {} mit Betrag {}",
                        invoice.getInvoiceNumber(), invoice.getTotalAmount());

            } catch (Exception e) {
                logger.error("Fehler beim Erstellen eines zusätzlichen OpenItems für Rechnung {}: {}",
                        invoice.getInvoiceNumber(), e.getMessage());
            }
        }

        logger.info("✓ {} zusätzliche Sample-OpenItems erstellt", openItemsCreated);
    }

    // ===============================================================================================
    // AUTOMATISCHER RECHNUNGSLAUF MIT VOLLSTÄNDIGER VALIDIERUNG
    // ===============================================================================================

    /**
     * Führt den automatischen Rechnungslauf durch und stellt sicher,
     * dass JEDE abgerechnete Fälligkeit eine Rechnung UND einen OpenItem erhält
     */
    private void runBillingProcess(LocalDate billingDate) {
        logger.info("===========================================");
        logger.info("Starte automatischen Rechnungslauf");
        logger.info("Stichtag: {}", billingDate);
        logger.info("===========================================");

        try {
            // 1. Status VOR dem Rechnungslauf
            long activeDueSchedulesBefore = dueScheduleRepository.countByStatus(DueStatus.ACTIVE);
            long invoicesBefore = invoiceRepository.count();
            long openItemsBefore = openItemRepository.count();

            logger.info("Status VOR Rechnungslauf:");
            logger.info("  - Aktive Fälligkeiten: {}", activeDueSchedulesBefore);
            logger.info("  - Rechnungen: {}", invoicesBefore);
            logger.info("  - OpenItems: {}", openItemsBefore);

            // 2. Rechnungslauf durchführen
            InvoiceBatchService.InvoiceBatchResult result = invoiceBatchService.runInvoiceBatch(billingDate);

            // 3. Status NACH dem Rechnungslauf
            long activeDueSchedulesAfter = dueScheduleRepository.countByStatus(DueStatus.ACTIVE);
            long completedDueSchedulesAfter = dueScheduleRepository.countByStatus(DueStatus.COMPLETED);
            long invoicesAfter = invoiceRepository.count();
            long openItemsAfter = openItemRepository.count();

            logger.info("Status NACH Rechnungslauf:");
            logger.info("  - Aktive Fälligkeiten: {} (war: {})", activeDueSchedulesAfter, activeDueSchedulesBefore);
            logger.info("  - Abgerechnete Fälligkeiten: {}", completedDueSchedulesAfter);
            logger.info("  - Rechnungen: {} (war: {}, neu: {})", invoicesAfter, invoicesBefore, invoicesAfter - invoicesBefore);
            logger.info("  - OpenItems: {} (war: {}, neu: {})", openItemsAfter, openItemsBefore, openItemsAfter - openItemsBefore);

            // 4. Validierung der Konsistenz
            long processedDueSchedules = activeDueSchedulesBefore - activeDueSchedulesAfter;
            long newInvoices = invoicesAfter - invoicesBefore;
            long newOpenItems = openItemsAfter - openItemsBefore;

            logger.info("Rechnungslauf-Ergebnis:");
            logger.info("  - Verarbeitete Fälligkeiten: {} (erwartet: {})", processedDueSchedules, result.getProcessedDueSchedules());
            logger.info("  - Erstellte Rechnungen: {} (erwartet: {})", newInvoices, result.getCreatedInvoices());
            logger.info("  - Neue OpenItems: {}", newOpenItems);
            logger.info("  - Gesamtbetrag: {} EUR", result.getTotalAmount());
            logger.info("  - Status: {}", result.getMessage());

            // 5. Konsistenz-Prüfung
            boolean consistent = (processedDueSchedules == newInvoices) && (newInvoices == newOpenItems);

            if (consistent) {
                logger.info("✓ KONSISTENZ-PRÜFUNG ERFOLGREICH: Alle Zahlen stimmen überein!");
            } else {
                logger.error("✗ KONSISTENZ-FEHLER!");
                logger.error("  Verarbeitete Fälligkeiten: {} | Neue Rechnungen: {} | Neue OpenItems: {}",
                        processedDueSchedules, newInvoices, newOpenItems);

                // Versuche fehlende OpenItems zu erstellen
                logger.info("Versuche fehlende OpenItems automatisch zu erstellen...");
                createMissingOpenItems();
            }

        } catch (Exception e) {
            logger.error("KRITISCHER FEHLER beim Rechnungslauf: {}", e.getMessage(), e);
            throw new RuntimeException("Rechnungslauf fehlgeschlagen", e);
        }
    }

    /**
     * Erstellt fehlende OpenItems für Rechnungen ohne OpenItems
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

    // ===============================================================================================
    // DATENBESTAND UND KONSISTENZ-MANAGEMENT
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
        logger.info("  - Verträge: {}", contractRepository.count());
        logger.info("  - Abonnements: {}", subscriptionRepository.count());

        logger.info("Fälligkeiten:");
        long activeSchedules = dueScheduleRepository.countByStatus(DueStatus.ACTIVE);
        long pausedSchedules = dueScheduleRepository.countByStatus(DueStatus.PAUSED);
        long completedSchedules = dueScheduleRepository.countByStatus(DueStatus.COMPLETED);
        logger.info("  - Gesamt: {}", dueScheduleRepository.count());
        logger.info("  - Aktiv: {}", activeSchedules);
        logger.info("  - Pausiert: {}", pausedSchedules);
        logger.info("  - Abgerechnet: {}", completedSchedules);

        logger.info("Rechnungen:");
        long totalInvoices = invoiceRepository.count();
        long draftInvoices = invoiceService.getInvoiceCountByStatus(Invoice.InvoiceStatus.DRAFT);
        long sentInvoices = invoiceService.getInvoiceCountByStatus(Invoice.InvoiceStatus.SENT);
        long cancelledInvoices = invoiceService.getInvoiceCountByStatus(Invoice.InvoiceStatus.CANCELLED);
        logger.info("  - Gesamt: {}", totalInvoices);
        logger.info("  - Entwürfe: {}", draftInvoices);
        logger.info("  - Versendet: {}", sentInvoices);
        logger.info("  - Storniert: {}", cancelledInvoices);

        logger.info("Offene Posten:");
        long totalOpenItems = openItemRepository.count();
        long openOpenItems = openItemRepository.countByStatus(OpenItem.OpenItemStatus.OPEN);
        long partiallyPaidOpenItems = openItemRepository.countByStatus(OpenItem.OpenItemStatus.PARTIALLY_PAID);
        long paidOpenItems = openItemRepository.countByStatus(OpenItem.OpenItemStatus.PAID);
        long overdueOpenItems = openItemRepository.countByStatus(OpenItem.OpenItemStatus.OVERDUE);
        long cancelledOpenItems = openItemRepository.countByStatus(OpenItem.OpenItemStatus.CANCELLED);

        logger.info("  - Gesamt: {}", totalOpenItems);
        logger.info("  - Offen: {}", openOpenItems);
        logger.info("  - Teilweise bezahlt: {}", partiallyPaidOpenItems);
        logger.info("  - Bezahlt: {}", paidOpenItems);
        logger.info("  - Überfällig: {}", overdueOpenItems);
        logger.info("  - Storniert: {}", cancelledOpenItems);

        // Konsistenz-Prüfung - FIXED VERSION
        logger.info("Konsistenz-Prüfung:");
        long invoicesWithoutOpenItems = 0;
        long openItemsWithoutInvoices = 0;

        try {
            // OLD PROBLEMATIC CODE:
            // invoicesWithoutOpenItems = invoiceRepository.findAll().stream()
            //     .filter(invoice -> invoice.getStatus() != Invoice.InvoiceStatus.CANCELLED)
            //     .filter(invoice -> invoice.getTotalAmount() != null && invoice.getTotalAmount().compareTo(BigDecimal.ZERO) > 0)
            //     .filter(invoice -> invoice.getOpenItems().isEmpty()) // <-- LazyInitializationException hier!
            //     .count();

            // NEW FIXED CODE - use repository queries instead:
            invoicesWithoutOpenItems = countInvoicesWithoutOpenItems();

            openItemsWithoutInvoices = openItemRepository.findAll().stream()
                    .filter(openItem -> openItem.getInvoice() == null)
                    .count();

            logger.info("  - Rechnungen ohne OpenItems: {}", invoicesWithoutOpenItems);
            logger.info("  - OpenItems ohne Rechnungen: {}", openItemsWithoutInvoices);

            if (invoicesWithoutOpenItems == 0 && openItemsWithoutInvoices == 0) {
                logger.info("  ✓ Konsistenz-Prüfung erfolgreich!");
            } else {
                logger.warn("  ⚠ Konsistenz-Probleme gefunden!");
            }

        } catch (Exception e) {
            logger.error("Fehler bei Konsistenz-Prüfung: {}", e.getMessage());
        }

        logger.info("===========================================");
    }

    // Helper method for consistency check
    private long countInvoicesWithoutOpenItems() {
        try {
            // Use repository query to avoid lazy loading issues
            List<Invoice> invoicesWithoutOpenItems = invoiceRepository.findInvoicesWithoutOpenItems();

            return invoicesWithoutOpenItems.stream()
                    .filter(invoice -> invoice.getStatus() != Invoice.InvoiceStatus.CANCELLED)
                    .filter(invoice -> invoice.getTotalAmount() != null && invoice.getTotalAmount().compareTo(BigDecimal.ZERO) > 0)
                    .count();

        } catch (Exception e) {
            logger.error("Fehler beim Zählen der Rechnungen ohne OpenItems: {}", e.getMessage());
            return -1; // Indicator für Fehler
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

    /**
     * Repariert inkonsistente Daten
     */
    @Transactional
    public void repairDataConsistency() {
        logger.info("Starte Daten-Konsistenz-Reparatur...");

        try {
            // 1. Erstelle fehlende OpenItems
            createMissingOpenItems();

            // 2. Entferne OpenItems ohne gültige Rechnungen
            cleanupOrphanedOpenItems();

            // 3. Update überfällige OpenItems
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