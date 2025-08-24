package com.erp.backend.repository;

import com.erp.backend.domain.Customer;
import java.util.List;
import java.util.Optional;

public interface CustomerRepository {
    Customer save(Customer customer);
    List<Customer> findAll();
    Optional<Customer> findById(String id);
    void deleteById(String id);

    boolean existsByCustomerNumber(String s);
}
