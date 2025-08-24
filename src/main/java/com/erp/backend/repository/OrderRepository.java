package com.erp.backend.repository;

import com.erp.backend.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
    // Zusätzliche Abfragen, falls benötigt:
    // List<Order> findByCustomerId(Long customerId);
}
