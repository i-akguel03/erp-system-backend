package com.erp.backend.repository;

import com.erp.backend.domain.InventoryItem;
import java.util.List;
import java.util.Optional;

public interface InventoryRepository {
    InventoryItem save(InventoryItem item);
    List<InventoryItem> findAll();
    Optional<InventoryItem> findById(String id);
    void deleteById(String id);
}
