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

    public Optional<Customer> getCustomerById(String id) {
        Optional<Customer> customer = repository.findById(id);
        if (customer.isPresent()) {
            logger.info("Found customer with id={}", id);
        } else {
            logger.warn("No customer found with id={}", id);
        }
        return customer;
    }

    public Customer createOrUpdateCustomer(Customer customer) {
        boolean isUpdate = customer.getId() != null && repository.findById(customer.getId()).isPresent();
        Customer saved = repository.save(customer);
        if (isUpdate) {
            logger.info("Updated customer: id={}, email={}", saved.getId(), saved.getEmail());
        } else {
            logger.info("Created new customer: id={}, email={}", saved.getId(), saved.getEmail());
        }
        return saved;
    }

    public void deleteCustomerById(String id) {
        repository.deleteById(id);
        logger.info("Deleted customer with id={}", id);
    }
}

