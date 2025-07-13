package com.erp.backend.adapter.mongo.Inventory;

import com.erp.backend.domain.InventoryItem;
import com.erp.backend.repository.InventoryRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class MongoInventoryRepositoryImpl implements InventoryRepository {

    private final MongoInventoryDataRepository mongoRepo;

    public MongoInventoryRepositoryImpl(MongoInventoryDataRepository mongoRepo) {
        this.mongoRepo = mongoRepo;
    }

    private InventoryItem toDomain(InventoryItemDocument doc) {
        return new InventoryItem(doc.getId(), doc.getProductId(), doc.getQuantity());
    }

    private InventoryItemDocument toDocument(InventoryItem item) {
        return new InventoryItemDocument(item.getId(), item.getProductId(), item.getQuantity());
    }

    @Override
    public InventoryItem save(InventoryItem item) {
        return toDomain(mongoRepo.save(toDocument(item)));
    }

    @Override
    public List<InventoryItem> findAll() {
        return mongoRepo.findAll().stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public Optional<InventoryItem> findById(String id) {
        return mongoRepo.findById(id).map(this::toDomain);
    }

    @Override
    public void deleteById(String id) {
        mongoRepo.deleteById(id);
    }
}
