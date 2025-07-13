package com.erp.backend.adapter.mongo.payment;

import com.erp.backend.domain.Payment;
import com.erp.backend.repository.PaymentRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class MongoPaymentRepositoryImpl implements PaymentRepository {

    private final MongoPaymentDataRepository mongoRepo;

    public MongoPaymentRepositoryImpl(MongoPaymentDataRepository mongoRepo) {
        this.mongoRepo = mongoRepo;
    }

    private Payment toDomain(PaymentDocument doc) {
        return new Payment(doc.getId(), doc.getOrderId(), doc.getAmount(), doc.getMethod(), doc.getStatus());
    }

    private PaymentDocument toDocument(Payment payment) {
        return new PaymentDocument(payment.getId(), payment.getOrderId(), payment.getAmount(), payment.getMethod(), payment.getStatus());
    }

    @Override
    public Payment save(Payment payment) {
        return toDomain(mongoRepo.save(toDocument(payment)));
    }

    @Override
    public List<Payment> findAll() {
        return mongoRepo.findAll().stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public Optional<Payment> findById(String id) {
        return mongoRepo.findById(id).map(this::toDomain);
    }

    @Override
    public void deleteById(String id) {
        mongoRepo.deleteById(id);
    }
}
