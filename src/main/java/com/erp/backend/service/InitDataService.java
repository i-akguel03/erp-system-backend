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
 * Dieser Service erstellt eine vollständige Testdatenstruktur mit allen notwendigen
 * Abhängigkeiten und stellt sicher, dass die Geschäftslogik korrekt abgebildet wird.
 *
 * Initialisierungsreihenfolge:
 * 1. Adressen erzeugen (Basis für Kunden)
 * 2. Kunden erzeugen (mit Adresszuweisungen)
 * 3. Produkte erzeugen (für Abonnements)
 * 4. Verträge erzeugen (Kundenbeziehungen)
 * 5. Abonnements erzeugen (produktbasiert)
 * 6. Fälligkeitspläne für 12 Monate erzeugen (Abrechnungszyklen)
 * 7. Rechnungen und offene Posten für bezahlte Fälligkeiten erzeugen
 */
@Service
public class InitDataService {

    private static final Logger logger = LoggerFactory.getLogger(InitDataService.class);

    // Repository-Abhängigkeiten für Datenzugriff
    private final AddressRepository addressRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final ContractRepository contractRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final DueScheduleRepository dueScheduleRepository;
    private final OpenItemRepository openItemRepository;

    // Service-Abhängigkeiten für Geschäftslogik
    private final InvoiceService invoiceService;
    private final NumberGeneratorService numberGeneratorService;

    // Random-Generator für realistische Testdaten
    private final Random random = new Random();

    /**
     * Konstruktor mit Dependency Injection
     */
    public InitDataService(AddressRepository addressRepository,
                           CustomerRepository customerRepository,
                           ProductRepository productRepository,
                           ContractRepository contractRepository,
                           SubscriptionRepository subscriptionRepository,
                           DueScheduleRepository dueScheduleRepository,
                           OpenItemRepository openItemRepository,
                           InvoiceService invoiceService,
                           NumberGeneratorService numberGeneratorService) {
        this.addressRepository = addressRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.contractRepository = contractRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.dueScheduleRepository = dueScheduleRepository;
        this.openItemRepository = openItemRepository;
        this.invoiceService = invoiceService;
        this.numberGeneratorService = numberGeneratorService;
    }

    /**
     * Hauptmethode zur Initialisierung aller Testdaten.
     *
     * Diese Methode orchestriert die gesamte Testdateninitialisierung in der
     * korrekten Reihenfolge, um referenzielle Integrität zu gewährleisten.
     */
    @Transactional
    public void initAllData() {
        logger.info("Starte Initialisierung der Testdaten...");

        initAddresses();        // Schritt 1: Grundlegende Adressdaten
        initCustomers();        // Schritt 2: Kunden mit Adressreferenzen
        initProducts();         // Schritt 3: Produktkatalog für Abonnements
        initContracts();        // Schritt 4: Kundenverträge
        initSubscriptions();    // Schritt 5: Abonnements basierend auf Verträgen
        initDueSchedules();     // Schritt 6: Abrechnungszyklen für Abonnements
        initInvoicesAndOpenItems(); // Schritt 7: Rechnungen und offene Posten

        logger.info("Testdateninitialisierung erfolgreich abgeschlossen.");
    }

    // ===============================================================================================
    // SCHRITT 1: ADRESSEN ERZEUGEN
    // ===============================================================================================

    /**
     * Erstellt eine Sammlung von Testadressen aus verschiedenen deutschen Städten.
     * Diese Adressen werden später für Kunden (Rechnungsadresse, Lieferadresse, etc.) verwendet.
     */
    private void initAddresses() {
        // Prüfung: Nur initialisieren wenn noch keine Adressen vorhanden
        if (addressRepository.count() > 0) {
            logger.info("Adressen bereits vorhanden - Überspringe Initialisierung");
            return;
        }

        logger.info("Initialisiere Adressen...");

        // Realistische deutsche Testdaten für verschiedene Städte
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

        // Adressen aus den Testdaten erstellen und speichern
        for (String[] data : addressData) {
            Address address = new Address(data[0], data[1], data[2], data[3]);
            addressRepository.save(address);
        }

        logger.info("Es wurden {} Adressen erstellt", addressData.length);
    }

    // ===============================================================================================
    // SCHRITT 2: KUNDEN ERZEUGEN
    // ===============================================================================================

