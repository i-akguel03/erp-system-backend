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
 * Korrigierte Architektur:
 * - DueSchedule = nur Terminplan (Datum, Status), keine Beträge
 * - Subscription = enthält Produkt und Preise
 * - Invoice = wird beim Rechnungslauf aus DueSchedule + Subscription erzeugt
 * - OpenItem = offene Posten aus Rechnungen
 */
@Service
public class InitDataService {

    private static final Logger logger = LoggerFactory.getLogger(InitDataService.class);

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
    private final NumberGeneratorService numberGeneratorService;

    private final Random random = new Random();

    public InitDataService(AddressRepository addressRepository,
                           CustomerRepository customerRepository,
                           ProductRepository productRepository,
                           ContractRepository contractRepository,
                           SubscriptionRepository subscriptionRepository,
                           DueScheduleRepository dueScheduleRepository,
                           OpenItemRepository openItemRepository, InvoiceRepository invoiceRepository,
                           InvoiceService invoiceService,
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
        this.numberGeneratorService = numberGeneratorService;
    }

    /**
     * Hauptmethode zur Initialisierung aller Testdaten.
     */
    @Transactional
    public void initAllData() {
        logger.info("Starte Initialisierung der Testdaten...");

        initAddresses();
        initCustomers();
        initProducts();
        initContracts();
        initSubscriptions();
        initDueSchedules();      // Nur Terminpläne ohne Beträge
        processInvoiceRun();     // Rechnungslauf: DueSchedules -> Invoices

        logger.info("Testdateninitialisierung erfolgreich abgeschlossen.");
    }

    // ===============================================================================================
    // SCHRITT 1-4: Basisdaten (unverändert)
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

        logger.info("Es wurden {} Adressen erstellt", addressData.length);
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

