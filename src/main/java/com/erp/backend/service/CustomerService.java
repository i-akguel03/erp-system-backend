package com.erp.backend.service;

import com.erp.backend.domain.Customer;
import com.erp.backend.repository.CustomerRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CustomerService {

    private final CustomerRepository repository;

    public CustomerService(CustomerRepository repository) {
        this.repository = repository;
    }

    public List<Customer> getAllCustomers() {
        return repository.findAll();
    }

    public Optional<Customer> getCustomerById(String id) {
        return repository.findById(id);
    }

    public Customer createOrUpdateCustomer(Customer customer) {
        return repository.save(customer);
    }

    public void deleteCustomerById(String id) {
        repository.deleteById(id);
    }
}
