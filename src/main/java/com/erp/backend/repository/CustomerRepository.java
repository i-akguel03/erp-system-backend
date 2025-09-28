package com.erp.backend.repository;

import com.erp.backend.domain.Address;
import com.erp.backend.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    boolean existsByCustomerNumber(String customerNumber);

    boolean existsByEmail(String email);

    Optional<Customer> findByEmail(String email);

    Optional<Customer> findByCustomerNumber(String customerNumber);

    /**
     * Zählt alle Customers, die eine bestimmte Adresse verwenden
     */
    long countByResidentialAddress(Address address);

    /**
     * Findet alle Customers, die eine bestimmte Adresse verwenden
     */
    List<Customer> findByResidentialAddress(Address address);


//    /**
//     * Alternative mit expliziter Query (falls die automatische nicht funktioniert)
//     */
//    @Query("SELECT COUNT(c) FROM Customer c WHERE c.address = :address")
//    long countCustomersByResidentialAddress(@Param("address") Address address);
//
//    /**
//     * Alternative mit expliziter Query für findByAddress
//     */
//    @Query("SELECT c FROM Customer c WHERE c.address = :address")
//    List<Customer> findCustomersByResidentialAddress(@Param("address") Address address);

    // Suche nach Vor- oder Nachnamen (Case-Insensitive)
    java.util.List<Customer> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
            String firstName,
            String lastName
    );
}
