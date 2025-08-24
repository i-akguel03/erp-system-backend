package com.erp.backend.repository;

import com.erp.backend.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    boolean existsByCustomerNumber(String customerNumber);
}