    /**
     * Erstellt Testkunden mit zufällig zugewiesenen Adressen.
     * Jeder Kunde erhält eine eindeutige Kundennummer und verschiedene Adresstypen.
     */
    private void initCustomers() {
        // Prüfung: Nur initialisieren wenn noch keine Kunden vorhanden
        if (customerRepository.count() > 0) {
            logger.info("Kunden bereits vorhanden - Überspringe Initialisierung");
            return;
        }

        logger.info("Initialisiere Kunden...");

        // Alle verfügbaren Adressen für Zuweisung laden
        List<Address> addresses = addressRepository.findAll();
        if (addresses.isEmpty()) {
            throw new IllegalStateException("Keine Adressen gefunden - Adressen müssen zuerst initialisiert werden");
        }

        // Realistische deutsche Namen für Testkunden
        String[] firstNames = {"Max", "Anna", "Tom", "Laura", "Paul", "Sophie", "Lukas", "Marie",
                "Felix", "Emma", "Jonas", "Lea", "Ben", "Mia", "Leon", "Hannah",
                "Noah", "Lena", "Finn", "Emilia"};
        String[] lastNames = {"Müller", "Schmidt", "Schneider", "Fischer", "Weber", "Becker",
                "Hoffmann", "Schäfer", "Koch", "Richter", "Klein", "Wolf",
                "Schröder", "Neumann", "Schwarz", "Zimmermann"};

        // 25 Testkunden erstellen
        final int numberOfCustomers = 25;
        for (int i = 1; i <= numberOfCustomers; i++) {
            // Zufällige Namenskombination
            String firstName = firstNames[random.nextInt(firstNames.length)];
            String lastName = lastNames[random.nextInt(lastNames.length)];

            // Realistische E-Mail und Telefonnummer generieren
            String email = firstName.toLowerCase() + "." + lastName.toLowerCase() + i + "@test.com";
            String phoneNumber = "+49" + (random.nextInt(900000000) + 100000000);

            // Kunden erstellen
            Customer customer = new Customer(firstName, lastName, email, phoneNumber);
            customer.setCustomerNumber(numberGeneratorService.generateCustomerNumber());

            // Zufällige Adresszuweisungen (können unterschiedlich sein)
            customer.setBillingAddress(addresses.get(random.nextInt(addresses.size())));
            customer.setShippingAddress(addresses.get(random.nextInt(addresses.size())));
            customer.setResidentialAddress(addresses.get(random.nextInt(addresses.size())));

            customerRepository.save(customer);
        }

        logger.info("Es wurden {} Kunden erstellt", numberOfCustomers);
    }

    // ===============================================================================================
    // SCHRITT 3: PRODUKTE ERZEUGEN
    // ===============================================================================================

    /**
     * Erstellt einen Produktkatalog mit verschiedenen Produkttypen.
     * Unterscheidet zwischen einmaligen Produkten und Abonnement-Produkten.
     */
    private void initProducts() {
        // Prüfung: Nur initialisieren wenn noch keine Produkte vorhanden
        if (productRepository.count() > 0) {
            logger.info("Produkte bereits vorhanden - Überspringe Initialisierung");
            return;
        }

        logger.info("Initialisiere Produktkatalog...");

        // Produktdefinitionen: [Name, Preis, Einheit, Kategorie]
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

        // Produkte erstellen und speichern
        for (int i = 0; i < products.length; i++) {
            Product product = new Product(
                    (String) products[i][0],           // Name
                    BigDecimal.valueOf((Double) products[i][1]), // Preis
                    (String) products[i][2]            // Einheit
            );

            // Eindeutige Produktnummer generieren
            product.setProductNumber("PROD-" + String.format("%03d", i + 1));

            // Kategorie als Beschreibung setzen (falls verfügbar)
            if (products[i].length > 3) {
                product.setDescription("Kategorie: " + products[i][3]);
            }

            productRepository.save(product);
        }

        logger.info("Es wurden {} Produkte erstellt", products.length);
    }

    // ===============================================================================================
    // SCHRITT 4: VERTRÄGE ERZEUGEN
    // ===============================================================================================

    /**
     * Erstellt Kundenverträge mit verschiedenen Status und Laufzeiten.
     * Verträge bilden die Basis für Abonnements.
     */
    private void initContracts() {
        // Prüfung: Nur initialisieren wenn noch keine Verträge vorhanden
        if (contractRepository.count() > 0) {
            logger.info("Verträge bereits vorhanden - Überspringe Initialisierung");
            return;
        }

        logger.info("Initialisiere Verträge...");

        List<Customer> customers = customerRepository.findAll();
        if (customers.isEmpty()) {
            throw new IllegalStateException("Keine Kunden gefunden - Kunden müssen zuerst initialisiert werden");
        }

        // 20 Verträge erstellen (mehr als Kunden, da ein Kunde mehrere Verträge haben kann)
        final int numberOfContracts = 20;
        for (int i = 1; i <= numberOfContracts; i++) {
            // Zufälligen Kunden auswählen
            Customer customer = customers.get(random.nextInt(customers.size()));

            // Vertragsstartdatum zwischen 0-365 Tagen in der Vergangenheit
            LocalDate startDate = LocalDate.now().minusDays(random.nextInt(365));

            // Vertrag erstellen
            Contract contract = new Contract("Servicevertrag " + i, startDate, customer);
            contract.setId(null); // Sicherstellen, dass ID von Datenbank generiert wird
            contract.setContractNumber(numberGeneratorService.generateContractNumber());

            // Vertragsstatus zufällig festlegen
            // 80% aktive Verträge, 20% beendet
            if (random.nextDouble() < 0.8) {
                contract.setContractStatus(ContractStatus.ACTIVE);

                // 30% der aktiven Verträge haben ein festgelegtes Enddatum
                if (random.nextDouble() < 0.3) {
                    contract.setEndDate(startDate.plusYears(1 + random.nextInt(2)));
                }
            } else {
                // Beendete Verträge
                contract.setContractStatus(ContractStatus.TERMINATED);
                contract.setEndDate(startDate.plusDays(random.nextInt(300)));
            }

            contractRepository.save(contract);
        }

        logger.info("Es wurden {} Verträge erstellt", numberOfContracts);
    }

