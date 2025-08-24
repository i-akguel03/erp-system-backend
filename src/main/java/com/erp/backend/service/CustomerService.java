package com.erp.backend.service;

import com.erp.backend.domain.Customer;
import com.erp.backend.repository.CustomerRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class CustomerService {

    private static final Logger logger = LoggerFactory.getLogger(CustomerService.class);
    private final CustomerRepository repository;

    public CustomerService(CustomerRepository repository) {
        this.repository = repository;
    }

    public List<Customer> getAllCustomers() {
        List<Customer> customers = repository.findAll();
        logger.info("Fetched {} customers", customers.size());
        return customers;
    }

    public Optional<Customer> getCustomerById(Long id) {
        Optional<Customer> customer = repository.findById(id);
        if (customer.isPresent()) {
            logger.info("Found customer with id={}", id);
        } else {
            logger.warn("No customer found with id={}", id);
        }
        return customer;
    }

    public Customer createCustomer(Customer customer) {
        // Sicherstellen, dass keine ID mitgegeben wird (wird von DB generiert)
        customer.setId(null);

        // 8-stellige Kundennummer generieren
        customer.setCustomerNumber(generateCustomerNumber());

        Customer saved = repository.save(customer);
        logger.info("Created new customer: id={}, customerNumber={}, email={}",
                saved.getId(), saved.getCustomerNumber(), saved.getEmail());
        return saved;
    }

    private String generateCustomerNumber() {
        int min = 60000000;
        int max = 69999999;
        int customerNumber = (int) (Math.random() * (max - min + 1)) + min;

        while (repository.existsByCustomerNumber(String.valueOf(customerNumber))) {
            customerNumber = (int) (Math.random() * (max - min + 1)) + min;
        }

        return String.valueOf(customerNumber);
    }

    public Customer updateCustomer(Customer customer) {
        if (customer.getId() == null || !repository.findById(customer.getId()).isPresent()) {
            throw new IllegalArgumentException("Customer not found for update");
        }
        Customer saved = repository.save(customer);
        logger.info("Updated customer: id={}, email={}", saved.getId(), saved.getEmail());
        return saved;
    }

    public Customer createOrUpdateCustomer(Customer customer) {
        if (customer.getId() != null && repository.findById(customer.getId()).isPresent()) {
            return updateCustomer(customer);
        } else {
            return createCustomer(customer);
        }
    }

    public void deleteCustomerById(Long id) {
        repository.deleteById(id);
        logger.info("Deleted customer with id={}", id);
    }
}
