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
    private final InvoiceService invoiceService; // zum Erstellen von Rechnungen

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
        if (addressRepository.count() > 0) return;
        for (int i = 1; i <= 30; i++) {
            Address address = new Address("Street " + i, "1000" + i, "City" + i, "Germany");
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
        if (productRepository.count() > 0) return;
        for (int i = 1; i <= 15; i++) {
            Product product = new Product("Product " + i, BigDecimal.valueOf(10 + i), "Stück");
            product.setProductNumber("PROD-" + String.format("%03d", i));
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
            customer.addContract(contract);
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

            // Füge zufällige InvoiceItems aus Subscriptions des Kunden hinzu
            customer.getContracts().forEach(contract -> {
                contract.getSubscriptions().forEach(sub -> {
                    if (random.nextBoolean()) {
                        InvoiceItem item = new InvoiceItem(sub.getProductName(), sub.getMonthlyPrice(), "Stück", sub.getMonthlyPrice());
                        invoice.addInvoiceItem(item);
                    }
                });
            });

            invoiceService.createInvoice(invoice);
        }
    }
}
