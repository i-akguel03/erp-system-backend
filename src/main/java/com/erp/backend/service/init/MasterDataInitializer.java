package com.erp.backend.service.init;

import com.erp.backend.domain.Vorgang;
import com.erp.backend.domain.VorgangTyp;
import com.erp.backend.service.VorgangService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erp.backend.domain.*;
import com.erp.backend.entity.Role;
import com.erp.backend.repository.AddressRepository;
import com.erp.backend.repository.CustomerRepository;
import com.erp.backend.repository.ProductRepository;
import com.erp.backend.service.NumberGeneratorService;
import com.erp.backend.service.UserDetailsServiceImpl;
import com.erp.backend.service.VorgangService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;

/**
 * STAMMDATEN-INITIALIZER
 * <p>
 * Verantwortlich für die Initialisierung der Basisdaten:
 * - Benutzer (für Login)
 * - Adressen (für Kunden und Rechnungen)
 * - Kunden (Geschäftspartner)
 * - Produkte (Artikelstamm)
 * <p>
 * Diese Daten sind die Grundlage für alle anderen Geschäftsdaten.
 */
@Service
@Transactional
public class MasterDataInitializer {

    private static final Logger logger = LoggerFactory.getLogger(MasterDataInitializer.class);

    // Repository-Dependencies für Stammdaten
    private final AddressRepository addressRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;

    // Service-Dependencies
    private final NumberGeneratorService numberGeneratorService;
    private final UserDetailsServiceImpl userDetailsService;
    private final VorgangService vorgangService;

    private final Random random = new Random();

    /**
     * KONSTRUKTOR mit Dependency Injection
     */
    public MasterDataInitializer(AddressRepository addressRepository,
                                 CustomerRepository customerRepository,
                                 ProductRepository productRepository,
                                 NumberGeneratorService numberGeneratorService,
                                 UserDetailsServiceImpl userDetailsService,
                                 VorgangService vorgangService) {
        this.addressRepository = addressRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.numberGeneratorService = numberGeneratorService;
        this.userDetailsService = userDetailsService;
        this.vorgangService = vorgangService;
    }

    /**
     * HAUPTMETHODE: Alle Stammdaten initialisieren
     */
    public void initializeMasterData() {
        logger.info("Starte Stammdaten-Initialisierung...");

        // Vorgang für Stammdaten-Import starten
        Vorgang vorgang = vorgangService.starteAutomatischenVorgang(
                VorgangTyp.DATENIMPORT, "Stammdaten-Import (Benutzer, Adressen, Kunden, Produkte)"
        );

        try {
            int totalOperations = 0;

            // 1. Benutzer initialisieren
            initializeUsers();
            totalOperations++;

            // 2. Adressen initialisieren
            int addressCount = initializeAddresses();
            totalOperations++;

            // 3. Kunden initialisieren
            int customerCount = initializeCustomers();
            totalOperations++;

            // 4. Produkte initialisieren
            int productCount = initializeProducts();
            totalOperations++;

            // Vorgang erfolgreich abschließen
            vorgangService.vorgangErfolgreichAbschliessen(vorgang.getId(),
                    totalOperations, totalOperations, 0, null);

            logger.info("✓ Stammdaten-Initialisierung abgeschlossen: {} Adressen, {} Kunden, {} Produkte",
                    addressCount, customerCount, productCount);

        } catch (Exception e) {
            logger.error("Fehler bei Stammdaten-Initialisierung", e);
            vorgangService.vorgangMitFehlerAbschliessen(vorgang.getId(), e.getMessage());
            throw e;
        }
    }

    /**
     * PRIVATE METHODE: Benutzer für Login erstellen
     */
    private void initializeUsers() {
        logger.info("Initialisiere Login-Benutzer...");

        // Standard Test-Benutzer erstellen
        userDetailsService.createUserSafe("admin", "admin", Role.ROLE_ADMIN);
        userDetailsService.createUserSafe("a", "a", Role.ROLE_ADMIN);
        userDetailsService.createUserSafe("string", "string", Role.ROLE_ADMIN);
        userDetailsService.createUserSafe("user", "user", Role.ROLE_USER);

        logger.debug("✓ Test-Benutzer erstellt");
    }

    /**
     * PRIVATE METHODE: Adressen initialisieren
     *
     * @return Anzahl erstellter Adressen
     */
    private int initializeAddresses() {
        // Prüfen ob bereits Daten vorhanden
        if (addressRepository.count() > 0) {
            logger.info("Adressen bereits vorhanden - überspringe Initialisierung");
            return (int) addressRepository.count();
        }

        logger.info("Initialisiere Adressen...");

        // Deutsche Beispiel-Adressen
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

        // Adressen erstellen und speichern
        for (String[] data : addressData) {
            Address address = new Address(data[0], data[1], data[2], data[3]);
            addressRepository.save(address);
        }

        logger.debug("✓ {} Adressen erstellt", addressData.length);
        return addressData.length;
    }

