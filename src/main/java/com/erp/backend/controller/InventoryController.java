package com.erp.backend.controller;

import com.erp.backend.domain.InventoryItem;
import com.erp.backend.domain.Product;
import com.erp.backend.service.InventoryService;
import com.erp.backend.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@CrossOrigin
public class InventoryController {

    private final InventoryService inventoryService;
    private final ProductService productService; // um Produkte zu laden

    public InventoryController(InventoryService inventoryService, ProductService productService) {
        this.inventoryService = inventoryService;
        this.productService = productService;
    }

    @GetMapping
    public List<InventoryItem> getAllInventoryItems() {
        return inventoryService.getAllInventoryItems();
    }

    @GetMapping("/{id}")
    public ResponseEntity<InventoryItem> getInventoryItemById(@PathVariable Long id) {
        return inventoryService.getInventoryItemById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<InventoryItem> createInventoryItem(@RequestBody InventoryItem item) {
        // Produkt aus DB laden
        Product product = productService.getProductById(item.getProduct().getId())
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        item.setProduct(product);

        InventoryItem created = inventoryService.createOrUpdateInventoryItem(item);
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<InventoryItem> updateInventoryItem(@PathVariable Long id, @RequestBody InventoryItem updated) {
        return inventoryService.getInventoryItemById(id)
                .map(existing -> {
                    // Produkt aus DB laden
                    Product product = productService.getProductById(updated.getProduct().getId())
                            .orElseThrow(() -> new IllegalArgumentException("Product not found"));
                    existing.setProduct(product);
                    existing.setQuantity(updated.getQuantity());

                    InventoryItem saved = inventoryService.createOrUpdateInventoryItem(existing);
                    return ResponseEntity.ok(saved);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInventoryItem(@PathVariable Long id) {
        inventoryService.deleteInventoryItemById(id);
        return ResponseEntity.noContent().build();
    }
}
