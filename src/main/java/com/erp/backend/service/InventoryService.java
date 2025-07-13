package com.erp.backend.service;

import com.erp.backend.domain.InventoryItem;
import com.erp.backend.repository.InventoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class InventoryService {

    private final InventoryRepository repository;

    public InventoryService(InventoryRepository repository) {
        this.repository = repository;
    }

    public List<InventoryItem> getAllInventoryItems() {
        return repository.findAll();
    }

    public Optional<InventoryItem> getInventoryItemById(String id) {
        return repository.findById(id);
    }

    public InventoryItem createOrUpdateInventoryItem(InventoryItem item) {
        return repository.save(item);
    }

    public void deleteInventoryItemById(String id) {
        repository.deleteById(id);
    }
}