        logger.info("Es wurden {} Kunden erstellt", numberOfCustomers);
    }

    private void initProducts() {
        if (productRepository.count() > 0) {
            logger.info("Produkte bereits vorhanden - Überspringe Initialisierung");
            return;
        }

        logger.info("Initialisiere Produktkatalog...");

        Object[][] products = {
                // Hardware-Produkte (einmalige Käufe)
                {"Laptop Dell XPS 13", 1200.0, "Stück", "Hardware"},
                {"MacBook Pro 14\"", 2200.0, "Stück", "Hardware"},
                {"Samsung Galaxy S23", 900.0, "Stück", "Hardware"},
                {"iPhone 15 Pro", 1300.0, "Stück", "Hardware"},
                {"iPad Air", 650.0, "Stück", "Hardware"},
                {"Surface Pro 9", 1100.0, "Stück", "Hardware"},

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
                {"Docker Pro", 9.00, "Monat", "Cloud"}
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

            // Subscription enthält Produktreferenz und Preis
            Subscription subscription = new Subscription(
                    product.getName(),
                    product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO,
                    subscriptionStart,
                    contract
            );

            // Produktreferenz setzen (wichtig für Rechnungslauf)
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
    // SCHRITT 6: FÄLLIGKEITSPLÄNE (NUR TERMINE, KEINE BETRÄGE)
    // ===============================================================================================

    /**
     * Erstellt Fälligkeitspläne nur mit Terminen und Status.
     * Keine Beträge - die kommen erst beim Rechnungslauf aus den Subscriptions.
     */
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

                // DueSchedule enthält NUR Termine und Status
                DueSchedule dueSchedule = new DueSchedule();
                dueSchedule.setDueNumber(numberGeneratorService.generateDueNumber());
                dueSchedule.setDueDate(dueDate);
                dueSchedule.setPeriodStart(periodStart);
                dueSchedule.setPeriodEnd(periodEnd);
                dueSchedule.setSubscription(subscription);

                // Nur Status setzen - keine Beträge!
                // DueStatus aus dem korrigierten Service verwenden
                if (dueDate.isBefore(LocalDate.now())) {
                    // Vergangene Fälligkeiten: 60% completed, 40% active (= überfällig)
                    if (random.nextDouble() < 0.6) {
                        dueSchedule.setStatus(DueStatus.COMPLETED); // Wurde bereits abgerechnet
                    } else {
                        dueSchedule.setStatus(DueStatus.ACTIVE); // Überfällig
                    }
                } else {
                    // Zukünftige Fälligkeiten: meistens aktiv, wenige pausiert
                    if (random.nextDouble() < 0.9) {
                        dueSchedule.setStatus(DueStatus.ACTIVE);
                    } else {
                        dueSchedule.setStatus(DueStatus.PAUSED);
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
    // SCHRITT 7: RECHNUNGSLAUF (AUS DUESCHEDULES + SUBSCRIPTIONS)
    // ===============================================================================================

    /**
     * Simuliert einen Rechnungslauf: Für abgerechnete DueSchedules werden Invoices erstellt.
     * Beträge kommen aus den Subscriptions.
     */
    private void processInvoiceRun() {
        logger.info("Starte Rechnungslauf...");

        // Alle DueSchedules die als COMPLETED markiert sind (= bereits abgerechnet)
        List<DueSchedule> completedSchedules = dueScheduleRepository.findByStatus(DueStatus.COMPLETED);

        int invoicesCreated = 0;
        int openItemsCreated = 0;

        for (DueSchedule dueSchedule : completedSchedules) {
            try {
                // Prüfen ob bereits eine Rechnung existiert
                if (dueSchedule.getInvoice() != null) {
                    continue; // Bereits abgerechnet
                }

                // Rechnung aus DueSchedule + Subscription erstellen
                Invoice invoice = createInvoiceFromDueScheduleAndSubscription(dueSchedule);
                invoicesCreated++;

                // Zahlungsverhalten simulieren
                double paymentRandom = random.nextDouble();

                if (paymentRandom < 0.7) {
                    // 70% vollständig bezahlt
                    invoice.setStatus(Invoice.InvoiceStatus.PAID);

                } else if (paymentRandom < 0.9) {
                    // 20% teilweise bezahlt - OpenItem erstellen
                    invoice.setStatus(Invoice.InvoiceStatus.PARTIALLY_PAID);
                    createPartialPaymentOpenItem(invoice);
                    openItemsCreated++;

                } else {
                    // 10% unbezahlt - OpenItem für Vollbetrag
                    invoice.setStatus(Invoice.InvoiceStatus.OPEN);
                    createFullOpenItem(invoice);
                    openItemsCreated++;
                }

                invoiceRepository.save(invoice);

                // DueSchedule als abgerechnet markieren
                dueSchedule.setInvoice(invoice);
                dueSchedule.setProcessedForInvoicing(true);
                dueScheduleRepository.save(dueSchedule);

            } catch (Exception e) {
                logger.error("Fehler beim Verarbeiten von DueSchedule {}: {}",
                        dueSchedule.getDueNumber(), e.getMessage());
            }
        }

        logger.info("Rechnungslauf abgeschlossen: {} Rechnungen erstellt, {} offene Posten",
                invoicesCreated, openItemsCreated);
    }

    /**
     * Erstellt eine Rechnung aus DueSchedule und holt Preise aus Subscription.
     */
    private Invoice createInvoiceFromDueScheduleAndSubscription(DueSchedule dueSchedule) {
        Subscription subscription = dueSchedule.getSubscription();
        Customer customer = subscription.getContract().getCustomer();

        // Preis aus Subscription holen
        BigDecimal price = subscription.getMonthlyPrice() != null ?
                subscription.getMonthlyPrice() : BigDecimal.ZERO;

        // Rechnung erstellen
        Invoice invoice = new Invoice();
        invoice.setCustomer(customer);
        invoice.setBillingAddress(customer.getBillingAddress());
        invoice.setInvoiceDate(dueSchedule.getDueDate().minusDays(5)); // 5 Tage vor Fälligkeit
        invoice.setDueDate(dueSchedule.getDueDate());
        invoice.setInvoiceNumber(generateInvoiceNumber());
        invoice.setStatus(Invoice.InvoiceStatus.DRAFT);

        // Invoice Item mit Preis aus Subscription
        InvoiceItem item = new InvoiceItem();
        item.setInvoice(invoice);
        item.setDescription(
                "Abonnement: " + subscription.getProductName() +
                        " | Periode: " + dueSchedule.getPeriodStart() + " bis " + dueSchedule.getPeriodEnd()
        );
        item.setQuantity(BigDecimal.ONE);
        item.setUnitPrice(price);
        item.setTaxRate(BigDecimal.valueOf(19));

        invoice.addInvoiceItem(item);
        invoice.calculateTotals();

        return invoice;
    }

    /**
     * Erstellt OpenItem für teilweise bezahlte Rechnung
     */
    private void createPartialPaymentOpenItem(Invoice invoice) {
        BigDecimal totalAmount = invoice.getTotalAmount();
        BigDecimal paidAmount = totalAmount.multiply(BigDecimal.valueOf(0.6 + random.nextDouble() * 0.3)); // 60-90%
        BigDecimal openAmount = totalAmount.subtract(paidAmount);

        OpenItem openItem = new OpenItem();
        openItem.setInvoice(invoice);
        openItem.setAmount(openAmount);
        openItem.setDescription("Ausstehender Betrag nach Teilzahlung");
        openItem.setDueDate(invoice.getDueDate().plusDays(14));
        openItem.setStatus(OpenItem.OpenItemStatus.OPEN);

        openItemRepository.save(openItem);
    }

    /**
     * Erstellt OpenItem für unbezahlte Rechnung
     */
    private void createFullOpenItem(Invoice invoice) {
        OpenItem openItem = new OpenItem();
        openItem.setInvoice(invoice);
        openItem.setAmount(invoice.getTotalAmount());
        openItem.setDescription("Vollständig offener Rechnungsbetrag");
        openItem.setDueDate(invoice.getDueDate().plusDays(7));
        openItem.setStatus(OpenItem.OpenItemStatus.OPEN);

        openItemRepository.save(openItem);
    }

    // ===============================================================================================
    // HILFSMETHODEN
    // ===============================================================================================

    private String generateInvoiceNumber() {
        return String.format("INV-%d-%04d",
                LocalDate.now().getYear(),
                random.nextInt(9999) + 1);
    }

    @Transactional(readOnly = true)
    public void logCurrentDataStatus() {
        logger.info("=== AKTUELLER DATENBESTAND ===");
        logger.info("Adressen: {}", addressRepository.count());
        logger.info("Kunden: {}", customerRepository.count());
        logger.info("Produkte: {}", productRepository.count());
        logger.info("Verträge: {}", contractRepository.count());
        logger.info("Abonnements: {}", subscriptionRepository.count());
        logger.info("Fälligkeitspläne: {}", dueScheduleRepository.count());

        long activeSchedules = dueScheduleRepository.countByStatus(DueStatus.ACTIVE);
        long pausedSchedules = dueScheduleRepository.countByStatus(DueStatus.PAUSED);
        long completedSchedules = dueScheduleRepository.countByStatus(DueStatus.COMPLETED);

        logger.info("  - Aktive Fälligkeiten: {}", activeSchedules);
        logger.info("  - Pausierte Fälligkeiten: {}", pausedSchedules);
        logger.info("  - Abgerechnete Fälligkeiten: {}", completedSchedules);

        long totalInvoices = invoiceService.getAllInvoices().size();
        long openItems = openItemRepository.count();
        long openOpenItems = openItemRepository.countByStatus(OpenItem.OpenItemStatus.OPEN);

        logger.info("Rechnungen: {}", totalInvoices);
        logger.info("Offene Posten: {} (davon offen: {})", openItems, openOpenItems);
        logger.info("==============================");
    }

    @Transactional
    public void clearAllTestData() {
        logger.warn("WARNUNG: Lösche alle Testdaten...");

        openItemRepository.deleteAll();
        invoiceService.getAllInvoices().forEach(invoice -> {
            try {
                invoiceService.deleteInvoice(invoice.getId());
            } catch (Exception e) {
                logger.warn("Fehler beim Löschen der Rechnung {}: {}", invoice.getId(), e.getMessage());
            }
        });
        dueScheduleRepository.deleteAll();
        subscriptionRepository.deleteAll();
        contractRepository.deleteAll();
        productRepository.deleteAll();
        customerRepository.deleteAll();
        addressRepository.deleteAll();

        logger.info("Alle Testdaten wurden gelöscht.");
    }
}