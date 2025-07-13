package com.erp.backend.controller;

import com.erp.backend.domain.InventoryItem;
import com.erp.backend.service.InventoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@CrossOrigin
public class InventoryController {

    private final InventoryService service;

    public InventoryController(InventoryService service) {
        this.service = service;
    }

    @GetMapping
    public List<InventoryItem> getAllInventoryItems() {
        return service.getAllInventoryItems();
    }

    @GetMapping("/{id}")
    public ResponseEntity<InventoryItem> getInventoryItemById(@PathVariable String id) {
        return service.getInventoryItemById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<InventoryItem> createInventoryItem(@RequestBody InventoryItem item) {
        InventoryItem created = service.createOrUpdateInventoryItem(item);
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<InventoryItem> updateInventoryItem(@PathVariable String id, @RequestBody InventoryItem updated) {
        return service.getInventoryItemById(id)
                .map(existing -> {
                    existing.setProductId(updated.getProductId());
                    existing.setQuantity(updated.getQuantity());
                    InventoryItem saved = service.createOrUpdateInventoryItem(existing);
                    return ResponseEntity.ok(saved);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInventoryItem(@PathVariable String id) {
        service.deleteInventoryItemById(id);
        return ResponseEntity.noContent().build();
    }
}
