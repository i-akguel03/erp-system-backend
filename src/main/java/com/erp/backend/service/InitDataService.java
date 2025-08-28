package com.erp.backend.service;

import com.erp.backend.domain.*;
import com.erp.backend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Random;

@Service
public class InitDataService {

    private final AddressRepository addressRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final ContractRepository contractRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final DueScheduleRepository dueScheduleRepository;
    private final InvoiceService invoiceService;
    private final NumberGeneratorService numberGeneratorService;

    public InitDataService(AddressRepository addressRepository,
                           CustomerRepository customerRepository,
                           ProductRepository productRepository,
                           ContractRepository contractRepository,
                           SubscriptionRepository subscriptionRepository,
                           DueScheduleRepository dueScheduleRepository,
                           InvoiceService invoiceService,
                           NumberGeneratorService numberGeneratorService) {
        this.addressRepository = addressRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.contractRepository = contractRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.dueScheduleRepository = dueScheduleRepository;
        this.invoiceService = invoiceService;
        this.numberGeneratorService = numberGeneratorService;
    }

    @Transactional
    public void initAllData() {
        initAddresses();
        initCustomers();
        initProducts();
        initContracts();
        initSubscriptions();
        initDueSchedules();
        initInvoices();
    }

    // --- 1. Adressen ---
    private void initAddresses() {
        if(addressRepository.count() > 0) return;

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
                {"Kastanienallee 7", "24103", "Kiel", "Germany"},
                {"Brunnenstraße 11", "66111", "Saarbrücken", "Germany"},
                {"Königstraße 19", "70173", "Stuttgart", "Germany"},
                {"Parkweg 2", "18055", "Rostock", "Germany"},
                {"Wilhelmstraße 30", "90459", "Nürnberg", "Germany"},
                {"Mozartstraße 25", "60313", "Frankfurt", "Germany"},
                {"Beethovenstraße 14", "50674", "Köln", "Germany"},
                {"Talstraße 8", "80331", "München", "Germany"},
                {"Am Stadtpark 6", "24103", "Kiel", "Germany"},
                {"Kirchweg 10", "99084", "Erfurt", "Germany"},
                {"Lindenstraße 5", "01069", "Dresden", "Germany"},
                {"Brückenstraße 3", "28195", "Bremen", "Germany"},
                {"Kaiserstraße 9", "65185", "Wiesbaden", "Germany"},
                {"Schwanenweg 12", "97070", "Würzburg", "Germany"},
                {"Wallstraße 7", "66111", "Saarbrücken", "Germany"},
                {"Hindenburgstraße 18", "45127", "Essen", "Germany"},
                {"Uhlandstraße 4", "60316", "Frankfurt", "Germany"},
                {"Seestraße 22", "18055", "Rostock", "Germany"},
                {"Gartenweg 11", "20095", "Hamburg", "Germany"}
        };

