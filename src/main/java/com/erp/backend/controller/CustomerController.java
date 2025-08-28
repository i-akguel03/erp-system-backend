package com.erp.backend.controller;

import com.erp.backend.domain.Customer;
import com.erp.backend.dto.CustomerDto;
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
    public ResponseEntity<List<CustomerDto>> getAllCustomers(
            @RequestParam(defaultValue = "false") boolean paginated,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "firstName") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDirection) {

        if (paginated) {
            Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
            Pageable pageable = PageRequest.of(page, size, sort);
            Page<Customer> customerPage = service.getAllCustomers(pageable);

            List<CustomerDto> dtos = customerPage.getContent().stream()
                    .map(CustomerDto::fromEntity)
                    .toList();

            return ResponseEntity.ok()
                    .header("X-Total-Count", String.valueOf(customerPage.getTotalElements()))
                    .header("X-Total-Pages", String.valueOf(customerPage.getTotalPages()))
                    .header("X-Current-Page", String.valueOf(page))
                    .body(dtos);
        } else {
            List<CustomerDto> dtos = service.getAllCustomers().stream()
                    .map(CustomerDto::fromEntity)
                    .toList();
            return ResponseEntity.ok(dtos);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerDto> getCustomerById(@PathVariable UUID id) {
        return service.getCustomerById(id)
                .map(CustomerDto::fromEntity)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/by-email/{email}")
    public ResponseEntity<CustomerDto> getCustomerByEmail(@PathVariable String email) {
        return service.getCustomerByEmail(email)
                .map(CustomerDto::fromEntity)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/by-number/{customerNumber}")
    public ResponseEntity<CustomerDto> getCustomerByNumber(@PathVariable String customerNumber) {
        return service.getCustomerByCustomerNumber(customerNumber)
                .map(CustomerDto::fromEntity)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public ResponseEntity<List<CustomerDto>> searchCustomers(@RequestParam String q) {
        List<CustomerDto> dtos = service.searchCustomersByName(q).stream()
                .map(CustomerDto::fromEntity)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/count")
    public ResponseEntity<Long> getCustomerCount() {
        return ResponseEntity.ok(service.getTotalCustomerCount());
    }

    @PostMapping
    public ResponseEntity<CustomerDto> createCustomer(@RequestBody Customer customer) {
        Customer created = service.createCustomer(customer);
        return ResponseEntity.status(HttpStatus.CREATED).body(CustomerDto.fromEntity(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CustomerDto> updateCustomer(@PathVariable UUID id, @RequestBody Customer updated) {
        return service.getCustomerById(id)
                .map(existing -> {
                    existing.setFirstName(updated.getFirstName());
                    existing.setLastName(updated.getLastName());
                    existing.setEmail(updated.getEmail());
                    existing.setTel(updated.getTel());
                    if (updated.getResidentialAddress() != null)
                        existing.setResidentialAddress(updated.getResidentialAddress());
                    if (updated.getBillingAddress() != null)
                        existing.setBillingAddress(updated.getBillingAddress());
                    if (updated.getShippingAddress() != null)
                        existing.setShippingAddress(updated.getShippingAddress());
                    Customer saved = service.updateCustomer(existing);
                    return ResponseEntity.ok(CustomerDto.fromEntity(saved));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable UUID id) {
        service.deleteCustomerById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/validate-email")
    public ResponseEntity<Boolean> validateEmail(@PathVariable UUID id, @RequestParam String email) {
        boolean exists = service.existsByEmail(email);
        if (exists) {
            Optional<Customer> customer = service.getCustomerByEmail(email);
            if (customer.isPresent() && customer.get().getId().equals(id)) {
                exists = false;
            }
        }
        return ResponseEntity.ok(!exists);
    }
}