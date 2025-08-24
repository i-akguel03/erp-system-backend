package com.erp.backend.service;

import com.erp.backend.domain.Payment;
import com.erp.backend.repository.PaymentRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PaymentService {

    private final PaymentRepository repository;

    public PaymentService(PaymentRepository repository) {
        this.repository = repository;
    }

    public List<Payment> getAllPayments() {
        return repository.findAll();
    }

    public Optional<Payment> getPaymentById(Long id) {
        return repository.findById(id);
    }

    public Payment createOrUpdatePayment(Payment payment) {
        return repository.save(payment);
    }

    public void deletePaymentById(Long id) {
        repository.deleteById(id);
    }
}
