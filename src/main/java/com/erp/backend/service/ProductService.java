package com.erp.backend.service;

import com.erp.backend.domain.Product;
import com.erp.backend.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

@Service
public class ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository repository;

    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    public void initTestProducts() {

        final Random random = new Random();

        if (repository.count() > 0) return; // nur einmal

        String[] productNames = {
                "Laptop", "Smartphone", "Monitor", "Tastatur", "Maus",
                "Drucker", "Scanner", "Router", "Webcam", "Headset",
                "Software Lizenz", "Cloud Storage", "Support Paket",
                "Consulting Stunde", "Online Kurs"
        };

        String[] units = {"Stück", "kg", "m", "Std."};
        Product.ProductType[] types = Product.ProductType.values();

        for (int i = 0; i < 15; i++) {
            String name = productNames[i];
            BigDecimal price = BigDecimal.valueOf(random.nextInt(900) + 100); // 100 - 1000
            String unit = units[random.nextInt(units.length)];
            Product.ProductType type = types[random.nextInt(types.length)];

            Product product = new Product();
            product.setName(name);
            product.setPrice(price);
            product.setUnit(unit);
            product.setProductType(type);
            product.setProductNumber("PROD-" + String.format("%04d", i + 1));
            repository.save(product);
        }
    }

    public List<Product> getAllProducts() {
        List<Product> products = repository.findAll();
        logger.info("Fetched {} products", products.size());
        return products;
    }

    public Optional<Product> getProductById(UUID id) {
        Optional<Product> product = repository.findById(id);
        if (product.isPresent()) {
            logger.info("Found product with id={}", id);
        } else {
            logger.warn("No product found with id={}", id);
        }
        return product;
    }

    public Optional<Product> getProductByProductNumber(String productNumber) {
        Optional<Product> product = repository.findByProductNumber(productNumber);
        logger.info("Search for product with productNumber={}: {}",
                productNumber, product.isPresent() ? "found" : "not found");
        return product;
    }

    public Product createProduct(Product product) {
        validate(product);
        product.setId(null); // DB generiert ID
        Product saved = repository.save(product);
        logger.info("Created new product: id={}, productNumber={}, name={}, price={}",
                saved.getId(), saved.getProductNumber(), saved.getName(), saved.getPrice());
        return saved;
    }

    public Product updateProduct(Product product) {
        validate(product);
        if (product.getId() == null || !repository.existsById(product.getId())) {
            throw new IllegalArgumentException("Product not found for update");
        }
        Product saved = repository.save(product);
        logger.info("Updated product: id={}, productNumber={}, name={}, price={}",
                saved.getId(), saved.getProductNumber(), saved.getName(), saved.getPrice());
        return saved;
    }

    public Product createOrUpdateProduct(Product product) {
        if (product.getId() != null && repository.existsById(product.getId())) {
            return updateProduct(product);
        } else {
            return createProduct(product);
        }
    }

    public void deleteProductById(UUID id) {
        if (!repository.existsById(id)) {
            throw new IllegalArgumentException("Product not found with id=" + id);
        }
        repository.deleteById(id);
        logger.info("Deleted product with id={}", id);
    }

    private void validate(Product product) {
        if (product.getName() == null || product.getName().isBlank()) {
            throw new IllegalArgumentException("Product name must not be empty");
        }
        if (product.getPrice() == null || product.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Product price must be >= 0");
        }
    }

    public List<Product> searchProductsByName(String searchTerm) {
        return repository.findByNameContainingIgnoreCase(searchTerm);
    }

    public long getTotalProductCount() {
        return repository.count();
    }
}
