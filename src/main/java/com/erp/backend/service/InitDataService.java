package com.erp.backend.service;

import com.erp.backend.domain.*;
        import com.erp.backend.repository.*;
        import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class InitDataService {

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
                           NumberGeneratorService numberGeneratorService) {
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

    /**
     * Führt den automatischen Rechnungslauf durch
     */
    private void runBillingProcess(LocalDate billingDate) {
        logger.info("===========================================");
        logger.info("Starte automatischen Rechnungslauf");
        logger.info("Stichtag: {}", billingDate);
        logger.info("===========================================");

        try {
            // Rechnungslauf durchführen
            InvoiceBatchService.InvoiceBatchResult result =
                    invoiceBatchService.runInvoiceBatch(billingDate);

            logger.info("Rechnungslauf-Ergebnis:");
            logger.info("  - Erstellte Rechnungen: {}", result.getCreatedInvoices());
            logger.info("  - Verarbeitete Fälligkeiten: {}", result.getProcessedDueSchedules());
            logger.info("  - Gesamtbetrag: {} EUR", result.getTotalAmount());
            logger.info("  - Status: {}", result.getMessage());

        } catch (Exception e) {
            logger.error("Fehler beim Rechnungslauf: {}", e.getMessage(), e);
        }
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
    // Die einzelnen Init-Methoden bleiben unverändert
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

        logger.info("Es wurden {} Produkte erstellt", products.length);
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

        logger.info("Es wurden {} Verträge erstellt", numberOfContracts);
    }

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

            contract.addSubscription(subscription);
            subscriptionRepository.save(subscription);
        }

        logger.info("Es wurden {} Abonnements erstellt", numberOfSubscriptions);
    }

    // ===============================================================================================
    // SCHRITT 6: FÄLLIGKEITSPLÄNE (NUR TERMINE, KEINE BETRÄGE ODER RECHNUNGS-REFERENZEN)
    // ===============================================================================================

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
            LocalDate currentDate = subscription.getStartDate();

            // 12 Monate ab Abonnement-Start - nur Terminpläne
            for (int month = 0; month < 12; month++) {
                LocalDate periodStart = currentDate;
                LocalDate periodEnd = currentDate.plusMonths(1).minusDays(1);
                LocalDate dueDate = periodEnd;

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
                currentDate = currentDate.plusMonths(1);
            }
        }

        logger.info("Es wurden {} Fälligkeitspläne erstellt (nur Termine)", totalSchedulesCreated);
    }

    // ===============================================================================================
    // SCHRITT 7: SAMPLE RECHNUNGEN (UNABHÄNGIG VON DUESCHEDULES)
    // ===============================================================================================

    private void createSampleInvoices() {
        if (invoiceRepository.count() > 0) {
            logger.info("Rechnungen bereits vorhanden - Überspringe Erstellung");
            return;
        }

        logger.info("Erstelle Sample-Rechnungen...");

        List<Customer> customers = customerRepository.findAll();
        List<Product> products = productRepository.findAll();

        if (customers.isEmpty() || products.isEmpty()) {
            logger.warn("Keine Kunden oder Produkte für Rechnungen gefunden");
            return;
        }

        final int numberOfInvoices = 30;
        int invoicesCreated = 0;

        for (int i = 1; i <= numberOfInvoices; i++) {
            try {
                Customer customer = customers.get(random.nextInt(customers.size()));
                Product product = products.get(random.nextInt(products.size()));

                Invoice invoice = new Invoice();
                invoice.setCustomer(customer);
                invoice.setBillingAddress(customer.getBillingAddress());
                invoice.setInvoiceDate(LocalDate.now().minusDays(random.nextInt(90)));
                invoice.setDueDate(invoice.getInvoiceDate().plusDays(14 + random.nextInt(16)));

                // Zufälligen Status setzen
                Invoice.InvoiceStatus[] statuses = {
                        Invoice.InvoiceStatus.DRAFT,
                        Invoice.InvoiceStatus.SENT,
                        Invoice.InvoiceStatus.CANCELLED
                };
                invoice.setStatus(statuses[random.nextInt(statuses.length)]);

                // InvoiceItem hinzufügen
                InvoiceItem item = new InvoiceItem();
                item.setDescription("Testposition: " + product.getName());
                item.setQuantity(BigDecimal.ONE);
                item.setUnitPrice(product.getPrice());
                item.setTaxRate(BigDecimal.valueOf(19));

                invoice.addInvoiceItem(item);

                // Manchmal zweite Position hinzufügen
                if (random.nextDouble() < 0.3) {
                    Product secondProduct = products.get(random.nextInt(products.size()));
                    InvoiceItem secondItem = new InvoiceItem();
                    secondItem.setDescription("Zusätzliche Position: " + secondProduct.getName());
                    secondItem.setQuantity(BigDecimal.ONE);
                    secondItem.setUnitPrice(secondProduct.getPrice());
                    secondItem.setTaxRate(BigDecimal.valueOf(19));

                    invoice.addInvoiceItem(secondItem);
                }

                Invoice savedInvoice = invoiceService.createInvoice(invoice);
                invoicesCreated++;

            } catch (Exception e) {
                logger.error("Fehler beim Erstellen einer Sample-Rechnung: {}", e.getMessage());
            }
        }

        logger.info("Es wurden {} Sample-Rechnungen erstellt", invoicesCreated);
    }

    // ===============================================================================================
    // SCHRITT 8: SAMPLE OPENITEMS (UNABHÄNGIG VON RECHNUNGEN)
    // ===============================================================================================

    private void createSampleOpenItems() {
        logger.info("Erstelle Sample-OpenItems...");

        List<Invoice> allInvoices = invoiceService.getAllInvoices();

        if (allInvoices.isEmpty()) {
            logger.warn("Keine Rechnungen für OpenItems gefunden");
            return;
        }

        int openItemsCreated = 0;

        for (Invoice invoice : allInvoices) {
            // Nur Rechnungen mit Betrag > 0 verwenden
            if (invoice.getTotalAmount() == null || invoice.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
                logger.debug("Überspringe Rechnung {} - kein gültiger Betrag", invoice.getInvoiceNumber());
                continue;
            }

            // 90% aller gültigen Rechnungen bekommen OpenItems (nicht nur versendete)
            if (random.nextDouble() < 0.9) {
                try {
                    OpenItem openItem = new OpenItem(invoice,
                            "Ausstehender Betrag für Rechnung " + invoice.getInvoiceNumber(),
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

                    logger.debug("OpenItem erstellt für Rechnung {} mit Betrag {}",
                            invoice.getInvoiceNumber(), invoice.getTotalAmount());

                } catch (Exception e) {
                    logger.error("Fehler beim Erstellen eines OpenItems für Rechnung {}: {}",
                            invoice.getInvoiceNumber(), e.getMessage());
                }
            }
        }

        logger.info("Es wurden {} Sample-OpenItems erstellt", openItemsCreated);
    }

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
        logger.info("  - Gesamt: {}", totalInvoices);
        logger.info("  - Entwürfe: {}", draftInvoices);
        logger.info("  - Versendet: {}", sentInvoices);

        logger.info("Offene Posten:");
        long totalOpenItems = openItemRepository.count();
        long openOpenItems = openItemRepository.countByStatus(OpenItem.OpenItemStatus.OPEN);
        long paidOpenItems = openItemRepository.countByStatus(OpenItem.OpenItemStatus.PAID);
        logger.info("  - Gesamt: {}", totalOpenItems);
        logger.info("  - Offen: {}", openOpenItems);
        logger.info("  - Bezahlt: {}", paidOpenItems);
        logger.info("===========================================");
    }

    @Transactional
    public void clearAllTestData() {
        logger.warn("WARNUNG: Lösche alle Testdaten...");

        openItemRepository.deleteAll();
        invoiceRepository.deleteAll();
        dueScheduleRepository.deleteAll();
        subscriptionRepository.deleteAll();
        contractRepository.deleteAll();
        productRepository.deleteAll();
        customerRepository.deleteAll();
        addressRepository.deleteAll();

        logger.info("Alle Testdaten wurden gelöscht.");
    }
}