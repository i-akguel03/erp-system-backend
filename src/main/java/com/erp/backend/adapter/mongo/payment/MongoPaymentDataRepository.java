package com.erp.backend.adapter.mongo.payment;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface MongoPaymentDataRepository extends MongoRepository<PaymentDocument, String> {
}