    /**
     * PRIVATE METHODE: Kunden initialisieren
     *
     * @return Anzahl erstellter Kunden
     */
    private int initializeCustomers() {
        // Prüfen ob bereits Daten vorhanden
        if (customerRepository.count() > 0) {
            logger.info("Kunden bereits vorhanden - überspringe Initialisierung");
            return (int) customerRepository.count();
        }

        logger.info("Initialisiere Kunden...");

        // Verfügbare Adressen holen
        List<Address> addresses = addressRepository.findAll();
        if (addresses.isEmpty()) {
            throw new IllegalStateException("Keine Adressen gefunden - Adressen müssen zuerst initialisiert werden");
        }

        // Deutsche Namen für realistische Testdaten
        String[] firstNames = {"Max", "Anna", "Tom", "Laura", "Paul", "Sophie", "Lukas", "Marie",
                "Felix", "Emma", "Jonas", "Lea", "Ben", "Mia", "Leon", "Hannah"};
        String[] lastNames = {"Müller", "Schmidt", "Schneider", "Fischer", "Weber", "Becker",
                "Hoffmann", "Schäfer", "Koch", "Richter", "Klein", "Wolf"};

        final int numberOfCustomers = 25;

        // Kunden erstellen
        for (int i = 1; i <= numberOfCustomers; i++) {
            String firstName = firstNames[random.nextInt(firstNames.length)];
            String lastName = lastNames[random.nextInt(lastNames.length)];
            String email = firstName.toLowerCase() + "." + lastName.toLowerCase() + i + "@test.com";
            String phoneNumber = "+49" + (random.nextInt(900000000) + 100000000);

            // Kunden-Objekt erstellen
            Customer customer = new Customer(firstName, lastName, email, phoneNumber);
            customer.setCustomerNumber(numberGeneratorService.generateCustomerNumber());

            // Zufällige Adressen zuweisen
            customer.setBillingAddress(addresses.get(random.nextInt(addresses.size())));
            customer.setShippingAddress(addresses.get(random.nextInt(addresses.size())));
            customer.setResidentialAddress(addresses.get(random.nextInt(addresses.size())));

            customerRepository.save(customer);
        }

        logger.debug("✓ {} Kunden erstellt", numberOfCustomers);
        return numberOfCustomers;
    }

    /**
     * PRIVATE METHODE: Produktkatalog initialisieren
     *
     * @return Anzahl erstellter Produkte
     */
    private int initializeProducts() {
        // Prüfen ob bereits Daten vorhanden
        if (productRepository.count() > 0) {
            logger.info("Produkte bereits vorhanden - überspringe Initialisierung");
            return (int) productRepository.count();
        }

        logger.info("Initialisiere Produktkatalog...");

        // Realistische Produkte für ein B2B-ERP-System
        Object[][] products = {
                // Software-Lizenzen (monatliche Abonnements)
                {"Microsoft 365 Business", 18.90, "Monat", "Software"},
                {"Adobe Creative Cloud", 59.99, "Monat", "Software"},
                {"Salesforce CRM Professional", 75.00, "Monat", "Software"},
                {"Slack Business+", 12.50, "Monat", "Software"},
                {"Zoom Pro", 14.99, "Monat", "Software"},

                // Cloud-Services (monatliche Abonnements)
                {"AWS Cloud Hosting", 89.90, "Monat", "Cloud"},
                {"Google Workspace Business", 12.00, "Monat", "Cloud"},
                {"GitHub Enterprise", 21.00, "Monat", "Cloud"},
                {"Dropbox Business", 15.00, "Monat", "Cloud"},

                // Hardware (einmalige Käufe)
                {"Business Laptop Dell", 1200.0, "Stück", "Hardware"},
                {"MacBook Pro 14\"", 2200.0, "Stück", "Hardware"},
                {"iPhone 15 Pro", 1300.0, "Stück", "Hardware"},
                {"iPad Air", 650.0, "Stück", "Hardware"}
        };

        // Produkte erstellen
        for (int i = 0; i < products.length; i++) {
            Product product = new Product(
                    (String) products[i][0],                          // Name
                    BigDecimal.valueOf((Double) products[i][1]),      // Preis
                    (String) products[i][2]                           // Einheit
            );

            // Produktnummer generieren
            product.setProductNumber("PROD-" + String.format("%03d", i + 1));

            // Beschreibung setzen wenn vorhanden
            if (products[i].length > 3) {
                product.setDescription("Kategorie: " + products[i][3]);
            }

            productRepository.save(product);
        }

        logger.debug("✓ {} Produkte erstellt", products.length);
        return products.length;
    }
}
