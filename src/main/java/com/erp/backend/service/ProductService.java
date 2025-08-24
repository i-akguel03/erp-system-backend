package com.erp.backend.service;

import com.erp.backend.domain.Product;
import com.erp.backend.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository repository;

    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    public List<Product> getAllProducts() {
        List<Product> products = repository.findAll();
        logger.info("Fetched {} products", products.size());
        return products;
    }

    public Optional<Product> getProductById(Long id) {
        Optional<Product> product = repository.findById(id);
        if (product.isPresent()) {
            logger.info("Found product with id={}", id);
        } else {
            logger.warn("No product found with id={}", id);
        }
        return product;
    }

    public Product createProduct(Product product) {
        validate(product);
        product.setId(null); // DB generiert ID
        Product saved = repository.save(product);
        logger.info("Created new product: id={}, name={}, price={}",
                saved.getId(), saved.getName(), saved.getPrice());
        return saved;
    }

    public Product updateProduct(Product product) {
        validate(product);
        if (product.getId() == null || !repository.existsById(product.getId())) {
            throw new IllegalArgumentException("Product not found for update");
        }
        Product saved = repository.save(product);
        logger.info("Updated product: id={}, name={}, price={}",
                saved.getId(), saved.getName(), saved.getPrice());
        return saved;
    }

    public Product createOrUpdateProduct(Product product) {
        if (product.getId() != null && repository.existsById(product.getId())) {
            return updateProduct(product);
        } else {
            return createProduct(product);
        }
    }

    public void deleteProductById(Long id) {
        repository.deleteById(id);
        logger.info("Deleted product with id={}", id);
    }

    private void validate(Product product) {
        if (product.getName() == null || product.getName().isBlank()) {
            throw new IllegalArgumentException("Product name must not be empty");
        }
        if (product.getPrice() < 0) { // nur auf <0 prüfen
            throw new IllegalArgumentException("Product price must be >= 0");
        }
    }

}