    // ===============================================================================================
    // SCHRITT 5: ABONNEMENTS ERZEUGEN
    // ===============================================================================================

    /**
     * Erstellt Abonnements basierend auf Verträgen und monatlichen Produkten.
     * Abonnements bilden die Basis für wiederkehrende Abrechnungen.
     */
    private void initSubscriptions() {
        // Prüfung: Nur initialisieren wenn noch keine Abonnements vorhanden
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

        // Nur Produkte mit monatlicher Abrechnung für Abonnements verwenden
        List<Product> subscriptionProducts = products.stream()
                .filter(p -> "Monat".equals(p.getUnit()))
                .toList();

        if (subscriptionProducts.isEmpty()) {
            logger.warn("Keine monatlichen Produkte für Abonnements gefunden");
            return;
        }

        // 35 Abonnements erstellen (mehr als Verträge, da ein Vertrag mehrere Abos haben kann)
        final int numberOfSubscriptions = 35;
        for (int i = 1; i <= numberOfSubscriptions; i++) {
            // Zufälligen Vertrag und Produkt auswählen
            Contract contract = contracts.get(random.nextInt(contracts.size()));
            Product product = subscriptionProducts.get(random.nextInt(subscriptionProducts.size()));

            // Abonnement-Start nach Vertragsbeginn
            LocalDate subscriptionStart = contract.getStartDate().plusDays(random.nextInt(30));

            // Abonnement erstellen
            Subscription subscription = new Subscription(
                    product.getName(),
                    product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO,
                    subscriptionStart,
                    contract
            );

            subscription.setSubscriptionNumber(numberGeneratorService.generateSubscriptionNumber());
            subscription.setDescription("Monatliches Abonnement für " + product.getName());

            // Zufälligen Abrechnungszyklus auswählen
            BillingCycle[] cycles = BillingCycle.values();
            subscription.setBillingCycle(cycles[random.nextInt(cycles.length)]);

            // Abonnement-Status abhängig vom Vertragsstatus
            if (contract.getContractStatus() == ContractStatus.ACTIVE) {
                subscription.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
            } else {
                subscription.setSubscriptionStatus(SubscriptionStatus.CANCELLED);
                subscription.setEndDate(contract.getEndDate());
            }

            // Automatische Verlängerung zufällig setzen
            subscription.setAutoRenewal(random.nextBoolean());

            // Abonnement zum Vertrag hinzufügen und speichern
            contract.addSubscription(subscription);
            subscriptionRepository.save(subscription);
        }

        logger.info("Es wurden {} Abonnements erstellt", numberOfSubscriptions);
    }

    // ===============================================================================================
    // SCHRITT 6: FÄLLIGKEITSPLÄNE ERZEUGEN (12 MONATE)
    // ===============================================================================================

