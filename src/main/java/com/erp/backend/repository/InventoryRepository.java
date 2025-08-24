package com.erp.backend.repository;

import com.erp.backend.domain.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryRepository extends JpaRepository<InventoryItem, Long> {
    // Zusätzliche Abfragen bei Bedarf:
    // Optional<InventoryItem> findBySku(String sku);
}
