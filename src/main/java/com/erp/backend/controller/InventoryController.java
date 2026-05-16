package com.erp.backend.controller;

import com.erp.backend.domain.InventoryItem;
import com.erp.backend.domain.Product;
import com.erp.backend.service.InventoryService;
import com.erp.backend.service.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'INVENTORY_READ')")
    @GetMapping
    public ResponseEntity<List<InventoryItem>> getAllInventoryItems(
            @RequestParam(defaultValue = "false") boolean paginated,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDirection) {

        if (paginated) {
            Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
            Pageable pageable = PageRequest.of(page, size, sort);
            Page<InventoryItem> itemPage = inventoryService.getAllInventoryItems(pageable);
            return ResponseEntity.ok()
                    .header("X-Total-Count", String.valueOf(itemPage.getTotalElements()))
                    .header("X-Total-Pages", String.valueOf(itemPage.getTotalPages()))
                    .header("X-Current-Page", String.valueOf(page))
                    .body(itemPage.getContent());
        }
        return ResponseEntity.ok(inventoryService.getAllInventoryItems());
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'INVENTORY_READ')")
    @GetMapping("/{id}")
    public ResponseEntity<InventoryItem> getInventoryItemById(@PathVariable Long id) {
        return inventoryService.getInventoryItemById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<InventoryItem> createInventoryItem(@RequestBody InventoryItem item) {
        // Produkt aus DB laden
        Product product = productService.getProductById(item.getProduct().getId())
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        item.setProduct(product);

        InventoryItem created = inventoryService.createOrUpdateInventoryItem(item);
        return ResponseEntity.ok(created);
    }

    @PreAuthorize("hasRole('ADMIN')")
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

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInventoryItem(@PathVariable Long id) {
        inventoryService.deleteInventoryItemById(id);
        return ResponseEntity.noContent().build();
    }
}