    /**
     * Erstellt Fälligkeitspläne für aktive Abonnements über 12 Monate.
     * Simuliert realistische Zahlungsverhalten mit teilweise bezahlten und offenen Positionen.
     */
    private void initDueSchedules() {
        // Prüfung: Nur initialisieren wenn noch keine Fälligkeitspläne vorhanden
        if (dueScheduleRepository.count() > 0) {
            logger.info("Fälligkeitspläne bereits vorhanden - Überspringe Initialisierung");
            return;
        }

        logger.info("Initialisiere Fälligkeitspläne für 12 Monate...");

        // Nur aktive Abonnements für Fälligkeitspläne verwenden
        List<Subscription> activeSubscriptions = subscriptionRepository.findBySubscriptionStatus(SubscriptionStatus.ACTIVE);
        if (activeSubscriptions.isEmpty()) {
            logger.warn("Keine aktiven Abonnements für Fälligkeitspläne gefunden");
            return;
        }

        int totalSchedulesCreated = 0;
        int paidSchedules = 0;

        // Für jedes aktive Abonnement 12 monatliche Fälligkeiten erstellen
        for (Subscription subscription : activeSubscriptions) {
            LocalDate currentDate = subscription.getStartDate();

            // 12 Monate ab Abonnement-Start
            for (int month = 0; month < 12; month++) {
                LocalDate periodStart = currentDate;
                LocalDate periodEnd = currentDate.plusMonths(1).minusDays(1);
                LocalDate dueDate = periodEnd;

                // Betrag aus Abonnement übernehmen
                BigDecimal amount = subscription.getMonthlyPrice() != null
                        ? subscription.getMonthlyPrice()
                        : BigDecimal.ZERO;

                // Fälligkeitsplan erstellen
                DueSchedule dueSchedule = new DueSchedule();
                dueSchedule.setDueNumber(numberGeneratorService.generateDueNumber());
                dueSchedule.setDueDate(dueDate);
                dueSchedule.setAmount(amount);
                dueSchedule.setPeriodStart(periodStart);
                dueSchedule.setPeriodEnd(periodEnd);
                dueSchedule.setSubscription(subscription);

                // Realistische Zahlungsverteilung simulieren:
                // - 60% vollständig bezahlt
                // - 20% teilweise bezahlt
                // - 20% unbezahlt/ausstehend
                double paymentRandom = random.nextDouble();

                if (paymentRandom < 0.6) {
                    // Vollständig bezahlt
                    dueSchedule.setStatus(DueStatus.PAID);
                    dueSchedule.setPaidDate(dueDate.minusDays(random.nextInt(10))); // Bezahlt 0-10 Tage vor Fälligkeit
                    dueSchedule.setPaidAmount(amount);
                    dueSchedule.setPaymentMethod("SEPA-Lastschrift");
                    dueSchedule.setPaymentReference("REF-" + System.currentTimeMillis() + "-" + random.nextInt(10000));
                    paidSchedules++;

                } else if (paymentRandom < 0.8) {
                    // Teilweise bezahlt (70-90% des Betrags)
                    BigDecimal paidPercentage = BigDecimal.valueOf(0.7 + (random.nextDouble() * 0.2)); // 70-90%
                    BigDecimal paidAmount = amount.multiply(paidPercentage);

                    dueSchedule.setStatus(DueStatus.PARTIAL_PAID);
                    dueSchedule.setPaidDate(dueDate.minusDays(random.nextInt(5)));
                    dueSchedule.setPaidAmount(paidAmount);
                    dueSchedule.setPaymentMethod("Überweisung");
                    dueSchedule.setPaymentReference("PART-" + System.currentTimeMillis() + "-" + random.nextInt(10000));

                } else {
                    // Unbezahlt/Ausstehend
                    dueSchedule.setStatus(DueStatus.PENDING);
                }

                dueScheduleRepository.save(dueSchedule);
                totalSchedulesCreated++;
                currentDate = currentDate.plusMonths(1);
            }
        }

        logger.info("Es wurden {} Fälligkeitspläne erstellt ({} davon als bezahlt markiert)",
                totalSchedulesCreated, paidSchedules);
    }

    // ===============================================================================================
    // SCHRITT 7: RECHNUNGEN UND OFFENE POSTEN FÜR BEZAHLTE FÄLLIGKEITEN
    // ===============================================================================================

