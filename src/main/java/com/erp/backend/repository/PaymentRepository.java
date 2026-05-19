package com.erp.backend.repository;

import com.erp.backend.domain.Payment;
import com.erp.backend.domain.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    long countByStatus(PaymentStatus status);
}
