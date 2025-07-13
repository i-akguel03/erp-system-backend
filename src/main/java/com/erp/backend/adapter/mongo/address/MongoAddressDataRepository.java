package com.erp.backend.adapter.mongo.address;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface MongoAddressDataRepository extends MongoRepository<AddressDocument, String> {
}