    /**
     * Erstellt Rechnungen und offene Posten für alle bezahlten und teilweise bezahlten Fälligkeitspläne.
     * Stellt sicher, dass die Geschäftslogik zwischen Fälligkeiten, Rechnungen und offenen Posten
     * korrekt abgebildet wird.
     */
    private void initInvoicesAndOpenItems() {
        logger.info("Initialisiere Rechnungen und offene Posten...");

        // Alle bezahlten und teilweise bezahlten Fälligkeitspläne abrufen
        List<DueSchedule> paidSchedules = dueScheduleRepository.findByStatus(DueStatus.PAID);
        List<DueSchedule> partiallyPaidSchedules = dueScheduleRepository.findByStatus(DueStatus.PARTIAL_PAID);

        int invoicesCreated = 0;
        int openItemsCreated = 0;

        // === VOLLSTÄNDIG BEZAHLTE FÄLLIGKEITEN VERARBEITEN ===
        logger.info("Verarbeite {} vollständig bezahlte Fälligkeitspläne...", paidSchedules.size());

        for (DueSchedule dueSchedule : paidSchedules) {
            try {
                // Rechnung über den InvoiceService erstellen
                Invoice invoice = invoiceService.createInvoiceFromDueSchedule(dueSchedule);
                invoicesCreated++;

                // Bei vollständig bezahlten Fälligkeiten trotzdem einen kleinen offenen Posten erstellen
                // (z.B. Mahngebühren, Rundungsdifferenzen, etc.)
                if (random.nextDouble() < 0.3) { // 30% haben zusätzliche offene Posten
                    createRandomOpenItem(invoice, dueSchedule);
                    openItemsCreated++;
                }

                logger.debug("Rechnung erstellt für DueSchedule {}: Invoice {}",
                        dueSchedule.getDueNumber(), invoice.getInvoiceNumber());

            } catch (Exception e) {
                logger.error("Fehler beim Erstellen der Rechnung für DueSchedule {}: {}",
                        dueSchedule.getDueNumber(), e.getMessage());
            }
        }

        // === TEILWEISE BEZAHLTE FÄLLIGKEITEN VERARBEITEN ===
        logger.info("Verarbeite {} teilweise bezahlte Fälligkeitspläne...", partiallyPaidSchedules.size());

        for (DueSchedule dueSchedule : partiallyPaidSchedules) {
            try {
                // Zusätzliche Validierung für teilweise bezahlte Fälligkeiten
                if (dueSchedule.getAmount() == null || dueSchedule.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                    logger.warn("Überspringe teilweise bezahlten DueSchedule {} - Betrag ist null oder <= 0: {}",
                            dueSchedule.getDueNumber(), dueSchedule.getAmount());
                    continue;
                }

                if (dueSchedule.getSubscription() == null) {
                    logger.warn("Überspringe teilweise bezahlten DueSchedule {} - Subscription ist null",
                            dueSchedule.getDueNumber());
                    continue;
                }

                // Rechnung über den InvoiceService erstellen
                Invoice invoice = invoiceService.createInvoiceFromDueSchedule(dueSchedule);
                invoicesCreated++;

                // Offenen Betrag berechnen (Differenz zwischen Gesamtbetrag und bezahltem Betrag)
                BigDecimal totalAmount = dueSchedule.getAmount();
                BigDecimal paidAmount = dueSchedule.getPaidAmount() != null ? dueSchedule.getPaidAmount() : BigDecimal.ZERO;
                BigDecimal openAmount = totalAmount.subtract(paidAmount);

                // Offenen Posten nur erstellen wenn tatsächlich ein Betrag offen ist
                if (openAmount.compareTo(BigDecimal.ZERO) > 0) {
                    createOpenItemForPartialPayment(invoice, dueSchedule, openAmount);
                    openItemsCreated++;
                }

                logger.debug("Rechnung mit offenem Posten erstellt für DueSchedule {}: Invoice {}, Offener Betrag: {}",
                        dueSchedule.getDueNumber(), invoice.getInvoiceNumber(), openAmount);

            } catch (Exception e) {
                logger.error("Fehler beim Erstellen der Rechnung für teilweise bezahlten DueSchedule {}: {}",
                        dueSchedule.getDueNumber(), e.getMessage(), e);
            }
        }

        logger.info("Rechnungen und offene Posten erfolgreich erstellt: {} Rechnungen, {} offene Posten",
                invoicesCreated, openItemsCreated);
    }

    /**
     * Erstellt einen zufälligen offenen Posten für vollständig bezahlte Rechnungen.
     * Simuliert zusätzliche Gebühren oder Rundungsdifferenzen.
     */
    private void createRandomOpenItem(Invoice invoice, DueSchedule dueSchedule) {
        // Zufällige zusätzliche Gebühren zwischen 5-50 Euro
        BigDecimal randomAmount = BigDecimal.valueOf(5 + (random.nextDouble() * 45));

        String[] descriptions = {
                "Bearbeitungsgebühr",
                "Mahngebühr",
                "Rundungsdifferenz",
                "Verwaltungsgebühr",
                "Zusatzleistung"
        };

        String description = descriptions[random.nextInt(descriptions.length)] +
                " für " + dueSchedule.getSubscription().getProductName();

        OpenItem openItem = new OpenItem();
        openItem.setInvoice(invoice);
        openItem.setAmount(randomAmount);
        openItem.setDescription(description);
        openItem.setDueDate(dueSchedule.getDueDate().plusDays(14)); // 2 Wochen nach ursprünglicher Fälligkeit
        openItem.setStatus(OpenItem.OpenItemStatus.OPEN);

        openItemRepository.save(openItem);
    }

    /**
     * Erstellt einen offenen Posten für den unbezahlten Teil einer teilweise bezahlten Rechnung.
     */
    private void createOpenItemForPartialPayment(Invoice invoice, DueSchedule dueSchedule, BigDecimal openAmount) {
        String description = "Ausstehender Betrag für " + dueSchedule.getSubscription().getProductName() +
                " | Periode: " + dueSchedule.getPeriodStart() + " bis " + dueSchedule.getPeriodEnd();

        OpenItem openItem = new OpenItem();
        openItem.setInvoice(invoice);
        openItem.setAmount(openAmount);
        openItem.setDescription(description);
        openItem.setDueDate(dueSchedule.getDueDate().plusDays(7)); // 1 Woche nach ursprünglicher Fälligkeit
        openItem.setStatus(OpenItem.OpenItemStatus.OPEN);

        openItemRepository.save(openItem);
    }

    // ===============================================================================================
    // HILFSMETHODEN UND UTILITIES
    // ===============================================================================================

