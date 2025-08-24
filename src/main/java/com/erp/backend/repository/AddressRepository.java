package com.erp.backend.repository;

import com.erp.backend.domain.Address;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AddressRepository extends JpaRepository<Address, Long> {
    // Zusätzliche Abfragen bei Bedarf:
    // Optional<Address> findByZipCode(String zipCode);
}