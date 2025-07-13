package com.erp.backend.adapter.mongo.order;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface MongoOrderDataRepository extends MongoRepository<OrderDocument, String> {
}
