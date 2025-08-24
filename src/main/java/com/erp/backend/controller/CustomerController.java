package com.erp.backend.controller;

import com.erp.backend.domain.Customer;
import com.erp.backend.service.CustomerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/customers")
@CrossOrigin
public class CustomerController {

    private static final Logger logger = LoggerFactory.getLogger(CustomerController.class);

    private final CustomerService service;

    public CustomerController(CustomerService service) {
        this.service = service;
    }

    @GetMapping
    public List<Customer> getAllCustomers() {
        logger.info("GET /api/customers - Fetching all customers");
        List<Customer> customers = service.getAllCustomers();
        logger.debug("Found {} customers", customers.size());
        return customers;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Customer> getCustomerById(@PathVariable Long id) {
        logger.info("GET /api/customers/{} - Fetching customer by ID", id);
        return service.getCustomerById(id)
                .map(customer -> {
                    logger.debug("Customer found: {}", customer.getEmail());
                    return ResponseEntity.ok(customer);
                })
                .orElseGet(() -> {
                    logger.warn("Customer with ID {} not found", id);
                    return ResponseEntity.notFound().build();
                });
    }

    @PostMapping
    public ResponseEntity<Customer> createCustomer(@RequestBody Customer customer) {
        logger.info("POST /api/customers - Creating new customer with email {}", customer.getEmail());
        logger.info("RequestBody {}", customer.toString());
        try {
            Customer created = service.createCustomer(customer);
            logger.debug("Created customer with ID {}", created.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (Exception e) {
            logger.error("Error creating customer: {}", e.getMessage());
            return ResponseEntity.<Customer>badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Customer> updateCustomer(@PathVariable Long id, @RequestBody Customer updated) {
        logger.info("PUT /api/customers/{} - Updating customer", id);

        Optional<Customer> existingOpt = service.getCustomerById(id);
        if (existingOpt.isEmpty()) {
            logger.warn("Customer with ID {} not found for update", id);
            return ResponseEntity.notFound().build();
        }

        Customer existing = existingOpt.get();
        logger.debug("Updating fields for customer ID {}", id);
        existing.setFirstName(updated.getFirstName());
        existing.setLastName(updated.getLastName());
        existing.setEmail(updated.getEmail());
        existing.setTel(updated.getTel());

        try {
            Customer saved = service.updateCustomer(existing);
            logger.info("Updated customer with ID {}", saved.getId());
            return ResponseEntity.ok(saved);
        } catch (IllegalArgumentException e) {
            logger.error("Error updating customer: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable Long id) {
        logger.info("DELETE /api/customers/{} - Deleting customer", id);
        service.deleteCustomerById(id);
        logger.debug("Deleted customer with ID {}", id);
        return ResponseEntity.noContent().build();
    }
}
