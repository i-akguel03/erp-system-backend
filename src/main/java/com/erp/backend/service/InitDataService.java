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
    private final InvoiceService invoiceService;

    public InitDataService(AddressRepository addressRepository,
                           CustomerRepository customerRepository,
                           ProductRepository productRepository,
                           ContractRepository contractRepository,
                           SubscriptionRepository subscriptionRepository,
                           InvoiceService invoiceService) {
        this.addressRepository = addressRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.contractRepository = contractRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.invoiceService = invoiceService;
    }

    @Transactional
    public void initAllData() {
        initAddresses();
        initCustomers();
        initProducts();
        initContracts();
        initSubscriptions();
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
        String[] firstNames = {"Max", "Anna", "Tom", "Laura", "Paul", "Sophie", "Lukas", "Marie", "Felix", "Emma"};
        String[] lastNames = {"Müller", "Schmidt", "Schneider", "Fischer", "Weber", "Becker", "Hoffmann", "Schäfer", "Koch", "Richter"};

        for (int i = 1; i <= 20; i++) {
            Customer customer = new Customer(
                    firstNames[random.nextInt(firstNames.length)],
                    lastNames[random.nextInt(lastNames.length)],
                    "customer" + i + "@test.com",
                    "+49" + (random.nextInt(900000000) + 100000000)
            );
            customer.setCustomerNumber("CUST-" + String.format("%04d", i));
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
                {"Adobe Photoshop Lizenz", 20.0, "Monat"},
                {"Microsoft 365 Lizenz", 15.0, "Monat"},
                {"AWS Cloud Hosting", 100.0, "Monat"},
                {"GitHub Copilot", 10.0, "Monat"},
                {"Tisch „Ikea Bekant“", 200.0, "Stück"},
                {"Bürostuhl „Herman Miller“", 800.0, "Stück"},
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

        for (int i = 1; i <= 15; i++) {
            Customer customer = customers.get(random.nextInt(customers.size()));
            Contract contract = new Contract("Contract " + i, LocalDate.now().minusDays(random.nextInt(100)), customer);
            contract.setContractNumber("CON-" + String.format("%04d", i));
            contractRepository.save(contract);
        }
    }

    // --- 5. Abonnements ---
    private void initSubscriptions() {
        if (subscriptionRepository.count() > 0) return;
        List<Contract> contracts = contractRepository.findAll();
        List<Product> products = productRepository.findAll();
        Random random = new Random();

        for (int i = 1; i <= 30; i++) {
            Contract contract = contracts.get(random.nextInt(contracts.size()));
            Product product = products.get(random.nextInt(products.size()));
            Subscription sub = new Subscription(product.getName(), product.getPrice(), LocalDate.now().minusDays(random.nextInt(30)), contract);
            sub.setSubscriptionNumber("SUB-" + String.format("%04d", i));
            contract.addSubscription(sub);
            subscriptionRepository.save(sub);
        }
    }

    // --- 6. Rechnungen ---
    private void initInvoices() {
        List<Customer> customers = customerRepository.findAll();
        Random random = new Random();
        int invoiceCounter = 1;

        for (int i = 0; i < 40; i++) {
            Customer customer = customers.get(random.nextInt(customers.size()));
            Invoice invoice = new Invoice();
            invoice.setCustomer(customer);
            invoice.setBillingAddress(customer.getBillingAddress());
            invoice.setInvoiceNumber("INV-" + String.format("%04d", invoiceCounter++));
            invoice.setInvoiceDate(LocalDate.now().minusDays(random.nextInt(30)));
            invoice.setDueDate(invoice.getInvoiceDate().plusDays(30));

            // Alle Subscriptions des Kunden holen
            List<Contract> contracts = contractRepository.findByCustomer(customer);
            int positionCounter = 1;

            for (Contract contract : contracts) {
                for (Subscription sub : contract.getSubscriptions()) {
                    if (random.nextBoolean()) {
                        InvoiceItem item = new InvoiceItem(sub.getProductName(), sub.getMonthlyPrice(), "Stück", sub.getMonthlyPrice());
                        item.setPosition(positionCounter++); // Position setzen
                        invoice.addInvoiceItem(item);
                    }
                }
            }

            invoiceService.createInvoice(invoice);
        }
    }
}
