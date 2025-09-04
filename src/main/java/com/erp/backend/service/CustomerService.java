package com.erp.backend.service;

import com.erp.backend.domain.Address;
import com.erp.backend.domain.ContractStatus;
import com.erp.backend.domain.Customer;
import com.erp.backend.repository.AddressRepository;
import com.erp.backend.repository.CustomerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Transactional
public class CustomerService {

    private static final Logger logger = LoggerFactory.getLogger(CustomerService.class);

    private final CustomerRepository customerRepository;
    private final AddressRepository addressRepository;

    public CustomerService(CustomerRepository customerRepository, AddressRepository addressRepository) {
        this.customerRepository = customerRepository;
        this.addressRepository = addressRepository;
    }

    @Transactional
    public void initTestCustomers() {
        if (customerRepository.count() > 0) return; // nur einmal

        Random random = new Random();

        String[] firstNames = {"Max", "Anna", "Tom", "Laura", "Paul", "Sophie", "Lukas", "Marie", "Felix", "Emma"};
        String[] lastNames = {"Müller", "Schmidt", "Schneider", "Fischer", "Weber", "Becker", "Hoffmann", "Schäfer", "Koch", "Richter"};

        List<Address> allAddresses = addressRepository.findAll(); // alle Adressen aus DB

        if (allAddresses.size() < 3) {
            throw new IllegalStateException("Es müssen mindestens 3 Adressen in der Datenbank vorhanden sein.");
        }

        for (int i = 1; i <= 20; i++) {
            String firstName = firstNames[random.nextInt(firstNames.length)];
            String lastName = lastNames[random.nextInt(lastNames.length)];
            String email = firstName.toLowerCase() + "." + lastName.toLowerCase() + i + "@test.com";
            String tel = "+49" + (random.nextInt(900000000) + 100000000);

            Customer customer = new Customer(firstName, lastName, email, tel);
            customer.setCustomerNumber("CUST-" + String.format("%04d", i));

            // Zufällige Adressen aus der DB auswählen
            Address billing = allAddresses.get(random.nextInt(allAddresses.size()));
            Address shipping = allAddresses.get(random.nextInt(allAddresses.size()));
            Address residential = allAddresses.get(random.nextInt(allAddresses.size()));

            customer.setBillingAddress(billing);
            customer.setShippingAddress(shipping);
            customer.setResidentialAddress(residential);

            customerRepository.save(customer);
        }
    }



    @Transactional(readOnly = true)
    public List<Customer> getAllCustomers() {
        List<Customer> customers = customerRepository.findAll();
        logger.info("Fetched {} customers", customers.size());
        return customers;
    }

    @Transactional(readOnly = true)
    public Page<Customer> getAllCustomers(Pageable pageable) {
        Page<Customer> customers = customerRepository.findAll(pageable);
        logger.info("Fetched {} customers (page {}/{})",
                customers.getNumberOfElements(), customers.getNumber() + 1, customers.getTotalPages());
        return customers;
    }

    @Transactional(readOnly = true)
    public Optional<Customer> getCustomerById(UUID id) {
        Optional<Customer> customer = customerRepository.findById(id);
        if (customer.isPresent()) {
            logger.info("Found customer with id={}", id);
        } else {
            logger.warn("No customer found with id={}", id);
        }
        return customer;
    }

    @Transactional(readOnly = true)
    public Optional<Customer> getCustomerByEmail(String email) {
        Optional<Customer> customer = customerRepository.findByEmail(email);
        logger.info("Search for customer with email={}: {}", email, customer.isPresent() ? "found" : "not found");
        return customer;
    }

    @Transactional(readOnly = true)
    public Optional<Customer> getCustomerByCustomerNumber(String customerNumber) {
        Optional<Customer> customer = customerRepository.findByCustomerNumber(customerNumber);
        logger.info("Search for customer with number={}: {}", customerNumber, customer.isPresent() ? "found" : "not found");
        return customer;
    }

    @Transactional(readOnly = true)
    public List<Customer> searchCustomersByName(String searchTerm) {
        List<Customer> customers = customerRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(searchTerm, searchTerm);
        logger.info("Found {} customers matching name search: '{}'", customers.size(), searchTerm);
        return customers;
    }

