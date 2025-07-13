package com.erp.backend.adapter.mongo.Inventory;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface MongoInventoryDataRepository extends MongoRepository<InventoryItemDocument, String> {
}