    /**
     * Prüft den aktuellen Status der Testdateninitialisierung.
     * Nützlich für Debugging und Monitoring.
     */
    @Transactional(readOnly = true)
    public void logCurrentDataStatus() {
        logger.info("=== AKTUELLER DATENBESTAND ===");
        logger.info("Adressen: {}", addressRepository.count());
        logger.info("Kunden: {}", customerRepository.count());
        logger.info("Produkte: {}", productRepository.count());
        logger.info("Verträge: {}", contractRepository.count());
        logger.info("Abonnements: {}", subscriptionRepository.count());
        logger.info("Fälligkeitspläne: {}", dueScheduleRepository.count());

        // Detaillierte Aufschlüsselung der Fälligkeitspläne nach Status
        long paidSchedules = dueScheduleRepository.countByStatus(DueStatus.PAID);
        long partiallyPaidSchedules = dueScheduleRepository.countByStatus(DueStatus.PARTIAL_PAID);
        long pendingSchedules = dueScheduleRepository.countByStatus(DueStatus.PENDING);

        logger.info("  - Bezahlte Fälligkeiten: {}", paidSchedules);
        logger.info("  - Teilweise bezahlte Fälligkeiten: {}", partiallyPaidSchedules);
        logger.info("  - Ausstehende Fälligkeiten: {}", pendingSchedules);

        // Rechnungen und offene Posten
        long totalInvoices = invoiceService.getAllInvoices().size();
        long openItems = openItemRepository.count();
        long openOpenItems = openItemRepository.countByStatus(OpenItem.OpenItemStatus.OPEN);
        long paidOpenItems = openItemRepository.countByStatus(OpenItem.OpenItemStatus.PAID);

        logger.info("Rechnungen: {}", totalInvoices);
        logger.info("Offene Posten: {}", openItems);
        logger.info("  - Offene Posten: {}", openOpenItems);
        logger.info("  - Bezahlte Posten: {}", paidOpenItems);
        logger.info("==============================");
    }

    /**
     * Löscht alle Testdaten in umgekehrter Reihenfolge der Abhängigkeiten.
     * VORSICHT: Diese Methode löscht alle Daten unwiderruflich!
     */
    @Transactional
    public void clearAllTestData() {
        logger.warn("WARNUNG: Lösche alle Testdaten...");

        // Löschung in umgekehrter Reihenfolge der Abhängigkeiten
        logger.info("Lösche offene Posten...");
        openItemRepository.deleteAll();

        logger.info("Lösche Rechnungen...");
        // Über Service löschen um Geschäftslogik zu beachten
        invoiceService.getAllInvoices().forEach(invoice -> {
            try {
                invoiceService.deleteInvoice(invoice.getId());
            } catch (Exception e) {
                logger.warn("Fehler beim Löschen der Rechnung {}: {}", invoice.getId(), e.getMessage());
            }
        });

        logger.info("Lösche Fälligkeitspläne...");
        dueScheduleRepository.deleteAll();

        logger.info("Lösche Abonnements...");
        subscriptionRepository.deleteAll();

        logger.info("Lösche Verträge...");
        contractRepository.deleteAll();

        logger.info("Lösche Produkte...");
        productRepository.deleteAll();

        logger.info("Lösche Kunden...");
        customerRepository.deleteAll();

        logger.info("Lösche Adressen...");
        addressRepository.deleteAll();

        logger.info("Alle Testdaten wurden gelöscht.");
    }

    /**
     * Erstellt nur die Basisdaten (Adressen, Kunden, Produkte) ohne Geschäftsdaten.
     * Nützlich für minimale Testsetups.
     */
    @Transactional
    public void initBasicDataOnly() {
        logger.info("Initialisiere nur Basisdaten...");
        initAddresses();
        initCustomers();
        initProducts();
        logger.info("Basisdaten initialisiert.");
    }

    /**
     * Erstellt nur Geschäftsdaten (Verträge, Abonnements, Fälligkeiten, Rechnungen).
     * Setzt voraus, dass Basisdaten bereits existieren.
     */
    @Transactional
    public void initBusinessDataOnly() {
        logger.info("Initialisiere nur Geschäftsdaten...");

        // Prüfen ob Basisdaten vorhanden sind
        if (customerRepository.count() == 0 || productRepository.count() == 0) {
            throw new IllegalStateException("Basisdaten müssen vor Geschäftsdaten initialisiert werden");
        }

        initContracts();
        initSubscriptions();
        initDueSchedules();
        initInvoicesAndOpenItems();
        logger.info("Geschäftsdaten initialisiert.");
    }

    /**
     * Erstellt zusätzliche Testszenarien für spezifische Anwendungsfälle.
     * Kann nach der Hauptinitialisierung aufgerufen werden.
     */
    @Transactional
    public void initSpecialScenarios() {
        logger.info("Initialisiere spezielle Testszenarien...");

        // Szenario 1: Kunde mit vielen überfälligen Rechnungen
        createOverdueCustomerScenario();

        // Szenario 2: Kunde mit sehr hohen Beträgen
        createHighValueCustomerScenario();

        // Szenario 3: Stornierte Abonnements mit Restbeträgen
        createCancelledSubscriptionScenario();

        logger.info("Spezielle Testszenarien erstellt.");
    }

