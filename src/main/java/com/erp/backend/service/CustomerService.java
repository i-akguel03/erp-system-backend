package com.erp.backend.service;

import com.erp.backend.domain.Address;
import com.erp.backend.domain.Customer;
import com.erp.backend.repository.AddressRepository;
import com.erp.backend.repository.CustomerRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class CustomerService {

    private static final Logger logger = LoggerFactory.getLogger(CustomerService.class);

    private final CustomerRepository customerRepository;
    private final AddressRepository addressRepository;

    public CustomerService(CustomerRepository customerRepository, AddressRepository addressRepository) {
        this.customerRepository = customerRepository;
        this.addressRepository = addressRepository;
    }

    public List<Customer> getAllCustomers() {
        List<Customer> customers = customerRepository.findAll();
        logger.info("Fetched {} customers", customers.size());
        return customers;
    }

    public Optional<Customer> getCustomerById(Long id) {
        Optional<Customer> customer = customerRepository.findById(id);
        if (customer.isPresent()) {
            logger.info("Found customer with id={}", id);
        } else {
            logger.warn("No customer found with id={}", id);
        }
        return customer;
    }

    public Customer createCustomer(Customer customer) {
        // Keine ID setzen, wird von DB generiert
        customer.setId(null);

        // Kundennummer generieren
        customer.setCustomerNumber(generateCustomerNumber());

        // Adressen prüfen
        if (customer.getResidentialAddress() != null) {
            Address residential = customer.getResidentialAddress();

            // Wohnadresse speichern
            Address savedResidential = addressRepository.save(residential);
            customer.setResidentialAddress(savedResidential);

            // Falls Billing fehlt, übernehmen
            if (customer.getBillingAddress() == null) {
                Address billingCopy = new Address(
                        savedResidential.getStreet(),
                        savedResidential.getPostalCode(),
                        savedResidential.getCity(),
                        savedResidential.getCountry()
                );
                Address savedBilling = addressRepository.save(billingCopy);
                customer.setBillingAddress(savedBilling);
            }

            // Falls Shipping fehlt, übernehmen
            if (customer.getShippingAddress() == null) {
                Address shippingCopy = new Address(
                        savedResidential.getStreet(),
                        savedResidential.getPostalCode(),
                        savedResidential.getCity(),
                        savedResidential.getCountry()
                );
                Address savedShipping = addressRepository.save(shippingCopy);
                customer.setShippingAddress(savedShipping);
            }
        }

        Customer saved = customerRepository.save(customer);
        logger.info("Created new customer: id={}, customerNumber={}, email={}",
                saved.getId(), saved.getCustomerNumber(), saved.getEmail());
        return saved;
    }

    private String generateCustomerNumber() {
        int min = 60000000;
        int max = 69999999;
        int customerNumber = (int) (Math.random() * (max - min + 1)) + min;

        while (customerRepository.existsByCustomerNumber(String.valueOf(customerNumber))) {
            customerNumber = (int) (Math.random() * (max - min + 1)) + min;
        }

        return String.valueOf(customerNumber);
    }

    public Customer updateCustomer(Customer customer) {
        if (customer.getId() == null || !customerRepository.findById(customer.getId()).isPresent()) {
            throw new IllegalArgumentException("Customer not found for update");
        }
        Customer saved = customerRepository.save(customer);
        logger.info("Updated customer: id={}, email={}", saved.getId(), saved.getEmail());
        return saved;
    }

    public Customer createOrUpdateCustomer(Customer customer) {
        if (customer.getId() != null && customerRepository.findById(customer.getId()).isPresent()) {
            return updateCustomer(customer);
        } else {
            return createCustomer(customer);
        }
    }

    public void deleteCustomerById(Long id) {
        customerRepository.deleteById(id);
        logger.info("Deleted customer with id={}", id);
    }
}
