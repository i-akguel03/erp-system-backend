package com.erp.backend.adapter.mongo.product;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface MongoProductDataRepository extends MongoRepository<ProductDocument, String> {
}