    /**
     * Erstellt ein Testszenario mit einem Kunden der viele überfällige Zahlungen hat.
     */
    private void createOverdueCustomerScenario() {
        logger.debug("Erstelle Szenario: Kunde mit überfälligen Zahlungen...");

        // Einen bestehenden Kunden auswählen oder neuen erstellen
        List<Customer> customers = customerRepository.findAll();
        if (customers.isEmpty()) return;

        Customer overdueCustomer = customers.get(0);

        // Vertrag mit mehreren überfälligen Abonnements erstellen
        Contract overdueContract = new Contract("Überfälliger Servicevertrag",
                LocalDate.now().minusMonths(6), overdueCustomer);
        overdueContract.setContractNumber(numberGeneratorService.generateContractNumber());
        overdueContract.setContractStatus(ContractStatus.ACTIVE);
        contractRepository.save(overdueContract);

        // Mehrere Abonnements mit überfälligen Zahlungen
        List<Product> monthlyProducts = productRepository.findAll().stream()
                .filter(p -> "Monat".equals(p.getUnit()))
                .limit(3)
                .toList();

        for (Product product : monthlyProducts) {
            Subscription subscription = new Subscription(
                    product.getName() + " (Überfällig)",
                    product.getPrice(),
                    LocalDate.now().minusMonths(5),
                    overdueContract
            );
            subscription.setSubscriptionNumber(numberGeneratorService.generateSubscriptionNumber());
            subscription.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
            subscriptionRepository.save(subscription);

            // Überfällige Fälligkeitspläne erstellen
            for (int i = 1; i <= 4; i++) {
                LocalDate dueDate = LocalDate.now().minusMonths(i);

                DueSchedule overdueSchedule = new DueSchedule();
                overdueSchedule.setDueNumber(numberGeneratorService.generateDueNumber());
                overdueSchedule.setDueDate(dueDate);
                overdueSchedule.setAmount(product.getPrice());
                overdueSchedule.setPeriodStart(dueDate.minusMonths(1));
                overdueSchedule.setPeriodEnd(dueDate.minusDays(1));
                overdueSchedule.setSubscription(subscription);
                overdueSchedule.setStatus(DueStatus.PENDING); // Unbezahlt = überfällig

                dueScheduleRepository.save(overdueSchedule);

                // Rechnung für überfällige Position erstellen
                Invoice overdueInvoice = invoiceService.createInvoiceFromDueSchedule(overdueSchedule);

                // Offenen Posten mit Mahngebühren erstellen
                OpenItem overdueOpenItem = new OpenItem();
                overdueOpenItem.setInvoice(overdueInvoice);
                overdueOpenItem.setAmount(product.getPrice().add(BigDecimal.valueOf(10.00))); // + Mahngebühr
                overdueOpenItem.setDescription("Überfälliger Betrag + Mahngebühr für " + product.getName());
                overdueOpenItem.setDueDate(dueDate);
                overdueOpenItem.setStatus(OpenItem.OpenItemStatus.OPEN);

                openItemRepository.save(overdueOpenItem);
            }
        }

        logger.debug("Überfälligkeitsszenario für Kunde {} erstellt", overdueCustomer.getCustomerNumber());
    }

    /**
     * Erstellt ein Testszenario mit einem Kunden der sehr hohe Beträge hat.
     */
    private void createHighValueCustomerScenario() {
        logger.debug("Erstelle Szenario: Kunde mit hohen Beträgen...");

        List<Customer> customers = customerRepository.findAll();
        if (customers.size() < 2) return;

        Customer highValueCustomer = customers.get(1);

        // Enterprise-Vertrag erstellen
        Contract enterpriseContract = new Contract("Enterprise Servicevertrag",
                LocalDate.now().minusMonths(3), highValueCustomer);
        enterpriseContract.setContractNumber(numberGeneratorService.generateContractNumber());
        enterpriseContract.setContractStatus(ContractStatus.ACTIVE);
        contractRepository.save(enterpriseContract);

        // Hochpreisige Abonnements
        String[] enterpriseServices = {
                "Enterprise Cloud Platform",
                "Premium Support Package",
                "Advanced Analytics Suite"
        };

        BigDecimal[] enterprisePrices = {
                BigDecimal.valueOf(2500.00),
                BigDecimal.valueOf(1200.00),
                BigDecimal.valueOf(1800.00)
        };

        for (int i = 0; i < enterpriseServices.length; i++) {
            Subscription enterpriseSubscription = new Subscription(
                    enterpriseServices[i],
                    enterprisePrices[i],
                    LocalDate.now().minusMonths(2),
                    enterpriseContract
            );
            enterpriseSubscription.setSubscriptionNumber(numberGeneratorService.generateSubscriptionNumber());
            enterpriseSubscription.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
            subscriptionRepository.save(enterpriseSubscription);

            // Fälligkeitspläne für die letzten 2 Monate
            for (int month = 1; month <= 2; month++) {
                LocalDate dueDate = LocalDate.now().minusMonths(month).withDayOfMonth(1);

                DueSchedule enterpriseSchedule = new DueSchedule();
                enterpriseSchedule.setDueNumber(numberGeneratorService.generateDueNumber());
                enterpriseSchedule.setDueDate(dueDate);
                enterpriseSchedule.setAmount(enterprisePrices[i]);
                enterpriseSchedule.setPeriodStart(dueDate.minusMonths(1));
                enterpriseSchedule.setPeriodEnd(dueDate.minusDays(1));
                enterpriseSchedule.setSubscription(enterpriseSubscription);
                enterpriseSchedule.setStatus(DueStatus.PAID);
                enterpriseSchedule.setPaidDate(dueDate.minusDays(2));
                enterpriseSchedule.setPaidAmount(enterprisePrices[i]);
                enterpriseSchedule.setPaymentMethod("Überweisung");

                dueScheduleRepository.save(enterpriseSchedule);

                // Rechnung erstellen
                invoiceService.createInvoiceFromDueSchedule(enterpriseSchedule);
            }
        }

        logger.debug("High-Value-Szenario für Kunde {} erstellt", highValueCustomer.getCustomerNumber());
    }

