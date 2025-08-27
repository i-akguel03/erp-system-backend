package com.erp.backend.repository;

import com.erp.backend.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    boolean existsByCustomerNumber(String customerNumber);

    boolean existsByEmail(String email);

    Optional<Customer> findByEmail(String email);

    Optional<Customer> findByCustomerNumber(String customerNumber);

    // Suche nach Vor- oder Nachnamen (Case-Insensitive)
    java.util.List<Customer> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
            String firstName,
            String lastName
    );
}
