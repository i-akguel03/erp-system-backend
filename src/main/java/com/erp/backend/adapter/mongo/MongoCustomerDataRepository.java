package com.erp.backend.adapter.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface MongoCustomerDataRepository extends MongoRepository<CustomerDocument, String> {
}