    public Customer createCustomer(Customer customer) {
        // Validierung
        validateCustomerForCreation(customer);

        // Keine ID setzen, wird von DB generiert
        customer.setId(null);

        // Kundennummer generieren
        customer.setCustomerNumber(generateCustomerNumber());

        // Adressen verarbeiten
        processCustomerAddresses(customer);

        Customer saved = customerRepository.save(customer);
        logger.info("Created new customer: id={}, customerNumber={}, email={}",
                saved.getId(), saved.getCustomerNumber(), saved.getEmail());
        return saved;
    }

    public Customer updateCustomer(Customer customer) {
        if (customer.getId() == null) {
            throw new IllegalArgumentException("Customer ID cannot be null for update");
        }

        Customer existingCustomer = customerRepository.findById(customer.getId())
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with ID: " + customer.getId()));

        // Email-Eindeutigkeit prüfen (außer bei gleichem Kunden)
        if (!existingCustomer.getEmail().equals(customer.getEmail())) {
            validateEmailUniqueness(customer.getEmail());
        }

        Customer saved = customerRepository.save(customer);
        logger.info("Updated customer: id={}, email={}", saved.getId(), saved.getEmail());
        return saved;
    }

    public Customer createOrUpdateCustomer(Customer customer) {
        if (customer.getId() != null && customerRepository.existsById(customer.getId())) {
            return updateCustomer(customer);
        } else {
            return createCustomer(customer);
        }
    }

    public void deleteCustomerById(UUID id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with ID: " + id));

        // Prüfen, ob aktive Verträge vorhanden sind
        boolean hasActiveContracts = customer.getContracts().stream()
                .anyMatch(contract -> contract.getContractStatus().equals(ContractStatus.ACTIVE)); // oder contract.getStatus().equals("ACTIVE")

        if (hasActiveContracts) {
            throw new IllegalStateException("Cannot delete customer with active contracts (id=" + id + ")");
        }

        customerRepository.deleteById(id);
        logger.info("Soft-deleted customer with id={}", id);
    }


    @Transactional(readOnly = true)
    public long getTotalCustomerCount() {
        long count = customerRepository.count();
        logger.info("Total customer count: {}", count);
        return count;
    }

    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return customerRepository.existsByEmail(email);
    }

    @Transactional(readOnly = true)
    public boolean existsByCustomerNumber(String customerNumber) {
        return customerRepository.existsByCustomerNumber(customerNumber);
    }

    // Private Hilfsmethoden

    private void validateCustomerForCreation(Customer customer) {
        if (customer.getFirstName() == null || customer.getFirstName().trim().isEmpty()) {
            throw new IllegalArgumentException("First name is required");
        }
        if (customer.getLastName() == null || customer.getLastName().trim().isEmpty()) {
            throw new IllegalArgumentException("Last name is required");
        }
        if (customer.getEmail() == null || customer.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }

        validateEmailUniqueness(customer.getEmail());
    }

    private void validateEmailUniqueness(String email) {
        if (customerRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Customer with email already exists: " + email);
        }
    }

    private void processCustomerAddresses(Customer customer) {
        if (customer.getResidentialAddress() != null) {
            Address residential = customer.getResidentialAddress();
            residential.setId(null); // Für neue Adresse
            Address savedResidential = addressRepository.save(residential);
            customer.setResidentialAddress(savedResidential);

            // Falls Billing fehlt, Wohnadresse übernehmen
            if (customer.getBillingAddress() == null) {
                customer.setBillingAddress(copyAddress(savedResidential));
            } else {
                customer.getBillingAddress().setId(null);
                customer.setBillingAddress(addressRepository.save(customer.getBillingAddress()));
            }

            // Falls Shipping fehlt, Wohnadresse übernehmen
            if (customer.getShippingAddress() == null) {
                customer.setShippingAddress(copyAddress(savedResidential));
            } else {
                customer.getShippingAddress().setId(null);
                customer.setShippingAddress(addressRepository.save(customer.getShippingAddress()));
            }
        }
    }

    private Address copyAddress(Address original) {
        Address copy = new Address(
                original.getStreet(),
                original.getPostalCode(),
                original.getCity(),
                original.getCountry()
        );
        return addressRepository.save(copy);
    }

    private String generateCustomerNumber() {
        int min = 60000000;
        int max = 69999999;
        String customerNumber;

        do {
            int number = (int) (Math.random() * (max - min + 1)) + min;
            customerNumber = String.valueOf(number);
        } while (customerRepository.existsByCustomerNumber(customerNumber));

        return customerNumber;
    }
}