    /**
     * Erstellt ein Testszenario mit stornierten Abonnements die noch Restbeträge haben.
     */
    private void createCancelledSubscriptionScenario() {
        logger.debug("Erstelle Szenario: Stornierte Abonnements mit Restbeträgen...");

        List<Customer> customers = customerRepository.findAll();
        if (customers.size() < 3) return;

        Customer cancelledCustomer = customers.get(2);

        // Stornierter Vertrag
        Contract cancelledContract = new Contract("Stornierter Servicevertrag",
                LocalDate.now().minusMonths(4), cancelledCustomer);
        cancelledContract.setContractNumber(numberGeneratorService.generateContractNumber());
        cancelledContract.setContractStatus(ContractStatus.TERMINATED);
        cancelledContract.setEndDate(LocalDate.now().minusMonths(1));
        contractRepository.save(cancelledContract);

        // Storniertes Abonnement mit Restbetrag
        List<Product> products = productRepository.findAll().stream()
                .filter(p -> "Monat".equals(p.getUnit()))
                .findFirst().stream().toList();

        if (!products.isEmpty()) {
            Product product = products.get(0);

            Subscription cancelledSubscription = new Subscription(
                    product.getName() + " (Storniert)",
                    product.getPrice(),
                    LocalDate.now().minusMonths(3),
                    cancelledContract
            );
            cancelledSubscription.setSubscriptionNumber(numberGeneratorService.generateSubscriptionNumber());
            cancelledSubscription.setSubscriptionStatus(SubscriptionStatus.CANCELLED);
            cancelledSubscription.setEndDate(cancelledContract.getEndDate());
            subscriptionRepository.save(cancelledSubscription);

            // Letzte Rechnung mit Restbetrag
            DueSchedule finalSchedule = new DueSchedule();
            finalSchedule.setDueNumber(numberGeneratorService.generateDueNumber());
            finalSchedule.setDueDate(cancelledContract.getEndDate());
            finalSchedule.setAmount(product.getPrice());
            finalSchedule.setPeriodStart(cancelledContract.getEndDate().minusMonths(1));
            finalSchedule.setPeriodEnd(cancelledContract.getEndDate().minusDays(1));
            finalSchedule.setSubscription(cancelledSubscription);
            finalSchedule.setStatus(DueStatus.PARTIAL_PAID);
            finalSchedule.setPaidAmount(product.getPrice().multiply(BigDecimal.valueOf(0.6))); // 60% bezahlt
            finalSchedule.setPaidDate(cancelledContract.getEndDate().minusDays(10));
            finalSchedule.setPaymentMethod("Überweisung");

            dueScheduleRepository.save(finalSchedule);

            // Rechnung mit Restbetrag
            Invoice finalInvoice = invoiceService.createInvoiceFromDueSchedule(finalSchedule);

            // Offener Posten für Restbetrag
            BigDecimal openAmount = product.getPrice().subtract(finalSchedule.getPaidAmount());
            OpenItem restOpenItem = new OpenItem();
            restOpenItem.setInvoice(finalInvoice);
            restOpenItem.setAmount(openAmount);
            restOpenItem.setDescription("Restbetrag nach Stornierung: " + product.getName());
            restOpenItem.setDueDate(cancelledContract.getEndDate().plusDays(14));
            restOpenItem.setStatus(OpenItem.OpenItemStatus.OPEN);

            openItemRepository.save(restOpenItem);
        }

        logger.debug("Stornierungsszenario für Kunde {} erstellt", cancelledCustomer.getCustomerNumber());
    }
}