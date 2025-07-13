package com.erp.backend.adapter.mongo.customer;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface MongoCustomerDataRepository extends MongoRepository<CustomerDocument, String> {
}