        for (String[] data : addressData) {
            Address address = new Address(data[0], data[1], data[2], data[3]);
            addressRepository.save(address);
        }
    }

    // --- 2. Kunden ---
    private void initCustomers() {
        if (customerRepository.count() > 0) return;
        List<Address> addresses = addressRepository.findAll();
        Random random = new Random();
        String[] firstNames = {"Max", "Anna", "Tom", "Laura", "Paul", "Sophie", "Lukas", "Marie", "Felix", "Emma",
                "Jonas", "Lena", "David", "Sarah", "Michael", "Lisa", "Alexander", "Julia", "Daniel", "Nina"};
        String[] lastNames = {"Müller", "Schmidt", "Schneider", "Fischer", "Weber", "Becker", "Hoffmann", "Schäfer",
                "Koch", "Richter", "Klein", "Wolf", "Neumann", "Braun", "Zimmermann", "Krüger", "Meyer",
                "Schulz", "Hartmann", "Wagner"};

        for (int i = 1; i <= 25; i++) {
            Customer customer = new Customer(
                    firstNames[random.nextInt(firstNames.length)],
                    lastNames[random.nextInt(lastNames.length)],
                    "customer" + i + "@test.com",
                    "+49" + (random.nextInt(900000000) + 100000000)
            );
            customer.setCustomerNumber(numberGeneratorService.generateCustomerNumber());
            // Zufällige Adressen aus DB zuweisen
            customer.setBillingAddress(addresses.get(random.nextInt(addresses.size())));
            customer.setShippingAddress(addresses.get(random.nextInt(addresses.size())));
            customer.setResidentialAddress(addresses.get(random.nextInt(addresses.size())));

            customerRepository.save(customer);
        }
    }

    // --- 3. Produkte ---
    private void initProducts() {
        if(productRepository.count() > 0) return;

        Object[][] products = {
                {"Laptop Dell XPS 13", 1200.0, "Stück"},
                {"MacBook Pro 14\"", 2200.0, "Stück"},
                {"Samsung Galaxy S23", 900.0, "Stück"},
                {"iPhone 15 Pro", 1300.0, "Stück"},
                {"Logitech MX Master 3", 100.0, "Stück"},
                {"HP LaserJet Drucker", 250.0, "Stück"},
                {"Adobe Photoshop Lizenz", 29.99, "Monat"},
                {"Microsoft 365 Business", 12.50, "Monat"},
                {"AWS Cloud Hosting", 89.90, "Monat"},
                {"GitHub Copilot", 10.00, "Monat"},
                {"Slack Business+", 8.75, "Monat"},
                {"Zoom Pro", 14.99, "Monat"},
                {"Dropbox Business", 15.00, "Monat"},
                {"Netflix Business", 19.99, "Monat"},
                {"Spotify Premium", 9.99, "Monat"},
                {"Tisch Ikea Bekant", 200.0, "Stück"},
                {"Bürostuhl Herman Miller", 800.0, "Stück"},
                {"Monitor Samsung 27\"", 300.0, "Stück"},
                {"SSD Samsung 2TB", 150.0, "Stück"},
                {"Externe Festplatte 5TB", 120.0, "Stück"}
        };

        for (int i = 0; i < products.length; i++) {
            Product product = new Product(
                    (String) products[i][0],
                    BigDecimal.valueOf((Double) products[i][1]),
                    (String) products[i][2]
            );
            product.setProductNumber("PROD-" + String.format("%03d", i+1));
            productRepository.save(product);
        }
    }

    // --- 4. Verträge ---
    private void initContracts() {
        if (contractRepository.count() > 0) return;
        List<Customer> customers = customerRepository.findAll();
        Random random = new Random();

        for (int i = 1; i <= 20; i++) {
            Customer customer = customers.get(random.nextInt(customers.size()));
            LocalDate startDate = LocalDate.now().minusDays(random.nextInt(365));

            Contract contract = new Contract("Contract " + i, startDate, customer);

            // Eindeutige ID erzwingen
            contract.setId(null);

            // Eindeutige Vertragsnummer generieren
            contract.setContractNumber(numberGeneratorService.generateContractNumber());

            // Status zufällig setzen (80% ACTIVE, 20% TERMINATED)
            if (random.nextDouble() < 0.8) {
                contract.setContractStatus(ContractStatus.ACTIVE);
                if (random.nextDouble() < 0.3) {
                    contract.setEndDate(startDate.plusYears(1 + random.nextInt(3)));
                }
            } else {
                contract.setContractStatus(ContractStatus.TERMINATED);
                contract.setEndDate(startDate.plusDays(random.nextInt(300)));
            }

            contractRepository.save(contract);
        }
    }


    // --- 5. Abonnements ---
    private void initSubscriptions() {
        if (subscriptionRepository.count() > 0) return;
        List<Contract> contracts = contractRepository.findAll();
        List<Product> products = productRepository.findAll();
        Random random = new Random();

        // Nur monatliche/wiederkehrende Produkte für Abonnements verwenden
        List<Product> subscriptionProducts = products.stream()
                .filter(p -> p.getUnit().equals("Monat"))
                .toList();

        for (int i = 1; i <= 40; i++) {
            Contract contract = contracts.get(random.nextInt(contracts.size()));
            Product product = subscriptionProducts.get(random.nextInt(subscriptionProducts.size()));

            LocalDate subscriptionStart = contract.getStartDate().plusDays(random.nextInt(30));

            Subscription sub = new Subscription(product.getName(), product.getPrice(), subscriptionStart, contract);
            sub.setSubscriptionNumber(numberGeneratorService.generateSubscriptionNumber());
            sub.setDescription("Monatliches Abonnement für " + product.getName());

            // Billing Cycle setzen
            BillingCycle[] cycles = BillingCycle.values();
            sub.setBillingCycle(cycles[random.nextInt(cycles.length)]);

            // Subscription Status basierend auf Contract Status
            if (contract.getContractStatus() == ContractStatus.ACTIVE) {
                // 90% aktiv, 10% pausiert/beendet
                if (random.nextDouble() < 0.9) {
                    sub.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
                } else {
                    SubscriptionStatus[] inactiveStatuses = {SubscriptionStatus.PAUSED, SubscriptionStatus.CANCELLED};
                    sub.setSubscriptionStatus(inactiveStatuses[random.nextInt(inactiveStatuses.length)]);
                    sub.setEndDate(subscriptionStart.plusMonths(random.nextInt(12)));
                }
            } else {
                sub.setSubscriptionStatus(SubscriptionStatus.CANCELLED);
                sub.setEndDate(contract.getEndDate());
            }

            sub.setAutoRenewal(random.nextBoolean());

            contract.addSubscription(sub);
            subscriptionRepository.save(sub);
        }
    }

    // --- 6. Fälligkeitspläne ---
    private void initDueSchedules() {
        if (dueScheduleRepository.count() > 0) return;

        List<Subscription> activeSubscriptions = subscriptionRepository.findBySubscriptionStatus(SubscriptionStatus.ACTIVE);
        Random random = new Random();

        for (Subscription subscription : activeSubscriptions) {
            LocalDate currentDate = subscription.getStartDate();
            LocalDate endDate = subscription.getEndDate() != null
                    ? subscription.getEndDate()
                    : LocalDate.now().plusMonths(12); // 12 Monate in die Zukunft für unbefristete

            int monthsToGenerate = random.nextInt(6) + 3; // 3-8 Monate generieren

            for (int i = 0; i < monthsToGenerate && currentDate.isBefore(endDate); i++) {
                LocalDate periodStart = currentDate;
                LocalDate periodEnd = calculatePeriodEnd(currentDate, subscription.getBillingCycle());
                LocalDate dueDate = calculateDueDate(periodEnd, subscription.getBillingCycle());

                // Nicht über das Abo-Enddatum hinaus generieren
                if (dueDate.isAfter(endDate)) {
                    break;
                }

                DueSchedule dueSchedule = new DueSchedule();
                dueSchedule.setDueNumber(numberGeneratorService.generateDueNumber());
                dueSchedule.setDueDate(dueDate);
                dueSchedule.setAmount(subscription.getMonthlyPrice());
                dueSchedule.setPeriodStart(periodStart);
                dueSchedule.setPeriodEnd(periodEnd);
                dueSchedule.setSubscription(subscription);
                dueSchedule.setReminderSent(false);
                dueSchedule.setReminderCount(0);

                // Status basierend auf Fälligkeitsdatum setzen
                LocalDate now = LocalDate.now();
                if (dueDate.isBefore(now.minusDays(30))) {
                    // Alte Fälligkeiten als bezahlt markieren (80% bezahlt, 20% überfällig)
                    if (random.nextDouble() < 0.8) {
                        dueSchedule.setStatus(DueStatus.PAID);
                        dueSchedule.setPaidDate(dueDate.plusDays(random.nextInt(14)));
                        dueSchedule.setPaidAmount(subscription.getMonthlyPrice());
                        dueSchedule.setPaymentMethod(getRandomPaymentMethod(random));
                        dueSchedule.setPaymentReference("REF-" + System.currentTimeMillis() + "-" + random.nextInt(1000));
                    } else {
                        dueSchedule.setStatus(DueStatus.OVERDUE);
                        // Mahnungen für überfällige
                        if (random.nextBoolean()) {
                            dueSchedule.setReminderSent(true);
                            dueSchedule.setReminderCount(random.nextInt(3) + 1);
                            dueSchedule.setLastReminderDate(now.minusDays(random.nextInt(10)));
                        }
                    }
                } else if (dueDate.isBefore(now.plusDays(7))) {
                    // Bald fällige als ausstehend
                    dueSchedule.setStatus(DueStatus.PENDING);
                } else {
                    // Zukünftige als ausstehend
                    dueSchedule.setStatus(DueStatus.PENDING);
                }

                // Gelegentliche Notizen hinzufügen
                if (random.nextDouble() < 0.1) {
                    String[] notes = {
                            "Kunde hat um Aufschub gebeten",
                            "Zahlungserinnerung versendet",
                            "Automatische Zahlung fehlgeschlagen",
                            "Kunde kontaktiert wegen Zahlungsproblemen",
                            "Zahlung erfolgt nach Mahnung"
                    };
                    dueSchedule.setNotes(notes[random.nextInt(notes.length)]);
                }

                dueScheduleRepository.save(dueSchedule);

                // Nächste Periode berechnen
                currentDate = calculateNextPeriodStart(currentDate, subscription.getBillingCycle());
            }
        }
    }

    // --- 7. Rechnungen (angepasst) ---
    private void initInvoices() {
        // Prüfen ob bereits Rechnungen existieren
        try {
            if (!invoiceService.getAllInvoices().isEmpty()) {
                return;
            }
        } catch (Exception e) {
            // Falls getAllInvoices() andere Signatur hat, alternative Prüfung
            return;
        }

        // Rechnungen basierend auf bezahlten Fälligkeitsplänen generieren
        List<DueSchedule> paidSchedules = dueScheduleRepository.findByStatus(DueStatus.PAID);
        Random random = new Random();

        for (DueSchedule dueSchedule : paidSchedules) {
            // Nicht für jeden bezahlten Fälligkeitsplan eine Rechnung (60% Wahrscheinlichkeit)
            if (random.nextDouble() < 0.6) {
                Customer customer = dueSchedule.getSubscription().getContract().getCustomer();

                Invoice invoice = new Invoice();
                invoice.setCustomer(customer);
                invoice.setBillingAddress(customer.getBillingAddress());
                // Rechnungsnummer wird automatisch vom NumberGeneratorService generiert
                invoice.setInvoiceDate(dueSchedule.getPeriodStart());
                invoice.setDueDate(dueSchedule.getDueDate());

                // Invoice Item für das Abonnement
                InvoiceItem item = new InvoiceItem(
                        dueSchedule.getSubscription().getProductName(),
                        BigDecimal.ONE,
                        "Monat",
                        dueSchedule.getAmount()
                );
                item.setPosition(1);
                item.setDescription("Abonnement für Periode " +
                        dueSchedule.getPeriodStart() + " bis " + dueSchedule.getPeriodEnd());
                invoice.addInvoiceItem(item);

                invoiceService.createInvoice(invoice);
            }
        }
    }

    // --- Hilfsmethoden für Fälligkeitspläne ---

    private LocalDate calculatePeriodEnd(LocalDate periodStart, BillingCycle billingCycle) {
        return switch (billingCycle) {
            case MONTHLY -> periodStart.plusMonths(1).minusDays(1);
            case QUARTERLY -> periodStart.plusMonths(3).minusDays(1);
            case SEMI_ANNUALLY -> periodStart.plusMonths(6).minusDays(1);
            case ANNUALLY -> periodStart.plusYears(1).minusDays(1);
        };
    }

    private LocalDate calculateDueDate(LocalDate periodEnd, BillingCycle billingCycle) {
        // Fälligkeitsdatum ist meist am Ende der Periode oder kurz danach
        Random random = new Random();
        return periodEnd.plusDays(random.nextInt(7)); // 0-6 Tage nach Periodenende
    }

    private LocalDate calculateNextPeriodStart(LocalDate currentStart, BillingCycle billingCycle) {
        return switch (billingCycle) {
            case MONTHLY -> currentStart.plusMonths(1);
            case QUARTERLY -> currentStart.plusMonths(3);
            case SEMI_ANNUALLY -> currentStart.plusMonths(6);
            case ANNUALLY -> currentStart.plusYears(1);
        };
    }

    private String getRandomPaymentMethod(Random random) {
        String[] methods = {"SEPA-Lastschrift", "Kreditkarte", "PayPal", "Überweisung", "Sofortüberweisung"};
        return methods[random.nextInt(methods.length)];
    }
}