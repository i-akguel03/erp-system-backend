package com.erp.backend.repository;

import com.erp.backend.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    // Zusätzliche Abfragen, falls benötigt:
    // List<Payment> findByCustomerId(Long customerId);
}
