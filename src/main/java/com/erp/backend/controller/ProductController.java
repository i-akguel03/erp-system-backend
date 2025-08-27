package com.erp.backend.controller;

import com.erp.backend.domain.Product;
import com.erp.backend.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/products")
@CrossOrigin
public class ProductController {

    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts(
            @RequestParam(defaultValue = "false") boolean paginated,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDirection) {

        logger.info("GET /api/products - Fetching products (paginated: {})", paginated);

        if (paginated) {
            Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
            Pageable pageable = PageRequest.of(page, size, sort);
            List<Product> all = service.getAllProducts();
            Page<Product> productPage = new PageImpl<>(all, pageable, all.size());

            logger.debug("Found {} products on page {}/{}",
                    productPage.getNumberOfElements(), page + 1, productPage.getTotalPages());

            return ResponseEntity.ok()
                    .header("X-Total-Count", String.valueOf(productPage.getTotalElements()))
                    .header("X-Total-Pages", String.valueOf(productPage.getTotalPages()))
                    .header("X-Current-Page", String.valueOf(page))
                    .body(productPage.getContent());
        } else {
            List<Product> products = service.getAllProducts();
            logger.debug("Found {} products", products.size());
            return ResponseEntity.ok(products);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable UUID id) {
        logger.info("GET /api/products/{} - Fetching product by ID", id);
        return service.getProductById(id)
                .map(product -> {
                    logger.debug("Product found: {}", product.getName());
                    return ResponseEntity.ok(product);
                })
                .orElseGet(() -> {
                    logger.warn("Product with ID {} not found", id);
                    return ResponseEntity.notFound().build();
                });
    }

    @GetMapping("/by-number/{productNumber}")
    public ResponseEntity<Product> getProductByNumber(@PathVariable String productNumber) {
        logger.info("GET /api/products/by-number/{} - Fetching product by productNumber", productNumber);
        return service.getProductByProductNumber(productNumber)
                .map(product -> {
                    logger.debug("Product found with productNumber: {}", productNumber);
                    return ResponseEntity.ok(product);
                })
                .orElseGet(() -> {
                    logger.warn("Product with productNumber {} not found", productNumber);
                    return ResponseEntity.notFound().build();
                });
    }

    @GetMapping("/search")
    public ResponseEntity<List<Product>> searchProducts(@RequestParam String q) {
        logger.info("GET /api/products/search?q={} - Searching products by name", q);
        List<Product> products = service.searchProductsByName(q);
        logger.debug("Found {} products matching search term: '{}'", products.size(), q);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/count")
    public ResponseEntity<Long> getProductCount() {
        logger.info("GET /api/products/count - Fetching product count");
        long count = service.getTotalProductCount();
        logger.debug("Total product count: {}", count);
        return ResponseEntity.ok(count);
    }

    @PostMapping
    public ResponseEntity<Product> createProduct(@RequestBody Product product) {
        logger.info("POST /api/products - Creating new product with name {}", product.getName());
        logger.debug("RequestBody {}", product.toString());
        try {
            Product created = service.createProduct(product);
            logger.info("Created product with ID {} and productNumber {}", created.getId(), created.getProductNumber());
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            logger.error("Validation error creating product: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error creating product: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(@PathVariable UUID id, @RequestBody Product updated) {
        logger.info("PUT /api/products/{} - Updating product", id);

        Optional<Product> existingOpt = service.getProductById(id);
        if (existingOpt.isEmpty()) {
            logger.warn("Product with ID {} not found for update", id);
            return ResponseEntity.notFound().build();
        }

        Product existing = existingOpt.get();
        logger.debug("Updating fields for product ID {}", id);

        existing.setName(updated.getName());
        existing.setDescription(updated.getDescription());
        existing.setPrice(updated.getPrice());
        existing.setUnit(updated.getUnit());
        existing.setTaxRate(updated.getTaxRate());
        existing.setProductType(updated.getProductType());
        existing.setActive(updated.getActive());

        try {
            Product saved = service.updateProduct(existing);
            logger.info("Updated product with ID {}", saved.getId());
            return ResponseEntity.ok(saved);
        } catch (IllegalArgumentException e) {
            logger.error("Validation error updating product: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error updating product: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable UUID id) {
        logger.info("DELETE /api/products/{} - Deleting product", id);
        try {
            service.deleteProductById(id);
            logger.info("Deleted product with ID {}", id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            logger.error("Product not found for deletion: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error deleting product: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
