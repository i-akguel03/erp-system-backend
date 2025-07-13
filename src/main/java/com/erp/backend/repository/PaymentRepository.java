package com.erp.backend.repository;

import com.erp.backend.domain.Payment;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository {
    Payment save(Payment payment);
    List<Payment> findAll();
    Optional<Payment> findById(String id);
    void deleteById(String id);
}
