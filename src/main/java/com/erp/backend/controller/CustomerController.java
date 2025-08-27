package com.erp.backend.controller;

import com.erp.backend.domain.Customer;
import com.erp.backend.service.CustomerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/customers")
@CrossOrigin
public class CustomerController {

    private static final Logger logger = LoggerFactory.getLogger(CustomerController.class);

    private final CustomerService service;

    public CustomerController(CustomerService service) {
        this.service = service;
    }

    @PostMapping("/init")
    public ResponseEntity<String> initTestCustomers() {
        service.initTestCustomers();
        return ResponseEntity.ok("20 Testkunden wurden erstellt.");
    }

    @GetMapping
    public ResponseEntity<List<Customer>> getAllCustomers(
            @RequestParam(defaultValue = "false") boolean paginated,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "firstName") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDirection) {

        logger.info("GET /api/customers - Fetching customers (paginated: {})", paginated);

        if (paginated) {
            Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
            Pageable pageable = PageRequest.of(page, size, sort);
            Page<Customer> customerPage = service.getAllCustomers(pageable);

            logger.debug("Found {} customers on page {}/{}",
                    customerPage.getNumberOfElements(), page + 1, customerPage.getTotalPages());

            return ResponseEntity.ok()
                    .header("X-Total-Count", String.valueOf(customerPage.getTotalElements()))
                    .header("X-Total-Pages", String.valueOf(customerPage.getTotalPages()))
                    .header("X-Current-Page", String.valueOf(page))
                    .body(customerPage.getContent());
        } else {
            List<Customer> customers = service.getAllCustomers();
            logger.debug("Found {} customers", customers.size());
            return ResponseEntity.ok(customers);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Customer> getCustomerById(@PathVariable UUID id) {
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

    @GetMapping("/by-email/{email}")
    public ResponseEntity<Customer> getCustomerByEmail(@PathVariable String email) {
        logger.info("GET /api/customers/by-email/{} - Fetching customer by email", email);
        return service.getCustomerByEmail(email)
                .map(customer -> {
                    logger.debug("Customer found with email: {}", email);
                    return ResponseEntity.ok(customer);
                })
                .orElseGet(() -> {
                    logger.warn("Customer with email {} not found", email);
                    return ResponseEntity.notFound().build();
                });
    }

    @GetMapping("/by-number/{customerNumber}")
    public ResponseEntity<Customer> getCustomerByNumber(@PathVariable String customerNumber) {
        logger.info("GET /api/customers/by-number/{} - Fetching customer by number", customerNumber);
        return service.getCustomerByCustomerNumber(customerNumber)
                .map(customer -> {
                    logger.debug("Customer found with number: {}", customerNumber);
                    return ResponseEntity.ok(customer);
                })
                .orElseGet(() -> {
                    logger.warn("Customer with number {} not found", customerNumber);
                    return ResponseEntity.notFound().build();
                });
    }

    @GetMapping("/search")
    public ResponseEntity<List<Customer>> searchCustomers(@RequestParam String q) {
        logger.info("GET /api/customers/search?q={} - Searching customers by name", q);
        List<Customer> customers = service.searchCustomersByName(q);
        logger.debug("Found {} customers matching search term: '{}'", customers.size(), q);
        return ResponseEntity.ok(customers);
    }

    @GetMapping("/count")
    public ResponseEntity<Long> getCustomerCount() {
        logger.info("GET /api/customers/count - Fetching customer count");
        long count = service.getTotalCustomerCount();
        logger.debug("Total customer count: {}", count);
        return ResponseEntity.ok(count);
    }

    @PostMapping
    public ResponseEntity<Customer> createCustomer(@RequestBody Customer customer) {
        logger.info("POST /api/customers - Creating new customer with email {}", customer.getEmail());
        logger.debug("RequestBody {}", customer.toString());
        try {
            Customer created = service.createCustomer(customer);
            logger.info("Created customer with ID {} and number {}", created.getId(), created.getCustomerNumber());
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            logger.error("Validation error creating customer: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error creating customer: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Customer> updateCustomer(@PathVariable UUID id, @RequestBody Customer updated) {
        logger.info("PUT /api/customers/{} - Updating customer", id);

        Optional<Customer> existingOpt = service.getCustomerById(id);
        if (existingOpt.isEmpty()) {
            logger.warn("Customer with ID {} not found for update", id);
            return ResponseEntity.notFound().build();
        }

        Customer existing = existingOpt.get();
        logger.debug("Updating fields for customer ID {}", id);

        // Felder aktualisieren
        existing.setFirstName(updated.getFirstName());
        existing.setLastName(updated.getLastName());
        existing.setEmail(updated.getEmail());
        existing.setTel(updated.getTel());

        // Adressen aktualisieren falls vorhanden
        if (updated.getBillingAddress() != null) {
            existing.setBillingAddress(updated.getBillingAddress());
        }
        if (updated.getShippingAddress() != null) {
            existing.setShippingAddress(updated.getShippingAddress());
        }
        if (updated.getResidentialAddress() != null) {
            existing.setResidentialAddress(updated.getResidentialAddress());
        }

        try {
            Customer saved = service.updateCustomer(existing);
            logger.info("Updated customer with ID {}", saved.getId());
            return ResponseEntity.ok(saved);
        } catch (IllegalArgumentException e) {
            logger.error("Validation error updating customer: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error updating customer: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable UUID id) {
        logger.info("DELETE /api/customers/{} - Deleting customer", id);
        try {
            service.deleteCustomerById(id);
            logger.info("Deleted customer with ID {}", id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            logger.error("Customer not found for deletion: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error deleting customer: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{id}/validate-email")
    public ResponseEntity<Boolean> validateEmail(@PathVariable UUID id, @RequestParam String email) {
        logger.info("POST /api/customers/{}/validate-email - Validating email uniqueness", id);
        boolean exists = service.existsByEmail(email);

        // Wenn der Kunde existiert, prüfen ob es der gleiche Kunde ist
        if (exists) {
            Optional<Customer> customer = service.getCustomerByEmail(email);
            if (customer.isPresent() && customer.get().getId().equals(id)) {
                exists = false; // Email gehört zum gleichen Kunden
            }
        }

        return ResponseEntity.ok(!exists); // true wenn Email verfügbar
    }
}